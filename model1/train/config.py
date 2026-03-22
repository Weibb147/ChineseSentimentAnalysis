# -*- coding: utf-8 -*-
"""
统一训练配置
包含所有模型使用的基础参数和优化配置
已更新为第四阶段：模型特定优化 (最高性能)
"""

import os
import json
import torch


class Config:
    """
    统一训练配置类
    根据第四阶段优化方案，为不同模型提供定制化的最优参数
    """

    def __init__(self, model_type='roberta_base'):
        # --- 基础路径配置 ---
        self.model_name_or_path = os.path.abspath(os.path.join(os.path.dirname(__file__), 'pretrained_models', 'chinese-roberta-wwm-ext'))
        self.data_dir = '../data/clean'
        self.output_dir = '../model'
        self.log_dir = '../logs'
        
        # 模型类型
        self.model_type = model_type

        # --- 基础通用配置 ---
        self.num_labels = 6
        self.label_list = ['angry', 'surprise', 'fear', 'happy', 'sad', 'neutral']
        self.seed = 42                       
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        
        # 优化器通用默认值
        self.adam_epsilon = 1e-8
        self.adam_betas = (0.9, 0.999)
        self.max_grad_norm = 1.0
        self.dynamic_schedule = True
        
        # --- 日志通用默认值 ---
        self.logging_steps = 30
        self.eval_steps = 150
        self.save_steps = 300

        # --- 第四阶段：模型特定优化配置 ---
        self._apply_stage4_optimization(model_type)

        # --- 确保基本模型参数存在 (兼容性兜底) ---
        if not hasattr(self, 'lstm_hidden_size'):
            self.lstm_hidden_size = 256
        if not hasattr(self, 'lstm_num_layers'):
            self.lstm_num_layers = 2
        if not hasattr(self, 'gru_hidden_size'):
            self.gru_hidden_size = 256
        if not hasattr(self, 'gru_num_layers'):
            self.gru_num_layers = 2
        if not hasattr(self, 'hidden_size'): # RoBERTa default
            self.hidden_size = 768
        if not hasattr(self, 'num_attention_heads'): # RoBERTa default
            self.num_attention_heads = 12

        # --- 自动创建目录 ---
        os.makedirs(self.log_dir, exist_ok=True)

    def _apply_stage4_optimization(self, model_type):
        """应用第四阶段：统一消融实验配置 (Unified Ablation Configuration)"""
        
        # --- 统一基础参数 (控制变量法) ---
        # 为了进行公平的消融实验对比，所有 RoBERTa 变体模型必须使用完全相同的
        # 训练轮数、学习率、有效批次大小和正则化策略。
        
        unified_epochs = 10              # 统一增加到10轮，确保所有模型充分收敛
        unified_lr = 2e-5                # 统一标准学习率
        unified_weight_decay = 0.01
        unified_warmup_rate = 0.1
        unified_dropout = 0.3            # 统一 Dropout
        unified_label_smoothing = 0.1
        
        # 显存控制：BiLSTM/GRU 占用显存更多，为了统一，
        # 我们以显存占用最大的模型为基准设定 Batch Size。
        # 设定统一 Effective Batch Size = 36
        unified_batch_size = 18          # 18 * 2 = 36
        unified_grad_acc = 2

        if model_type == 'roberta_base':
            # --- RoBERTa 基础模型 (统一配置) ---
            self.max_seq_length = 256
            self.train_batch_size = unified_batch_size
            self.eval_batch_size = 36
            self.gradient_accumulation_steps = unified_grad_acc
            self.num_train_epochs = unified_epochs
            
            self.learning_rate = unified_lr
            self.backbone_lr = unified_lr * 0.8  # 骨干网络稍低
            self.head_lr = unified_lr * 1.5      # 分类头稍高
            
            self.weight_decay = unified_weight_decay
            self.warmup_rate = unified_warmup_rate
            self.label_smoothing = unified_label_smoothing
            self.dropout_rate = unified_dropout
            self.early_stopping_patience = 4
            self.use_amp = True
            self.scheduler_type = 'cosine'
            
            # 结构参数
            self.hidden_size = 768
            self.num_attention_heads = 12
            self.freeze_backbone_epochs = 0

        elif model_type == 'roberta_bilstm_attention':
            # --- RoBERTa + BiLSTM + Attention (统一配置) ---
            self.max_seq_length = 256
            self.train_batch_size = unified_batch_size
            self.eval_batch_size = 36
            self.gradient_accumulation_steps = unified_grad_acc
            self.num_train_epochs = unified_epochs
            
            self.learning_rate = unified_lr
            self.backbone_lr = unified_lr * 0.8
            self.head_lr = unified_lr * 1.5
            
            self.weight_decay = unified_weight_decay
            self.warmup_rate = unified_warmup_rate
            self.label_smoothing = unified_label_smoothing
            self.dropout_rate = unified_dropout
            self.early_stopping_patience = 4
            self.use_amp = True
            self.scheduler_type = 'cosine'
            
            # 模型结构优化
            self.lstm_hidden_size = 384
            self.lstm_num_layers = 2            # 统一为2层，避免过多参数
            self.hidden_size = 384
            self.num_attention_heads = 12
            self.freeze_backbone_epochs = 1     # 保持冻结首轮策略

        elif model_type == 'roberta_gru_attention':
            # --- RoBERTa + GRU + Attention (统一配置) ---
            self.max_seq_length = 256
            self.train_batch_size = unified_batch_size
            self.eval_batch_size = 36
            self.gradient_accumulation_steps = unified_grad_acc
            self.num_train_epochs = unified_epochs
            
            self.learning_rate = unified_lr
            self.backbone_lr = unified_lr * 0.8
            self.head_lr = unified_lr * 1.5
            
            self.weight_decay = unified_weight_decay
            self.warmup_rate = unified_warmup_rate
            self.label_smoothing = unified_label_smoothing
            self.dropout_rate = unified_dropout
            self.early_stopping_patience = 4
            self.use_amp = True
            self.scheduler_type = 'cosine'
            
            # 模型结构优化
            self.gru_hidden_size = 384          # 与 BiLSTM 保持一致
            self.gru_num_layers = 2             # 与 BiLSTM 保持一致
            self.hidden_size = 384
            self.num_attention_heads = 12       # 与 BiLSTM 保持一致
            self.freeze_backbone_epochs = 1
            
        elif model_type == 'bilstm_attention':
            # --- 纯 BiLSTM + Attention 最高优化 (Baseline) ---
            self.max_seq_length = 256
            self.train_batch_size = 32
            self.eval_batch_size = 64
            self.gradient_accumulation_steps = 1
            self.num_train_epochs = 25

            self.learning_rate = 1e-3
            self.backbone_lr = 1e-3
            self.head_lr = 1e-3
            self.weight_decay = 0.01
            self.warmup_rate = 0.1

            self.label_smoothing = 0.15
            self.dropout_rate = 0.35
            self.early_stopping_patience = 6
            self.use_amp = True
            self.scheduler_type = 'cosine'

            self.embedding_dim = 300
            self.lstm_hidden_size = 384
            self.lstm_num_layers = 2
            self.hidden_size = 384
            self.num_attention_heads = 12
            self.freeze_backbone_epochs = 0

        else:
            # 默认回退到 RoBERTa 基础配置
            print(f"⚠️ 未知模型类型 {model_type}，使用 RoBERTa 默认配置")
            self._apply_stage4_optimization('roberta_base')

    def to_dict(self):
        """将配置序列化为字典"""
        config_dict = {}
        for key, value in vars(self).items():
            if isinstance(value, torch.device):
                config_dict[key] = str(value)
            elif key.startswith('__') or callable(value):
                continue
            else:
                config_dict[key] = value
        return config_dict

    def save_to_json(self, file_path):
        """将配置保存到JSON文件"""
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(self.to_dict(), f, ensure_ascii=False, indent=4)
        print(f"📄 训练配置已保存到: {file_path}")

    def get_model_output_dir(self, model_name):
        """获取模型输出目录"""
        # 兼容旧逻辑，如果 model_name 已经包含了 unified_ 前缀则不重复添加
        # 第四阶段我们也可以继续沿用 unified_ 前缀，或者改为 stage4_
        # 为了保持一致性，我们继续使用 unified_，但在 print_config 中会明确指出是 Stage 4
        if model_name.startswith('unified_'):
            dir_name = model_name
        else:
            dir_name = f"unified_{model_name}"
            
        return os.path.abspath(os.path.join(self.output_dir, dir_name))
    
    def ensure_output_dir(self, model_name):
        """确保模型输出目录存在"""
        output_dir = self.get_model_output_dir(model_name)
        os.makedirs(output_dir, exist_ok=True)
        return output_dir
    
    def print_config(self):
        """打印配置信息"""
        print(f"🎯 训练配置 (第四阶段：模型特定优化) - {self.model_type}")
        print(f"  模型路径: {self.model_name_or_path}")
        print(f"  数据目录: {self.data_dir}")
        print(f"  输出目录: {self.output_dir}")
        print(f"  训练批次: {self.train_batch_size} (有效: {self.train_batch_size * self.gradient_accumulation_steps})")
        print(f"  训练轮数: {self.num_train_epochs}")
        print(f"  学习率 (Base/Head): {self.backbone_lr} / {self.head_lr}")
        print(f"  Dropout: {self.dropout_rate}")
        print(f"  Label Smoothing: {self.label_smoothing}")
        print(f"  冻结策略: 前 {getattr(self, 'freeze_backbone_epochs', 0)} 轮冻结骨干")
        
        # 打印特定结构参数
        if hasattr(self, 'lstm_hidden_size'):
            print(f"  LSTM结构: {self.lstm_num_layers}层, {self.lstm_hidden_size}维度")
        if hasattr(self, 'gru_hidden_size'):
            print(f"  GRU结构: {self.gru_num_layers}层, {self.gru_hidden_size}维度")
            
        print(f"  设备: {self.device}")
        
        # 估算显存使用
        estimated_memory = self._estimate_memory_usage()
        print(f"  预估显存使用: {estimated_memory:.1f}GB / 8GB")

    def _estimate_memory_usage(self):
        """估算显存使用情况"""
        base_memory = 2.0  # RoBERTa基础模型约2GB
        
        if self.model_type == 'roberta_base':
            model_memory = base_memory
        elif self.model_type == 'roberta_bilstm_attention':
            model_memory = base_memory + 1.2  # BiLSTM层数增加，显存增加
        elif self.model_type == 'roberta_gru_attention':
            model_memory = base_memory + 0.9
        else:
            model_memory = base_memory
        
        # 批次数据内存
        seq_memory = (self.train_batch_size * self.max_seq_length * 4) / (1024**3)
        
        # 梯度内存
        gradient_memory = model_memory * 0.5
        
        # 优化器内存
        optimizer_memory = model_memory * 2
        
        # 其他开销
        overhead = 1.0
        
        total_memory = model_memory + seq_memory + gradient_memory + optimizer_memory + overhead
        return min(total_memory, 8.0)


def create_unified_config(model_type):
    """创建配置的工厂函数"""
    return Config(model_type)
