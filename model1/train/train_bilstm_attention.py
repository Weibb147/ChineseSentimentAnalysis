#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
纯 BiLSTM + Attention 模型训练脚本
作为对比基线模型，不使用 RoBERTa 预训练权重，而是从头训练 Embedding + BiLSTM
"""

from trainer import train_model_with_config
from config import create_unified_config
import warnings
warnings.filterwarnings("ignore")


def train_bilstm_pure():
    """训练纯 BiLSTM + Attention 模型 (第四阶段：模型特定优化)"""
    # 创建统一配置
    config = create_unified_config('bilstm_attention')
    print("🚀 启动 BiLSTM+Attention 第四阶段训练 (最高性能配置)...")
    config.print_config()
    
    return train_model_with_config('bilstm_attention', config)


if __name__ == "__main__":
    try:
        success, best_f1, best_metrics = train_bilstm_pure()
        if success:
            print("✅ BiLSTM+Attention (Baseline) 模型训练成功!")
            print(f"📊 最终结果: F1={best_f1:.4f}, Acc={best_metrics.get('accuracy', 0):.4f}")
            print("📈 训练曲线和日志已保存到 ../model/unified_bilstm_attention/visualizations/")
        else:
            print("❌ BiLSTM+Attention 模型训练失败!")
    except Exception as e:
        print(f"❌ 训练过程中发生错误: {e}")
        import traceback
        traceback.print_exc()
