# -*- coding: utf-8 -*-
"""
核心工具函数与可视化工具
包含训练所需的辅助函数、训练器类以及可视化记录工具
"""

import torch
import torch.nn as nn
from transformers import get_cosine_schedule_with_warmup, get_linear_schedule_with_warmup
from sklearn.metrics import accuracy_score, precision_recall_fscore_support
import time
import os
import json
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from datetime import datetime
from typing import Dict, Any, Optional, List

# 设置 matplotlib 后端避免显示问题
import matplotlib
matplotlib.use('Agg')


# --- Helper Classes ---

class NumpyEncoder(json.JSONEncoder):
    """自定义JSON编码器，处理NumPy数据类型"""
    def default(self, obj):
        if isinstance(obj, np.integer):
            return int(obj)
        elif isinstance(obj, np.floating):
            return float(obj)
        elif isinstance(obj, np.ndarray):
            return obj.tolist()
        return super(NumpyEncoder, self).default(obj)


class LabelSmoothingLoss(nn.Module):
    """标签平滑损失函数"""
    def __init__(self, smoothing=0.1, num_classes=6):
        super(LabelSmoothingLoss, self).__init__()
        self.confidence = 1.0 - smoothing
        self.smoothing = smoothing
        self.cls = num_classes
        self.dim = -1

    def forward(self, pred, target):
        pred = pred.log_softmax(dim=self.dim)
        with torch.no_grad():
            true_dist = torch.zeros_like(pred)
            true_dist.fill_(self.smoothing / (self.cls - 1))
            true_dist.scatter_(1, target.data.unsqueeze(1), self.confidence)
        return torch.mean(torch.sum(-true_dist * pred, dim=self.dim))


# --- Helper Functions ---

def create_optimizer(model, config):
    """创建优化器，支持差分学习率"""
    no_decay = ['bias', 'LayerNorm.weight']
    
    # 区分骨干网络和分类头的参数
    optimizer_grouped_parameters = []
    
    # 骨干网络参数 (RoBERTa)
    if hasattr(model, 'roberta'):
        backbone_params = list(model.roberta.named_parameters())
        optimizer_grouped_parameters.extend([
            {
                'params': [p for n, p in backbone_params if not any(nd in n for nd in no_decay)],
                'weight_decay': config.weight_decay,
                'lr': config.backbone_lr
            },
            {
                'params': [p for n, p in backbone_params if any(nd in n for nd in no_decay)],
                'weight_decay': 0.0,
                'lr': config.backbone_lr
            }
        ])
        # 记录已处理的参数名
        backbone_param_names = [n for n, p in backbone_params]
    else:
        backbone_param_names = []
        
    # 分类头和其他参数
    head_params = [p for n, p in model.named_parameters() if n not in backbone_param_names]
    # 如果无法通过名称区分，就遍历所有参数排除骨干参数
    # 更安全的方法是再次遍历所有参数
    head_params_list = []
    for n, p in model.named_parameters():
        is_backbone = False
        if hasattr(model, 'roberta'):
            # 简单检查名称前缀
            if n.startswith('roberta.'):
                is_backbone = True
        
        if not is_backbone:
            head_params_list.append((n, p))
            
    optimizer_grouped_parameters.extend([
        {
            'params': [p for n, p in head_params_list if not any(nd in n for nd in no_decay)],
            'weight_decay': config.weight_decay,
            'lr': config.head_lr
        },
        {
            'params': [p for n, p in head_params_list if any(nd in n for nd in no_decay)],
            'weight_decay': 0.0,
            'lr': config.head_lr
        }
    ])
    
    optimizer = torch.optim.AdamW(
        optimizer_grouped_parameters,
        lr=config.learning_rate,
        eps=config.adam_epsilon
    )
    
    return optimizer


