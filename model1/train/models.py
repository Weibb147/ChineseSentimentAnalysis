# -*- coding: utf-8 -*-
"""
核心模型定义
包含 RoBERTa 基础模型及其变体 (BiLSTM, GRU)
"""

import torch
import torch.nn as nn
from transformers import AutoModel, PreTrainedModel, AutoConfig
from typing import Optional

# 轻量输出结构，兼容训练代码的 outputs.loss / outputs.logits 访问
class ModelOutput:
    def __init__(self, loss: Optional[torch.Tensor], logits: torch.Tensor):
        self.loss = loss
        self.logits = logits


class BiLSTMAttention(nn.Module):
    """纯 BiLSTM + Attention 模型 (不使用预训练模型)"""

    def __init__(self, config):
        super().__init__()
        self.num_labels = config.num_labels
        
        # 获取词表大小，默认21128 (中文RoBERTa)
        try:
            model_config = AutoConfig.from_pretrained(config.model_name_or_path, local_files_only=True)
            vocab_size = model_config.vocab_size
        except:
            vocab_size = 21128
            
        # 词嵌入层
        # 使用 hidden_size 作为嵌入维度，通常为 768 或 300
        # 这里为了与配置兼容，使用 config.hidden_size (通常配置为 768 或自定义)
        embedding_dim = getattr(config, 'embedding_dim', config.hidden_size)
        self.embedding = nn.Embedding(vocab_size, embedding_dim, padding_idx=0)
        
        # BiLSTM层
        self.lstm = nn.LSTM(
            embedding_dim,
            config.lstm_hidden_size,
            config.lstm_num_layers,
            batch_first=True,
            bidirectional=True,
            dropout=config.dropout_rate if config.lstm_num_layers > 1 else 0
        )
        
        # 注意力层
        self.attention = nn.MultiheadAttention(
            config.lstm_hidden_size * 2,
            config.num_attention_heads,
            dropout=config.dropout_rate,
            batch_first=True
        )
        
        # 归一化与融合投影 + 分类器
        self.layer_norm = nn.LayerNorm(config.lstm_hidden_size * 2)
        self.dropout = nn.Dropout(config.dropout_rate)
        
        # 投影层：将 LSTM 输出映射到 hidden_size (如果不同)
        # 这里假设 classifier 输入维度是 hidden_size
        # 如果 lstm_hidden_size * 2 != hidden_size，则需要投影
        # 现有 config 通常 hidden_size=768, lstm_hidden_size=256 -> 512
        self.fusion_proj = nn.Sequential(
            nn.Linear((config.lstm_hidden_size * 2) * 2, config.hidden_size),
            nn.GELU(),
            nn.LayerNorm(config.hidden_size),
            nn.Dropout(config.dropout_rate)
        )
        
        # 分类头
        self.classifier = nn.Sequential(
            nn.Linear(config.hidden_size, config.hidden_size // 2),
            nn.ReLU(),
            nn.LayerNorm(config.hidden_size // 2),
            nn.Dropout(config.dropout_rate),
            nn.Linear(config.hidden_size // 2, config.num_labels)
        )

    def forward(self, input_ids, attention_mask=None, labels=None):
        # 嵌入层
        embeddings = self.embedding(input_ids)
        embeddings = self.dropout(embeddings)
        
        # BiLSTM
        # 注意：这里没有使用 pack_padded_sequence，简单起见直接输入
        # 也可以根据 attention_mask 计算 lengths 并 pack
        lstm_output, _ = self.lstm(embeddings)
        
        # 注意力机制（忽略padding）
        key_padding_mask = (attention_mask == 0) if attention_mask is not None else None
        attn_output, _ = self.attention(lstm_output, lstm_output, lstm_output, key_padding_mask=key_padding_mask)
        attn_output = self.layer_norm(attn_output + lstm_output)
        
        # Masked mean/max 池化
        if attention_mask is not None:
            mask = attention_mask.unsqueeze(-1).expand(attn_output.size()).float()
            # mean
            masked_sum = torch.sum(attn_output * mask, dim=1)
            mask_count = torch.clamp(torch.sum(mask, dim=1), min=1e-6)
            mean_vec = masked_sum / mask_count
            # max
            very_small = torch.finfo(attn_output.dtype).min
            attn_masked_for_max = torch.where(mask > 0, attn_output, torch.full_like(attn_output, very_small))
            max_vec, _ = torch.max(attn_masked_for_max, dim=1)
        else:
            mean_vec = torch.mean(attn_output, dim=1)
            max_vec, _ = torch.max(attn_output, dim=1)
            
        fused = torch.cat([mean_vec, max_vec], dim=1)
        fused = self.fusion_proj(fused)
        logits = self.classifier(fused)
        
        loss = None
        if labels is not None:
            loss_fct = nn.CrossEntropyLoss()
            loss = loss_fct(logits.view(-1, self.num_labels), labels.view(-1))
            
        return ModelOutput(loss, logits)


class RobertaBase(nn.Module):
    """RoBERTa基础模型"""
    
    def __init__(self, config):
        super().__init__()
        self.num_labels = config.num_labels
        # 使用本地模型，避免网络下载
        self.roberta: PreTrainedModel = AutoModel.from_pretrained(config.model_name_or_path, local_files_only=True)
        self.dropout = nn.Dropout(config.dropout_rate)
        # 增强分类头
        self.classifier = nn.Sequential(
            nn.Linear(self.roberta.config.hidden_size, self.roberta.config.hidden_size // 2),
            nn.ReLU(),
            nn.LayerNorm(self.roberta.config.hidden_size // 2),
            nn.Dropout(config.dropout_rate),
            nn.Linear(self.roberta.config.hidden_size // 2, config.num_labels)
        )
    
    def forward(self, input_ids, attention_mask=None, labels=None):
        outputs = self.roberta(input_ids=input_ids, attention_mask=attention_mask)
        pooled_output = outputs.pooler_output
        pooled_output = self.dropout(pooled_output)
        logits = self.classifier(pooled_output)
        
        loss = None
        if labels is not None:
            if getattr(self, 'label_smoothing', 0) > 0:
                loss_fct = LabelSmoothingCrossEntropy(self.label_smoothing)
            else:
                loss_fct = nn.CrossEntropyLoss()
            loss = loss_fct(logits.view(-1, self.num_labels), labels.view(-1))
        
        return ModelOutput(loss, logits)


class RobertaBiLSTM(nn.Module):
    """RoBERTa + BiLSTM + Attention模型"""
    
    def __init__(self, config):
        super().__init__()
        self.num_labels = config.num_labels
        # 使用本地模型，避免网络下载
        self.roberta: PreTrainedModel = AutoModel.from_pretrained(config.model_name_or_path, local_files_only=True)
        
        # BiLSTM层
        self.lstm = nn.LSTM(
            self.roberta.config.hidden_size,
            config.lstm_hidden_size,
            config.lstm_num_layers,
            batch_first=True,
            bidirectional=True,
            dropout=config.dropout_rate if config.lstm_num_layers > 1 else 0
        )
        
        # 注意力层
        self.attention = nn.MultiheadAttention(
            config.lstm_hidden_size * 2,
            config.num_attention_heads,
            dropout=config.dropout_rate,
            batch_first=True
        )
        
        # 归一化与融合投影 + 分类器（更强的头部）
        self.layer_norm = nn.LayerNorm(config.lstm_hidden_size * 2)
        self.dropout = nn.Dropout(config.dropout_rate)
        # 融合 mean/max 后再投影到 RoBERTa hidden_size，提升表达与稳定性
        self.fusion_proj = nn.Sequential(
            nn.Linear((config.lstm_hidden_size * 2) * 2, self.roberta.config.hidden_size),
            nn.GELU(),
            nn.LayerNorm(self.roberta.config.hidden_size),
            nn.Dropout(config.dropout_rate)
        )
        # 增强分类头
        self.classifier = nn.Sequential(
            nn.Linear(self.roberta.config.hidden_size, self.roberta.config.hidden_size // 2),
            nn.ReLU(),
            nn.LayerNorm(self.roberta.config.hidden_size // 2),
            nn.Dropout(config.dropout_rate),
            nn.Linear(self.roberta.config.hidden_size // 2, config.num_labels)
        )
    
    def forward(self, input_ids, attention_mask=None, labels=None):
        # RoBERTa编码
        roberta_outputs = self.roberta(input_ids=input_ids, attention_mask=attention_mask)
        sequence_output = roberta_outputs.last_hidden_state
        
        # BiLSTM
        lstm_output, _ = self.lstm(sequence_output)
        
        # 注意力机制（忽略padding）+ 残差 + LayerNorm
        key_padding_mask = (attention_mask == 0) if attention_mask is not None else None
        attn_output, _ = self.attention(lstm_output, lstm_output, lstm_output, key_padding_mask=key_padding_mask)
        attn_output = self.layer_norm(attn_output + lstm_output)
        
        # Masked mean/max 池化并融合（更鲁棒）
        if attention_mask is not None:
            mask = attention_mask.unsqueeze(-1).expand(attn_output.size()).float()
            # mean
            masked_sum = torch.sum(attn_output * mask, dim=1)
            mask_count = torch.clamp(torch.sum(mask, dim=1), min=1e-6)
            mean_vec = masked_sum / mask_count
            # max（将pad位置置为极小）
            very_small = torch.finfo(attn_output.dtype).min
            attn_masked_for_max = torch.where(mask > 0, attn_output, torch.full_like(attn_output, very_small))
            max_vec, _ = torch.max(attn_masked_for_max, dim=1)
        else:
            mean_vec = torch.mean(attn_output, dim=1)
            max_vec, _ = torch.max(attn_output, dim=1)
        
        fused = torch.cat([mean_vec, max_vec], dim=1)
        fused = self.fusion_proj(fused)
        logits = self.classifier(fused)
        
        loss = None
        if labels is not None:
            if getattr(self, 'label_smoothing', 0) > 0:
                loss_fct = LabelSmoothingCrossEntropy(self.label_smoothing)
            else:
                loss_fct = nn.CrossEntropyLoss()
            loss = loss_fct(logits.view(-1, self.num_labels), labels.view(-1))
        
        return ModelOutput(loss, logits)


class RobertaGRU(nn.Module):
    """RoBERTa + GRU + Multi-head Attention模型"""
    
    def __init__(self, config):
        super().__init__()
        self.num_labels = config.num_labels
        # 使用本地模型，避免网络下载
        self.roberta: PreTrainedModel = AutoModel.from_pretrained(config.model_name_or_path, local_files_only=True)
        
        # GRU层
        self.gru = nn.GRU(
            self.roberta.config.hidden_size,
            config.gru_hidden_size,
            config.gru_num_layers,
            batch_first=True,
            bidirectional=True,
            dropout=config.dropout_rate if config.gru_num_layers > 1 else 0
        )
        
        # 多头注意力
        self.multihead_attention = nn.MultiheadAttention(
            config.gru_hidden_size * 2,
            config.num_attention_heads,
            dropout=config.dropout_rate,
            batch_first=True
        )
        
        # 归一化与融合投影 + 分类器（更强的头部）
        self.layer_norm = nn.LayerNorm(config.gru_hidden_size * 2)
        self.dropout = nn.Dropout(config.dropout_rate)
        self.fusion_proj = nn.Sequential(
            nn.Linear((config.gru_hidden_size * 2) * 2, self.roberta.config.hidden_size),
            nn.GELU(),
            nn.LayerNorm(self.roberta.config.hidden_size),
            nn.Dropout(config.dropout_rate)
        )
        # 增强分类头
        self.classifier = nn.Sequential(
            nn.Linear(self.roberta.config.hidden_size, self.roberta.config.hidden_size // 2),
            nn.ReLU(),
            nn.LayerNorm(self.roberta.config.hidden_size // 2),
            nn.Dropout(config.dropout_rate),
            nn.Linear(self.roberta.config.hidden_size // 2, config.num_labels)
        )
    
    def forward(self, input_ids, attention_mask=None, labels=None):
        # RoBERTa编码
        roberta_outputs = self.roberta(input_ids=input_ids, attention_mask=attention_mask)
        sequence_output = roberta_outputs.last_hidden_state
        
        # GRU
        gru_output, _ = self.gru(sequence_output)
        
        # 多头注意力（忽略padding）+ 残差 + LayerNorm
        key_padding_mask = (attention_mask == 0) if attention_mask is not None else None
        attn_output, _ = self.multihead_attention(gru_output, gru_output, gru_output, key_padding_mask=key_padding_mask)
        attn_output = self.layer_norm(attn_output + gru_output)
        
        # Masked mean/max 池化并融合（更鲁棒）
        if attention_mask is not None:
            mask = attention_mask.unsqueeze(-1).expand(attn_output.size()).float()
            masked_sum = torch.sum(attn_output * mask, dim=1)
            mask_count = torch.clamp(torch.sum(mask, dim=1), min=1e-6)
            mean_vec = masked_sum / mask_count
            very_small = torch.finfo(attn_output.dtype).min
            attn_masked_for_max = torch.where(mask > 0, attn_output, torch.full_like(attn_output, very_small))
            max_vec, _ = torch.max(attn_masked_for_max, dim=1)
        else:
            mean_vec = torch.mean(attn_output, dim=1)
            max_vec, _ = torch.max(attn_output, dim=1)
        
        fused = torch.cat([mean_vec, max_vec], dim=1)
        fused = self.fusion_proj(fused)
        logits = self.classifier(fused)
        
        loss = None
        if labels is not None:
            if getattr(self, 'label_smoothing', 0) > 0:
                loss_fct = LabelSmoothingCrossEntropy(self.label_smoothing)
            else:
                loss_fct = nn.CrossEntropyLoss()
            loss = loss_fct(logits.view(-1, self.num_labels), labels.view(-1))
        
        return ModelOutput(loss, logits)


class LabelSmoothingCrossEntropy(nn.Module):
    """标签平滑交叉熵损失"""
    def __init__(self, smoothing=0.1):
        super(LabelSmoothingCrossEntropy, self).__init__()
        self.smoothing = smoothing
        self.confidence = 1.0 - smoothing

    def forward(self, x, target):
        logprobs = torch.nn.functional.log_softmax(x, dim=-1)
        nll_loss = -logprobs.gather(dim=-1, index=target.unsqueeze(1))
        nll_loss = nll_loss.squeeze(1)
        smooth_loss = -logprobs.mean(dim=-1)
        loss = self.confidence * nll_loss + self.smoothing * smooth_loss
        return loss.mean()


def create_model(model_type, config):
    """创建模型"""
    if model_type == 'roberta_base':
        return RobertaBase(config)
    elif model_type == 'roberta_bilstm_attention':
        return RobertaBiLSTM(config)
    elif model_type == 'roberta_gru_attention':
        return RobertaGRU(config)
    elif model_type == 'bilstm_attention':
        return BiLSTMAttention(config)
    else:
        raise ValueError(f"未知的模型类型: {model_type}")
