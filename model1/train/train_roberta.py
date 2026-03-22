#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RoBERTa基础模型训练脚本 - 统一配置版
使用统一稳定的训练配置，确保三个模型训练结果可比性
"""

from trainer import train_model_with_config
from config import create_unified_config
import torch
import time
import warnings
warnings.filterwarnings("ignore")


def train_roberta():
    """训练RoBERTa基础模型 - 统一配置版"""
    # 创建统一配置
    config = create_unified_config('roberta_base')
    config.print_config()
    
    return train_model_with_config('roberta_base', config)


if __name__ == "__main__":
    try:
        success, best_f1, best_metrics = train_roberta()
        if success:
            print("✅ RoBERTa模型训练成功!")
            print(f"📊 最终结果: F1={best_f1:.4f}, Acc={best_metrics.get('accuracy', 0):.4f}")
            print("📈 训练曲线和日志已保存到 ../model/roberta_base/visualizations/")
        else:
            print("❌ RoBERTa模型训练失败!")
    except Exception as e:
        print(f"❌ 训练过程中发生错误: {e}")
        import traceback
        traceback.print_exc()