def create_scheduler(optimizer, config, num_training_steps):
    """创建学习率调度器"""
    if config.scheduler_type == 'cosine':
        return get_cosine_schedule_with_warmup(
            optimizer,
            num_warmup_steps=int(num_training_steps * config.warmup_rate),
            num_training_steps=num_training_steps
        )
    else:
        return get_linear_schedule_with_warmup(
            optimizer,
            num_warmup_steps=int(num_training_steps * config.warmup_rate),
            num_training_steps=num_training_steps
        )


def compute_metrics(predictions, labels):
    """计算评估指标"""
    accuracy = accuracy_score(labels, predictions)
    precision, recall, f1, _ = precision_recall_fscore_support(
        labels, predictions, average='weighted', zero_division=0
    )
    return {
        'accuracy': accuracy,
        'f1': f1,
        'precision': precision,
        'recall': recall
    }


def save_model(model, optimizer, scheduler, config, model_name, epoch, metrics):
    """保存模型检查点"""
    output_dir = config.ensure_output_dir(model_name)
    
    # 保存最佳模型 (通常是在此函数被调用时)
    # 我们保存一个不带 epoch 的版本作为"当前最佳"，方便后续加载
    model_to_save = model.module if hasattr(model, 'module') else model
    
    # 1. 保存模型权重
    torch.save(model_to_save.state_dict(), os.path.join(output_dir, 'best_model.pt'))
    
    # 2. 保存配置
    config.save_to_json(os.path.join(output_dir, 'config.json'))
    
    # 3. 保存详细信息 (带时间戳和指标)
    checkpoint_info = {
        'epoch': epoch,
        'metrics': metrics,
        'model_type': config.model_type,
        'timestamp': time.strftime("%Y-%m-%d %H:%M:%S")
    }
    
    # 仅保存最佳模型权重，避免保存重复的epoch文件占用空间
    # torch.save(model_to_save.state_dict(), os.path.join(output_dir, f'best_model_epoch_{epoch+1}.pt'))
    
    with open(os.path.join(output_dir, f'checkpoint_epoch_{epoch+1}.json'), 'w', encoding='utf-8') as f:
        json.dump(checkpoint_info, f, ensure_ascii=False, indent=4)
        
    print(f"💾 模型已保存到: {output_dir}")


