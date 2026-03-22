# -*- coding: utf-8 -*-
"""
数据处理模块
负责数据集的加载、预处理和DataLoader的创建
"""

import os
import json
import torch
from torch.utils.data import Dataset, DataLoader
from transformers import AutoTokenizer


class SentimentDataset(Dataset):
    """情感分析数据集"""
    
    # 标签映射
    LABEL_MAP = {
        'angry': 0,
        'happy': 1, 
        'sad': 2,
        'neutral': 3,
        'surprise': 4,
        'fear': 5
    }
    
    def __init__(self, texts, labels, tokenizer, max_length=128):
        self.texts = texts
        self.labels = labels
        self.tokenizer = tokenizer
        self.max_length = max_length
    
    def __len__(self):
        return len(self.texts)
    
    def __getitem__(self, idx):
        text = str(self.texts[idx])
        label_str = str(self.labels[idx])
        
        # 将字符串标签转换为数字
        if label_str in self.LABEL_MAP:
            label = self.LABEL_MAP[label_str]
        else:
            # 如果标签不在映射中，默认为neutral
            label = self.LABEL_MAP['neutral']
        
        # 编码文本
        encoding = self.tokenizer(
            text,
            truncation=True,
            padding='max_length',
            max_length=self.max_length,
            return_tensors='pt'
        )
        
        return {
            'input_ids': encoding['input_ids'].flatten(),
            'attention_mask': encoding['attention_mask'].flatten(),
            'labels': torch.tensor(label, dtype=torch.long)
        }
    
    @classmethod
    def get_num_labels(cls):
        """获取标签数量"""
        return len(cls.LABEL_MAP)
    
    @classmethod
    def get_label_names(cls):
        """获取标签名称列表"""
        return list(cls.LABEL_MAP.keys())


def load_data_from_file(file_path):
    """从文件加载数据"""
    # 检查文件是否存在
    if not os.path.exists(file_path):
        print(f"⚠️  警告: 文件 {file_path} 不存在")
        return [], []

    print(f"📂 正在加载文件: {file_path}")

    texts = []
    labels = []

    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read().strip()

            # 检查空文件
            if not content:
                print(f"  文件为空: {file_path}")
                return [], []

            # 检查是否是JSON数组格式
            if content.startswith('[') and content.endswith(']'):
                try:
                    data_list = json.loads(content)
                    for item in data_list:
                        if isinstance(item, dict) and 'content' in item and 'label' in item:
                            texts.append(item['content'])
                            labels.append(item['label'])
                    print(f"  成功从JSON数组格式加载 {len(texts)} 条数据")
                    return texts, labels
                except json.JSONDecodeError as e:
                    print(f"  JSON数组解析错误: {e}")
                    pass

            # 逐行处理JSON或制表符分隔格式
            lines = content.split('\n')
            print(f"  文件包含 {len(lines)} 行")

            for i, line in enumerate(lines):
                line = line.strip()
                if not line:
                    continue

                try:
                    # 尝试解析单行JSON格式
                    data = json.loads(line)
                    if isinstance(data, dict) and 'content' in data and 'label' in data:
                        texts.append(data['content'])
                        labels.append(data['label'])
                except json.JSONDecodeError:
                    # 如果不是JSON，尝试按制表符分割
                    parts = line.split('\t')
                    if len(parts) >= 2:
                        texts.append(parts[0])
                        try:
                            # 尝试将标签转换为整数
                            labels.append(int(parts[1]))
                        except ValueError:
                            # 如果无法转换为整数，则保留为字符串
                            labels.append(parts[1])

        print(f"  成功加载 {len(texts)} 条数据")
        return texts, labels
    except Exception as e:
        print(f"  文件读取错误: {e}")
        import traceback
        traceback.print_exc()
        return [], []


def create_datasets(config):
    """创建训练、验证和测试数据集"""
    print("📊 开始加载数据集...")

    # 加载tokenizer
    print("🤖 加载tokenizer...")
    tokenizer = AutoTokenizer.from_pretrained(config.model_name_or_path)

    # 定义文件路径
    train_file = os.path.join(config.data_dir, 'usual_train.txt')
    dev_file = os.path.join(config.data_dir, 'usual_eval_labeled.txt')
    test_file = os.path.join(config.data_dir, 'usual_test_labeled.txt')

    # 检查文件是否存在
    print(f"📁 数据目录: {config.data_dir}")
    print(f"  训练集文件: {train_file} ({'存在' if os.path.exists(train_file) else '不存在'})")
    print(f"  验证集文件: {dev_file} ({'存在' if os.path.exists(dev_file) else '不存在'})")
    print(f"  测试集文件: {test_file} ({'存在' if os.path.exists(test_file) else '不存在'})")

    # 加载数据
    print("📥 加载训练集...")
    train_texts, train_labels = load_data_from_file(train_file)

    print("📥 加载验证集...")
    dev_texts, dev_labels = load_data_from_file(dev_file)

    print("📥 加载测试集...")
    test_texts, test_labels = load_data_from_file(test_file)

    print(f"📊 数据加载完成:")
    print(f"  训练集: {len(train_texts)} 样本")
    print(f"  验证集: {len(dev_texts)} 样本")
    print(f"  测试集: {len(test_texts)} 样本")

    # 检查数据示例
    if train_texts:
        print(f"  训练集示例: '{train_texts[0][:50]}...' -> {train_labels[0]}")
    if dev_texts:
        print(f"  验证集示例: '{dev_texts[0][:50]}...' -> {dev_labels[0]}")
    if test_texts:
        print(f"  测试集示例: '{test_texts[0][:50]}...' -> {test_labels[0]}")

    # 创建数据集
    train_dataset = SentimentDataset(train_texts, train_labels, tokenizer, config.max_seq_length)
    dev_dataset = SentimentDataset(dev_texts, dev_labels, tokenizer, config.max_seq_length)
    test_dataset = SentimentDataset(test_texts, test_labels, tokenizer, config.max_seq_length)

    return train_dataset, dev_dataset, test_dataset, tokenizer


def create_dataloaders(train_dataset, dev_dataset, test_dataset, config):
    """创建数据加载器"""
    print("⚙️  创建数据加载器...")

    train_dataloader = DataLoader(
        train_dataset,
        batch_size=config.train_batch_size,
        shuffle=True,
        num_workers=0,        # 避免多进程内存问题
        pin_memory=False      # 减少内存使用
    )

    dev_dataloader = DataLoader(
        dev_dataset,
        batch_size=config.eval_batch_size,
        shuffle=False,
        num_workers=0,
        pin_memory=False
    )

    test_dataloader = DataLoader(
        test_dataset,
        batch_size=config.eval_batch_size,
        shuffle=False,
        num_workers=0,
        pin_memory=False
    )

    print(f"✅ 数据加载器创建完成")
    print(f"  训练集批次: {len(train_dataloader)}")
    print(f"  验证集批次: {len(dev_dataloader)}")
    print(f"  测试集批次: {len(test_dataloader)}")

    return train_dataloader, dev_dataloader, test_dataloader
