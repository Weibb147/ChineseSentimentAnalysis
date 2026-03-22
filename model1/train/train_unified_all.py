#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
统一配置批量训练脚本 - 依次训练三个模型
使用统一配置确保训练结果的可比性和稳定性
"""

import torch
import time
import json
import os
import sys
from datetime import datetime
from typing import Dict, Any, List, Tuple

# 导入训练函数
from train_roberta import train_roberta
from train_roberta_bilstm_attention import train_bilstm
from train_roberta_gru_attention import train_roberta_gru_attention


class UnifiedModelTrainer:
    """统一模型训练管理器"""
    
    def __init__(self):
        self.results = {}
        self.start_time = time.time()
        
    def train_single_model(self, model_name: str, train_func, description: str) -> Tuple[bool, float, Dict[str, Any]]:
        """训练单个模型"""
        print(f"\n{'='*80}")
        print(f"🚀 开始训练: {description}")
        print(f"{'='*80}")
        
        model_start_time = time.time()
        
        try:
            # 训练模型
            success, best_f1, best_metrics = train_func()
            
            model_time = time.time() - model_start_time
            
            if success:
                print(f"✅ {model_name} 训练成功!")
                print(f"📊 最终结果: F1={best_f1:.4f}, Acc={best_metrics.get('accuracy', 0):.4f}")
                print(f"⏱️ 训练耗时: {model_time:.2f}秒 ({model_time/60:.1f}分钟)")
            else:
                print(f"❌ {model_name} 训练失败!")
                
            return success, best_f1, best_metrics
            
        except Exception as e:
            print(f"❌ {model_name} 训练过程中发生错误: {e}")
            import traceback
            traceback.print_exc()
            return False, 0.0, {}
    
    def train_all_models(self):
        """训练所有模型"""
        print("🎯 统一配置批量训练开始")
        print(f"⏰ 开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"🖥️ 硬件配置: R6800H + RTX3060 8GB + 16GB内存")
        print(f"📊 数据集: SMP2020通用数据集 (6类情绪分类)")
        print(f"🔧 使用统一配置，确保训练结果可比性")
        
        # 定义要训练的模型
        models_to_train = [
            {
                'name': 'roberta_base',
                'func': train_roberta,
                'description': 'RoBERTa基础模型 (统一配置)'
            },
            {
                'name': 'roberta_bilstm_attention', 
                'func': train_bilstm,
                'description': 'RoBERTa + BiLSTM + Attention (统一配置)'
            },
            {
                'name': 'roberta_gru_attention',
                'func': train_roberta_gru_attention,
                'description': 'RoBERTa + GRU + Attention (统一配置)'
            }
        ]
        
        # 依次训练每个模型
        for i, model_info in enumerate(models_to_train, 1):
            print(f"\n📈 进度: {i}/{len(models_to_train)}")
            
            success, best_f1, best_metrics = self.train_single_model(
                model_info['name'],
                model_info['func'],
                model_info['description']
            )
            
            # 记录结果
            self.results[model_info['name']] = {
                'success': success,
                'best_f1': best_f1,
                'best_metrics': best_metrics,
                'description': model_info['description']
            }
            
            # 清理GPU内存
            if torch.cuda.is_available():
                torch.cuda.empty_cache()
                print(f"🧹 已清理GPU内存")
            
            # 模型间暂停，让GPU冷却
            if i < len(models_to_train):
                print(f"\n⏸️ 模型 {model_info['name']} 训练完成，等待30秒后继续...")
                time.sleep(30)
        
        # 训练完成总结
        self.print_summary()
        self.save_results()
    
    def print_summary(self):
        """打印训练总结"""
        total_time = time.time() - self.start_time
        
        print(f"\n{'='*100}")
        print("🎉 所有模型训练完成!")
        print(f"{'='*100}")
        print(f"⏰ 总耗时: {total_time:.2f}秒 ({total_time/3600:.2f}小时)")
        print(f"📅 完成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"🔧 配置: 统一训练配置，确保结果可比性")
        print()
        
        # 打印每个模型的结果
        print("📊 各模型训练结果对比:")
        print("-" * 100)
        
        for model_name, result in self.results.items():
            status = "✅ 成功" if result['success'] else "❌ 失败"
            f1_score = result['best_f1'] if result['success'] else 0.0
            accuracy = result['best_metrics'].get('accuracy', 0.0) if result['success'] else 0.0
            
            print(f"{result['description']:<45} | {status:<8} | F1: {f1_score:.4f} | Acc: {accuracy:.4f}")
        
        print("-" * 100)
        
        # 找出最佳模型
        successful_models = {k: v for k, v in self.results.items() if v['success']}
        if successful_models:
            best_model = max(successful_models.items(), key=lambda x: x[1]['best_f1'])
            print(f"🏆 最佳模型: {best_model[1]['description']}")
            print(f"🥇 最佳F1分数: {best_model[1]['best_f1']:.4f}")
            
            # 性能分析
            f1_scores = [v['best_f1'] for v in successful_models.values()]
            avg_f1 = sum(f1_scores) / len(f1_scores)
            max_f1 = max(f1_scores)
            min_f1 = min(f1_scores)
            
            print(f"\n📈 性能分析:")
            print(f"  平均F1分数: {avg_f1:.4f}")
            print(f"  最高F1分数: {max_f1:.4f}")
            print(f"  最低F1分数: {min_f1:.4f}")
            print(f"  F1分数范围: {max_f1 - min_f1:.4f}")
        
        print()
    
    def save_results(self):
        """保存训练结果到文件"""
        # 创建结果目录
        results_dir = "../model/unified_training_results"
        os.makedirs(results_dir, exist_ok=True)
        
        # 生成结果文件名
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        results_file = os.path.join(results_dir, f"unified_training_results_{timestamp}.json")
        
        # 准备结果数据
        results_data = {
            'training_info': {
                'start_time': datetime.fromtimestamp(self.start_time).strftime('%Y-%m-%d %H:%M:%S'),
                'end_time': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
                'total_duration_seconds': time.time() - self.start_time,
                'hardware_config': {
                    'cpu': 'R6800H',
                    'gpu': 'RTX3060 8GB',
                    'memory': '16GB'
                },
                'dataset': 'SMP2020通用数据集',
                'task': '6类情绪分类',
                'configuration': '统一配置',
                'description': '使用统一配置确保三个模型训练结果可比性'
            },
            'models': {}
        }
        
        # 添加每个模型的结果
        for model_name, result in self.results.items():
            results_data['models'][model_name] = {
                'description': result['description'],
                'success': result['success'],
                'best_f1_score': result['best_f1'],
                'best_metrics': result['best_metrics']
            }
        
        # 保存到文件
        with open(results_file, 'w', encoding='utf-8') as f:
            json.dump(results_data, f, ensure_ascii=False, indent=4)
        
        print(f"📄 统一训练结果已保存到: {results_file}")
        
        # 如果有成功的模型，也保存一个简化的对比文件
        successful_models = {k: v for k, v in self.results.items() if v['success']}
        if successful_models:
            comparison_file = os.path.join(results_dir, f"unified_model_comparison_{timestamp}.txt")
            
            with open(comparison_file, 'w', encoding='utf-8') as f:
                f.write("统一配置模型训练结果对比\n")
                f.write("=" * 50 + "\n")
                f.write(f"训练时间: {datetime.fromtimestamp(self.start_time).strftime('%Y-%m-%d %H:%M:%S')}\n")
                f.write(f"总耗时: {time.time() - self.start_time:.2f}秒\n")
                f.write(f"配置类型: 统一配置\n\n")
                
                for model_name, result in successful_models.items():
                    f.write(f"{result['description']}\n")
                    f.write(f"  F1 Score: {result['best_f1']:.4f}\n")
                    f.write(f"  Accuracy: {result['best_metrics'].get('accuracy', 0.0):.4f}\n")
                    f.write(f"  Precision: {result['best_metrics'].get('precision', 0.0):.4f}\n")
                    f.write(f"  Recall: {result['best_metrics'].get('recall', 0.0):.4f}\n")
                    f.write("-" * 30 + "\n")
            
            print(f"📊 统一配置模型对比报告已保存到: {comparison_file}")


def main():
    """主函数"""
    print("🎯 统一配置批量训练脚本")
    print("针对R6800H + RTX3060 8GB + 16GB内存")
    print("数据集: SMP2020通用数据集 (6类情绪分类)")
    print("特性: 使用统一配置确保三个模型训练结果可比性")
    print()
    
    # 检查CUDA可用性
    if torch.cuda.is_available():
        print(f"✅ 检测到CUDA设备: {torch.cuda.get_device_name(0)}")
        print(f"📊 GPU显存: {torch.cuda.get_device_properties(0).total_memory / 1024**3:.1f}GB")
    else:
        print("⚠️ 未检测到CUDA设备，将使用CPU训练（速度较慢）")
    
    # 创建训练器并开始训练
    trainer = UnifiedModelTrainer()
    trainer.train_all_models()


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n⏹️ 用户中断训练")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ 批量训练过程中发生错误: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)