def setup_training(config, model_name):
    """设置训练环境"""
    import random
    import numpy as np
    
    # 创建目录
    config.ensure_output_dir(model_name)
    
    # 设置随机种子
    random.seed(config.seed)
    np.random.seed(config.seed)
    torch.manual_seed(config.seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(config.seed)
        
    print(f"🛠️ 训练环境设置完成 - {model_name}")


def print_memory_usage():
    """打印显存使用情况"""
    if not torch.cuda.is_available():
        return "CPU Mode"
        
    allocated = torch.cuda.memory_allocated() / 1024**3
    reserved = torch.cuda.memory_reserved() / 1024**3
    
    return f"Allocated: {allocated:.2f}GB, Reserved: {reserved:.2f}GB"


# --- Visualization Class ---

class TrainingVisualizer:
    """训练可视化器 - 生成每个模型的专属图表和日志"""

    def __init__(self, model_name: str, output_dir: str):
        self.model_name = model_name
        self.output_dir = output_dir
        self.viz_dir = os.path.join(output_dir, 'visualizations')
        os.makedirs(self.viz_dir, exist_ok=True)

        # 设置中文字体和样式
        plt.rcParams['font.sans-serif'] = ['SimHei', 'DejaVu Sans']
        plt.rcParams['axes.unicode_minus'] = False
        sns.set_style("whitegrid")
        sns.set_palette("husl")

        # 训练历史记录
        self.training_history = {
            'epochs': [],
            'train_loss': [],
            'val_loss': [],
            'val_f1': [],
            'val_accuracy': [],
            'val_precision': [],
            'val_recall': [],
            'learning_rates': [],
            'train_time': [],
            'memory_usage': []
        }

        # 详细日志
        self.detailed_logs = []

    def log_epoch(self, epoch: int, train_metrics: Dict, val_metrics: Dict,
                  lr: float = None, train_time: float = None, memory_usage: str = None):
        """记录每个epoch的训练数据"""

        # 更新训练历史
        self.training_history['epochs'].append(epoch + 1)
        self.training_history['train_loss'].append(train_metrics.get('loss', 0))
        self.training_history['val_loss'].append(val_metrics.get('loss', 0))
        self.training_history['val_f1'].append(val_metrics.get('f1', 0))
        self.training_history['val_accuracy'].append(val_metrics.get('accuracy', 0))
        self.training_history['val_precision'].append(val_metrics.get('precision', 0))
        self.training_history['val_recall'].append(val_metrics.get('recall', 0))
        self.training_history['learning_rates'].append(lr or 0)
        self.training_history['train_time'].append(train_time or 0)
        self.training_history['memory_usage'].append(memory_usage or "N/A")

        # 记录详细日志
        log_entry = {
            'timestamp': datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            'epoch': epoch + 1,
            'model': self.model_name,
            'train_metrics': train_metrics,
            'val_metrics': val_metrics,
            'learning_rate': lr,
            'train_time': train_time,
            'memory_usage': memory_usage
        }
        self.detailed_logs.append(log_entry)
        
        # 实时保存CSV格式日志，方便表格查看
        self._save_training_csv(log_entry)

        # 实时保存数据
        self._save_training_data()

        # 每个epoch更新图表
        if epoch >= 0:  # 每个epoch都绘制
            self.plot_training_curves()

    def _save_training_csv(self, log_entry):
        """保存训练日志为CSV格式"""
        import csv
        csv_path = os.path.join(self.viz_dir, f'{self.model_name}_training_log.csv')
        
        # 准备行数据
        row_data = {
            'Epoch': log_entry['epoch'],
            'Time': log_entry['timestamp'],
            'Train Loss': f"{log_entry['train_metrics'].get('loss', 0):.4f}",
            'Val Loss': f"{log_entry['val_metrics'].get('loss', 0):.4f}",
            'Val F1': f"{log_entry['val_metrics'].get('f1', 0):.4f}",
            'Val Accuracy': f"{log_entry['val_metrics'].get('accuracy', 0):.4f}",
            'Val Precision': f"{log_entry['val_metrics'].get('precision', 0):.4f}",
            'Val Recall': f"{log_entry['val_metrics'].get('recall', 0):.4f}",
            'Learning Rate': f"{log_entry['learning_rate']:.2e}" if log_entry['learning_rate'] else "N/A",
            'Train Duration(s)': f"{log_entry['train_time']:.2f}" if log_entry['train_time'] else "N/A"
        }
        
        fieldnames = list(row_data.keys())
        
        try:
            file_exists = os.path.exists(csv_path)
            with open(csv_path, 'a', newline='', encoding='utf-8-sig') as f:
                writer = csv.DictWriter(f, fieldnames=fieldnames)
                if not file_exists:
                    writer.writeheader()
                writer.writerow(row_data)
        except Exception as e:
            print(f"⚠️ 保存CSV日志失败: {e}")

    def _save_training_data(self):
        """保存训练数据到JSON文件"""
        data_path = os.path.join(self.viz_dir, f'{self.model_name}_training_history.json')
        try:
            with open(data_path, 'w', encoding='utf-8') as f:
                json.dump({
                    'history': self.training_history,
                    'logs': self.detailed_logs
                }, f, ensure_ascii=False, indent=4, cls=NumpyEncoder)
        except Exception as e:
            print(f"⚠️ 保存训练数据失败: {e}")

    def plot_training_curves(self):
        """绘制训练曲线 - 生成综合图表和单独指标图表"""
        if len(self.training_history['epochs']) < 2:
            return

        # 1. 生成综合图表 (2x2布局)
        self._plot_combined_metrics()
        
        # 2. 生成各个指标的单独图表
        self._plot_individual_metrics()

    def _plot_combined_metrics(self):
        """绘制综合指标图表 (2x2)"""
        fig, axes = plt.subplots(2, 2, figsize=(15, 12))
        fig.suptitle(f'{self.model_name} 训练曲线汇总', fontsize=16, fontweight='bold')

        epochs = self.training_history['epochs']

        # Loss曲线
        axes[0, 0].plot(epochs, self.training_history['train_loss'], 
                       label='Training Loss', marker='o', linewidth=2)
        axes[0, 0].plot(epochs, self.training_history['val_loss'], 
                       label='Validation Loss', marker='s', linewidth=2)
        axes[0, 0].set_title('Loss Curve', fontweight='bold')
        axes[0, 0].set_xlabel('Epoch')
        axes[0, 0].set_ylabel('Loss')
        axes[0, 0].legend()
        axes[0, 0].grid(True, alpha=0.3)

        # F1分数曲线
        axes[0, 1].plot(epochs, self.training_history['val_f1'], 
                       label='F1 Score', marker='D', linewidth=2, color='green')
        axes[0, 1].set_title('F1 Score Curve', fontweight='bold')
        axes[0, 1].set_xlabel('Epoch')
        axes[0, 1].set_ylabel('F1 Score')
        axes[0, 1].legend()
        axes[0, 1].grid(True, alpha=0.3)

        # 准确率曲线
        axes[1, 0].plot(epochs, self.training_history['val_accuracy'], 
                       label='Accuracy', marker='^', linewidth=2, color='orange')
        axes[1, 0].set_title('Accuracy Curve', fontweight='bold')
        axes[1, 0].set_xlabel('Epoch')
        axes[1, 0].set_ylabel('Accuracy')
        axes[1, 0].legend()
        axes[1, 0].grid(True, alpha=0.3)

        # 综合指标曲线
        axes[1, 1].plot(epochs, self.training_history['val_f1'], 
                       label='F1 Score', marker='o', linewidth=2)
        axes[1, 1].plot(epochs, self.training_history['val_precision'], 
                       label='Precision', marker='s', linewidth=2)
        axes[1, 1].plot(epochs, self.training_history['val_recall'], 
                       label='Recall', marker='^', linewidth=2)
        axes[1, 1].set_title('Comprehensive Evaluation Metrics', fontweight='bold')
        axes[1, 1].set_xlabel('Epoch')
        axes[1, 1].set_ylabel('Score')
        axes[1, 1].legend()
        axes[1, 1].grid(True, alpha=0.3)

        plt.tight_layout()
        
        # 保存综合图片
        save_path = os.path.join(self.viz_dir, f'{self.model_name}_training_curves_combined.png')
        try:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            plt.close()
        except Exception as e:
            print(f"⚠️ 保存综合训练曲线失败: {e}")

    def _plot_individual_metrics(self):
        """绘制各个指标的单独图表"""
        epochs = self.training_history['epochs']
        metrics_config = [
            # (指标数据key, 指标名称, Y轴标签, 文件名后缀, 颜色)
            ('train_loss', 'Training Loss', 'Loss', 'loss_train', 'blue'),
            ('val_loss', 'Validation Loss', 'Loss', 'loss_val', 'red'),
            ('val_f1', 'Validation F1 Score', 'F1 Score', 'f1', 'green'),
            ('val_accuracy', 'Validation Accuracy', 'Accuracy', 'accuracy', 'orange'),
            ('val_precision', 'Validation Precision', 'Precision', 'precision', 'purple'),
            ('val_recall', 'Validation Recall', 'Recall', 'recall', 'cyan'),
            ('learning_rates', 'Learning Rate', 'Learning Rate', 'lr', 'brown')
        ]
        
        # 1. 绘制 Loss 对比图 (Train vs Val)
        plt.figure(figsize=(10, 6))
        plt.plot(epochs, self.training_history['train_loss'], label='Training Loss', marker='o', linewidth=2)
        plt.plot(epochs, self.training_history['val_loss'], label='Validation Loss', marker='s', linewidth=2)
        plt.title(f'{self.model_name} - Loss Curve', fontsize=14, fontweight='bold')
        plt.xlabel('Epoch')
        plt.ylabel('Loss')
        plt.legend()
        plt.grid(True, alpha=0.3)
        plt.tight_layout()
        plt.savefig(os.path.join(self.viz_dir, f'{self.model_name}_curve_loss_compare.png'), dpi=300)
        plt.close()

        # 2. 绘制每个单独指标
        for metric_key, title, ylabel, suffix, color in metrics_config:
            if not self.training_history[metric_key]:
                continue
                
            plt.figure(figsize=(10, 6))
            plt.plot(epochs, self.training_history[metric_key], label=title, marker='o', linewidth=2, color=color)
            plt.title(f'{self.model_name} - {title}', fontsize=14, fontweight='bold')
            plt.xlabel('Epoch')
            plt.ylabel(ylabel)
            plt.legend()
            plt.grid(True, alpha=0.3)
            plt.tight_layout()
            
            save_path = os.path.join(self.viz_dir, f'{self.model_name}_curve_{suffix}.png')
            try:
                plt.savefig(save_path, dpi=300)
                plt.close()
            except Exception as e:
                print(f"⚠️ 保存单独曲线 {suffix} 失败: {e}")

    def generate_training_report(self, best_metrics: Dict, total_time: float):
        """生成最终训练报告"""
        report_path = os.path.join(self.output_dir, f'{self.model_name}_final_report.txt')
        
        try:
            with open(report_path, 'w', encoding='utf-8') as f:
                f.write(f"=== {self.model_name} 训练报告 ===\n")
                f.write(f"生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
                f.write(f"总训练时间: {total_time:.2f}秒 ({total_time/60:.2f}分钟)\n")
                f.write(f"总Epoch数: {len(self.training_history['epochs'])}\n\n")
                
                f.write("--- 最佳性能指标 ---\n")
                f.write(f"F1 Score: {best_metrics.get('f1', 0):.4f}\n")
                f.write(f"Accuracy: {best_metrics.get('accuracy', 0):.4f}\n")
                f.write(f"Precision: {best_metrics.get('precision', 0):.4f}\n")
                f.write(f"Recall: {best_metrics.get('recall', 0):.4f}\n")
                f.write(f"Validation Loss: {best_metrics.get('loss', 0):.4f}\n\n")
                
                f.write("--- 训练过程摘要 ---\n")
                # 计算平均指标
                avg_train_loss = np.mean(self.training_history['train_loss'])
                avg_val_loss = np.mean(self.training_history['val_loss'])
                
                f.write(f"平均训练Loss: {avg_train_loss:.4f}\n")
                f.write(f"平均验证Loss: {avg_val_loss:.4f}\n")
                f.write(f"Loss下降趋势: {self.training_history['train_loss'][0]:.4f} -> {self.training_history['train_loss'][-1]:.4f}\n")
            
            return report_path
        except Exception as e:
            print(f"⚠️ 生成报告失败: {e}")
            return "Report generation failed"


# --- Trainer Class ---

class SimpleTrainer:
    """优化的训练器 - 增强版可视化功能"""
    
    def __init__(self, model, config, optimizer=None, scheduler=None, model_name='model'):
        self.model = model
        self.config = config
        self.optimizer = optimizer
        self.scheduler = scheduler
        self.device = config.device
        self.model_name = model_name
        
        # 损失函数
        if getattr(config, 'label_smoothing', 0) > 0:
            self.criterion = LabelSmoothingLoss(config.label_smoothing, config.num_labels)
        else:
            self.criterion = nn.CrossEntropyLoss()
        
        # 混合精度
        if getattr(config, 'use_amp', True) and torch.cuda.is_available():
            try:
                self.scaler = torch.cuda.amp.GradScaler()
            except Exception:
                self.scaler = None
            print("⚡️ 混合精度训练已启用 (AMP)。")
        else:
            self.scaler = None
            if getattr(config, 'use_amp', True):
                print("⚠️ 未启用混合精度训练 (AMP)，因为CUDA不可用。")
        
        # 可视化器
        output_dir = config.get_model_output_dir(model_name)
        self.visualizer = TrainingVisualizer(model_name, output_dir)
        
        self._setup_logging()
    
    def _setup_logging(self):
        """设置增强日志系统"""
        import logging
        
        base_output_dir = self.config.get_model_output_dir(self.model_name)
        # 训练过程日志写到临时目录，结束时再归档到 {结束时间}/logs
        log_dir = os.path.join(base_output_dir, "session_tmp_logs")
        os.makedirs(log_dir, exist_ok=True)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        log_file = os.path.join(log_dir, f"{self.model_name}_{timestamp}.log")
        self.log_file_path = log_file
        
        # 创建专用的logger
        self.logger = logging.getLogger(f"{self.model_name}_trainer")
        self.logger.setLevel(logging.INFO)
        
        # 避免重复添加handler
        if not self.logger.handlers:
            # 文件handler
            file_handler = logging.FileHandler(log_file, encoding='utf-8')
            file_handler.setLevel(logging.INFO)
            
            # 控制台handler
            console_handler = logging.StreamHandler()
            console_handler.setLevel(logging.INFO)
            
            # 格式化
            formatter = logging.Formatter('%(asctime)s - %(name)s - %(message)s')
            file_handler.setFormatter(formatter)
            console_handler.setFormatter(formatter)
            
            self.logger.addHandler(file_handler)
            self.logger.addHandler(console_handler)
        
        self.logger.info(f"开始训练模型: {self.model_name}")
    
    def train_step(self, batch):
        """执行单个训练步"""
        batch = {k: v.to(self.device) for k, v in batch.items()}
        
        # 混合精度上下文
        if self.scaler:
            with torch.cuda.amp.autocast():
                outputs = self.model(**batch)
                logits = outputs.logits if hasattr(outputs, 'logits') else outputs[1]
                loss = self.criterion(logits, batch['labels'])
                loss = loss / self.config.gradient_accumulation_steps
            
            # 缩放梯度
            self.scaler.scale(loss).backward()
        else:
            outputs = self.model(**batch)
            logits = outputs.logits if hasattr(outputs, 'logits') else outputs[1]
            loss = self.criterion(logits, batch['labels'])
            loss = loss / self.config.gradient_accumulation_steps
            loss.backward()
            
        return loss.item() * self.config.gradient_accumulation_steps, logits
        
    def optimizer_step(self):
        """执行优化器步"""
        if self.scaler:
            self.scaler.unscale_(self.optimizer)
            torch.nn.utils.clip_grad_norm_(self.model.parameters(), self.config.max_grad_norm)
            self.scaler.step(self.optimizer)
            self.scaler.update()
        else:
            torch.nn.utils.clip_grad_norm_(self.model.parameters(), self.config.max_grad_norm)
            self.optimizer.step()
            
        self.optimizer.zero_grad()
        
    def scheduler_step(self):
        """执行调度器步"""
        if self.scheduler:
            self.scheduler.step()
            
    def eval_step(self, batch):
        """执行单个评估步"""
        batch = {k: v.to(self.device) for k, v in batch.items()}
        
        outputs = self.model(**batch)
        logits = outputs.logits if hasattr(outputs, 'logits') else outputs[1]
        loss = self.criterion(logits, batch['labels'])
        
        return loss.item(), logits
    
    def log_epoch(self, epoch: int, train_metrics: Dict[str, Any], val_metrics: Dict[str, Any], 
                  lr: Optional[float] = None, train_time: Optional[float] = None):
        """记录epoch训练数据并生成可视化"""
        memory_usage = print_memory_usage()
        
        # 记录到可视化器
        self.visualizer.log_epoch(epoch, train_metrics, val_metrics, float(lr or 0.0), float(train_time or 0.0), memory_usage)
        
        # 记录到日志
        self.logger.info(f"Epoch {epoch+1} - Train Loss: {train_metrics.get('loss', 0):.4f}, "
                        f"Val Loss: {val_metrics.get('loss', 0):.4f}, "
                        f"Val F1: {val_metrics.get('f1', 0):.4f}, "
                        f"Val Acc: {val_metrics.get('accuracy', 0):.4f}")
        
        if lr:
            self.logger.info(f"Learning Rate: {lr:.2e}")
        if train_time:
            self.logger.info(f"Epoch Time: {train_time:.2f}s")
        self.logger.info(f"Memory: {memory_usage}")
    
    def log_training_complete(self, best_f1: float, total_time: float, best_metrics: Dict[str, Any]):
        """记录训练完成信息并生成最终报告，并将最佳模型追加训练结束时间戳命名"""
        self.logger.info(f"训练完成! 最佳F1: {best_f1:.4f}, 总时间: {total_time:.2f}s")
        
        # 生成最终训练报告
        report = self.visualizer.generate_training_report(best_metrics, total_time)
        self.logger.info(f"训练报告已生成: {report}")
        
        # 归档
        self._archive_results()

    def _archive_results(self):
        """归档训练结果"""
        try:
            end_ts = datetime.now().strftime("%Y%m%d_%H%M%S")
            base_output_dir = self.config.get_model_output_dir(self.model_name)
            end_dir = os.path.join(base_output_dir, end_ts)
            os.makedirs(end_dir, exist_ok=True)

            # 1) 归档最佳模型
            best_src = os.path.join(base_output_dir, 'best_model.pt')
            if not os.path.exists(best_src):
                candidates = [f for f in os.listdir(base_output_dir) if f.startswith('best_model_') and f.endswith('.pt')]
                if candidates:
                    candidates.sort(reverse=True)
                    best_src = os.path.join(base_output_dir, candidates[0])
            
            if os.path.exists(best_src):
                best_dst = os.path.join(end_dir, 'best_model.pt')
                try:
                    import shutil
                    shutil.copy2(best_src, best_dst) # 复制而不是移动，保留最新模型在根目录方便加载
                    self.logger.info(f"✅ 最佳模型已备份到: {best_dst}")
                except Exception as e:
                    self.logger.error(f"❌ 备份最佳模型失败: {e}")

            # 2) 归档可视化输出
            viz_src = getattr(self.visualizer, 'viz_dir', os.path.join(base_output_dir, 'visualizations'))
            viz_dst = os.path.join(end_dir, 'visualizations')
            try:
                import shutil
                if os.path.isdir(viz_src):
                    if os.path.exists(viz_dst):
                        shutil.rmtree(viz_dst)
                    shutil.copytree(viz_src, viz_dst)
                    self.logger.info(f"✅ 可视化文件已归档到: {viz_dst}")
            except Exception as e_viz:
                self.logger.error(f"❌ 可视化归档失败: {e_viz}")

            # 3) 归档日志
            logs_dst_dir = os.path.join(end_dir, 'logs')
            os.makedirs(logs_dst_dir, exist_ok=True)
            session_log_dir = os.path.join(base_output_dir, "session_tmp_logs")
            if os.path.isdir(session_log_dir):
                import shutil
                for name in os.listdir(session_log_dir):
                    src_path = os.path.join(session_log_dir, name)
                    dst_path = os.path.join(logs_dst_dir, name)
                    try:
                        shutil.copy2(src_path, dst_path)
                    except Exception:
                        pass
                self.logger.info(f"✅ 日志已归档到: {logs_dst_dir}")
                
        except Exception as e:
            self.logger.error(f"❌ 归档过程发生错误: {e}")
