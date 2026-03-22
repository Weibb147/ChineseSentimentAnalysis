#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
统一训练器模块
提供统一的训练流程，支持多种模型类型
"""

import torch
import time
import math
import warnings
from tqdm import tqdm
from typing import Tuple, Dict, Any

from config import Config
from data import create_datasets, create_dataloaders
from models import create_model
from utils import (
    SimpleTrainer, create_optimizer, create_scheduler,
    compute_metrics, save_model, setup_training, print_memory_usage
)

warnings.filterwarnings("ignore")


def train_model(model_type: str) -> Tuple[bool, float, Dict[str, Any]]:
    """
    统一模型训练函数 - 使用默认配置
    
    Args:
        model_type: 模型类型 ('roberta_base', 'roberta_bilstm_attention', 'roberta_gru_attention')
    
    Returns:
        tuple: (success, best_f1, best_metrics)
    """
    # 初始化配置
    config = Config(model_type=model_type)
    return train_model_with_config(model_type, config)


def train_model_with_config(model_type: str, config) -> Tuple[bool, float, Dict[str, Any]]:
    """
    统一模型训练函数 - 使用自定义配置
    
    Args:
        model_type: 模型类型 ('roberta_base', 'roberta_bilstm_attention', 'roberta_gru_attention')
        config: 训练配置对象
    
    Returns:
        tuple: (success, best_f1, best_metrics)
    """
    model_name = model_type
    
    # 设置训练环境
    setup_training(config, model_name)
    
    # 创建数据集
    print("📊 加载数据...")
    train_dataset, dev_dataset, test_dataset, tokenizer = create_datasets(config)
    train_loader, dev_loader, test_loader = create_dataloaders(train_dataset, dev_dataset, test_dataset, config)
    
    print(f"训练样本: {len(train_dataset)}, 验证样本: {len(dev_dataset)}, 测试样本: {len(test_dataset)}")
    
    # 创建模型
    print("🤖 创建模型...")
    model = create_model(model_type, config).to(config.device)
    print(f"模型参数量: {sum(p.numel() for p in model.parameters()):,}")
    
    # 计算训练步数
    steps_per_epoch = math.ceil(len(train_loader) / max(1, config.gradient_accumulation_steps))
    num_training_steps = steps_per_epoch * config.num_train_epochs
    
    # 创建优化器和调度器
    optimizer = create_optimizer(model, config)
    scheduler = create_scheduler(optimizer, config, num_training_steps)
    
    # 创建训练器（传入模型名称用于可视化）
    trainer = SimpleTrainer(model, config, optimizer, scheduler, model_name)
    
    # 训练循环
    print("🔥 开始训练...")
    best_f1 = 0.0
    best_metrics = {}
    patience_counter = 0
    start_time = time.time()
    
    for epoch in range(config.num_train_epochs):
        print(f"\n=== Epoch {epoch+1}/{config.num_train_epochs} ===")
        epoch_start = time.time()
        
        # 对于带有注意力机制的模型，支持冻结/解冻RoBERTa编码器
        if model_type in ['roberta_bilstm_attention', 'roberta_gru_attention']:
            try:
                freeze_epochs = getattr(config, 'freeze_backbone_epochs', 0)
                req_grad = False if epoch < freeze_epochs else True
                for p in model.roberta.parameters():
                    p.requires_grad = req_grad
            except Exception:
                pass
        
        # 训练阶段
        train_metrics = _train_epoch(trainer, train_loader, dev_loader, config, epoch)
        
        # 验证阶段 - 使用完整验证集，确保评估稳定性
        val_metrics = _evaluate_model(trainer, dev_loader, quick=False)
        
        # 记录训练历史和日志（包含可视化）
        current_lr = trainer.scheduler.get_last_lr()[0] if trainer.scheduler else config.learning_rate
        epoch_time = time.time() - epoch_start
        trainer.log_epoch(epoch, train_metrics, val_metrics, current_lr, epoch_time)
        
        print(f"Epoch {epoch+1} 结果: Loss={val_metrics['loss']:.4f}, F1={val_metrics['f1']:.4f}, Acc={val_metrics['accuracy']:.4f}")
        print(f"验证样本数: {val_metrics.get('eval_samples', 'Unknown')}")
        print(f"内存使用: {print_memory_usage()}")
        
        # 保存最佳模型 - 只有当F1显著提升时才保存
        if val_metrics['f1'] > best_f1:
            # 检查提升是否显著（至少提升0.001）
            if val_metrics['f1'] - best_f1 > 0.001:
                best_f1 = val_metrics['f1']
                best_metrics = val_metrics.copy()
                patience_counter = 0
                save_model(model, optimizer, scheduler, config, model_name, epoch, val_metrics)
                print(f"🎯 发现更好模型! F1: {best_f1:.4f} (提升: {val_metrics['f1'] - best_f1 + 0.001:.4f})")
            else:
                print(f"📊 F1提升较小 ({val_metrics['f1'] - best_f1 + 0.001:.4f})，继续训练")
                patience_counter += 1
        else:
            patience_counter += 1
        
        # 早停检查
        if patience_counter >= config.early_stopping_patience:
            print(f"🛑 早停触发 (patience: {patience_counter})")
            break
        
        # 清理内存
        if torch.cuda.is_available():
            torch.cuda.empty_cache()
    
    # 训练完成 - 生成最终报告和可视化
    total_time = time.time() - start_time
    print(f"\n🎉 训练完成! 最佳F1: {best_f1:.4f}, 训练时间: {total_time:.2f}秒")
    
    # 生成最终训练报告和可视化图表
    if hasattr(trainer, 'log_training_complete'):
        trainer.log_training_complete(float(best_f1), float(total_time), best_metrics)
    
    return True, best_f1, best_metrics


def _train_epoch(trainer, dataloader, dev_loader, config, epoch):
    """训练一个epoch"""
    trainer.model.train()
    total_loss = 0.0
    all_predictions = []
    all_labels = []
    
    progress_bar = tqdm(dataloader, desc=f"训练 Epoch {epoch+1}", leave=False)
    
    for step, batch in enumerate(progress_bar):
        # 训练步骤
        loss, logits = trainer.train_step(batch)
        total_loss += loss
        
        # 收集预测结果（采样以节省内存）
        if step % 10 == 0:
            predictions = torch.argmax(logits, dim=-1)
            all_predictions.extend(predictions.cpu().numpy())
            all_labels.extend(batch['labels'].numpy())
        
        # 梯度累积
        if (step + 1) % config.gradient_accumulation_steps == 0:
            trainer.optimizer_step()
            trainer.scheduler_step()
        
        # 更新进度条（减少频率）
        if step % 20 == 0:
            avg_loss = total_loss / (step + 1)
            current_lr = trainer.scheduler.get_last_lr()[0] if trainer.scheduler else config.learning_rate
            progress_bar.set_postfix({
                'loss': f'{avg_loss:.4f}', 
                'lr': f'{current_lr:.2e}'
            })
        
        # 定期评估 - 降低频率并使用更稳定的评估方式
        if step > 0 and step % config.eval_steps == 0:
            val_metrics = _evaluate_model(trainer, dev_loader, quick=True)
            print(f"\n  Step {step} - Val F1: {val_metrics['f1']:.4f} (样本: {val_metrics.get('eval_samples', 'Unknown')})")
            trainer.model.train()
        
        # 定期清理内存
        if step % 50 == 0 and torch.cuda.is_available():
            torch.cuda.empty_cache()
    
    # 计算epoch指标
    avg_loss = total_loss / len(dataloader)
    if all_predictions and all_labels:
        metrics = compute_metrics(all_predictions, all_labels)
    else:
        metrics = {'accuracy': 0.0, 'precision': 0.0, 'recall': 0.0, 'f1': 0.0}
    metrics['loss'] = avg_loss
    
    return metrics


def _evaluate_model(trainer, dataloader, quick=False):
    """评估模型 - 修复版本，减少评估波动"""
    trainer.model.eval()
    total_loss = 0.0
    all_predictions = []
    all_labels = []
    
    # 评估模式：使用完整验证集，确保评估稳定性
    # 训练中的快速评估使用固定数量的批次，确保每次评估的数据一致
    if quick:
        # 使用固定数量的批次，确保每次评估结果一致
        eval_batches = min(50, len(dataloader) // 4)  # 最多50个批次，或1/4的验证集
    else:
        # 完整评估
        eval_batches = len(dataloader)
    
    with torch.no_grad():
        for step, batch in enumerate(dataloader):
            if step >= eval_batches:
                break
                
            loss, logits = trainer.eval_step(batch)
            total_loss += loss
            
            predictions = torch.argmax(logits, dim=-1)
            all_predictions.extend(predictions.cpu().numpy())
            all_labels.extend(batch['labels'].numpy())
            
            # 定期清理内存
            if step % 20 == 0 and torch.cuda.is_available():
                torch.cuda.empty_cache()
    
    avg_loss = total_loss / eval_batches
    metrics = compute_metrics(all_predictions, all_labels)
    metrics['loss'] = avg_loss
    
    # 添加评估信息
    metrics['eval_samples'] = len(all_predictions)
    metrics['eval_batches'] = eval_batches
    
    return metrics


# 为向后兼容，保留原有的函数名
train_roberta = lambda: train_model('roberta_base')
train_roberta_bilstm_attention = lambda: train_model('roberta_bilstm_attention')
train_roberta_gru_attention = lambda: train_model('roberta_gru_attention')


if __name__ == "__main__":
    print("请使用具体的模型训练脚本或train_all_models.py来训练模型")
