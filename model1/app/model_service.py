# -*- coding: utf-8 -*-
"""
模型服务类 - 用于加载和预测情感分析模型
"""

import os
import sys

# Allow multiple OpenMP runtimes (fixes 0xC0000005 crash on Windows)
os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"

import torch
import json
import logging
import datetime
import re
from typing import Dict, List, Optional, Union
from transformers import AutoTokenizer
try:
    import jieba
    from jieba import analyse as jieba_analyse
    JIEBA_AVAILABLE = True
except Exception:
    JIEBA_AVAILABLE = False

# 设置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler(os.path.join(os.path.dirname(__file__), '..', 'logs', 'model_service.log'), encoding='utf-8')
    ]
)
logger = logging.getLogger(__name__)

# 确保日志目录存在
os.makedirs(os.path.join(os.path.dirname(__file__), '..', 'logs'), exist_ok=True)

# 导入模型类
train_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'train'))
sys.path.insert(0, train_path)

try:
    from models import RobertaBase, RobertaBiLSTM, RobertaGRU, BiLSTMAttention
    
    # 使用 importlib 导入 config 以避免与根目录 config.py 冲突
    import importlib.util
    config_path = os.path.join(train_path, 'config.py')
    
    # Check if file exists to provide better error message
    if not os.path.exists(config_path):
        raise FileNotFoundError(f"Config file not found at {config_path}")

    spec = importlib.util.spec_from_file_location("train_config_unique_name", config_path)
    train_config_module = importlib.util.module_from_spec(spec)
    # Important: Add to sys.modules to handle internal relative imports if any
    sys.modules["train_config_unique_name"] = train_config_module
    spec.loader.exec_module(train_config_module)
    Config = train_config_module.Config
    
    logger.info(f"成功导入模型类和配置 (from {config_path})")
except Exception as e:
    logger.error(f"导入模型类时出错: {str(e)}")
    # 为了避免服务启动失败，提供备用的简单模型定义
    logger.warning("提供备用模型定义")
    
    class RobertaBase(torch.nn.Module):
        def __init__(self, config):
            super().__init__()
            self.config = config
            self.dummy_layer = torch.nn.Linear(768, config.num_labels)
        
        def forward(self, input_ids, attention_mask=None, labels=None):
            class ModelOutput:
                def __init__(self, loss, logits):
                    self.loss = loss
                    self.logits = logits
            batch_size = input_ids.shape[0]
            logits = torch.zeros(batch_size, self.config.num_labels).to(input_ids.device)
            return ModelOutput(None, logits)
    
    RobertaBiLSTM = RobertaGRU = BiLSTMAttention = RobertaBase
    
    class Config:
        def __init__(self, model_type='roberta_base', **kwargs):
            self.model_name_or_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'train', 'pretrained_models', 'chinese-roberta-wwm-ext'))
            self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
            self.max_seq_length = 256
            self.num_labels = 6
            self.output_dir = '../model'
            self.model_type = model_type
            # 兼容性字段
            self.hidden_size = 768
            self.num_attention_heads = 12
            self.dropout_rate = 0.1
            self.lstm_hidden_size = 256
            self.lstm_num_layers = 2
            self.gru_hidden_size = 256
            self.gru_num_layers = 2
            self.backbone_lr = 1e-5
            self.head_lr = 1e-4
            
            # 手动处理kwargs
            for k, v in kwargs.items():
                setattr(self, k, v)



class ModelService:
    """模型服务类，负责加载和预测情感分析模型"""
    
    # 模型类型映射
    MODEL_CLASSES = {
        'roberta_base': RobertaBase,
        'roberta_bilstm_attention': RobertaBiLSTM,
        'roberta_gru_attention': RobertaGRU,
        'bilstm_attention': BiLSTMAttention
    }
    
    # 模型描述信息
    MODEL_INFO = {
        'roberta_base': {
            'name': 'RoBERTa Base',
            'description': '基础RoBERTa模型，准确可靠',
            'speed': 'fast',
            'accuracy': 'high',
            'recommended': True
        },
        'roberta_bilstm_attention': {
            'name': 'RoBERTa + BiLSTM + Attention',
            'description': '结合BiLSTM和注意力机制，更准确',
            'speed': 'medium',
            'accuracy': 'very_high',
            'recommended': False
        },
        'roberta_gru_attention': {
            'name': 'RoBERTa + GRU + Attention',
            'description': '结合GRU和注意力机制，快速准确',
            'speed': 'fast',
            'accuracy': 'high',
            'recommended': False
        },
        'bilstm_attention': {
            'name': 'BiLSTM + Attention (Baseline)',
            'description': '纯BiLSTM+注意力基线模型，不使用RoBERTa预训练',
            'speed': 'fast',
            'accuracy': 'medium',
            'recommended': False
        }
    }
    
    MODEL_ID_MAP = {
        1: 'roberta_base',
        2: 'roberta_bilstm_attention',
        3: 'roberta_gru_attention',
        4: 'bilstm_attention'
    }
    
    # 标签映射
    LABEL_MAP = {
        0: 'angry',
        1: 'happy',
        2: 'sad',
        3: 'neutral',
        4: 'surprise',
        5: 'fear'
    }
    
    # 模型缓存
    _model_cache = {}
    _tokenizer_cache = None
    
    def __init__(self):
        """初始化模型服务"""
        try:
            self.config = Config()
            self.device = self.config.device
            logger.info(f"模型服务初始化，使用设备: {self.device}")
            
            # Update LABEL_MAP from config if available
            if hasattr(self.config, 'label_list'):
                 self.LABEL_MAP = {i: label for i, label in enumerate(self.config.label_list)}
                 logger.info(f"Using label map from config: {self.LABEL_MAP}")
        except Exception as e:
            logger.error(f"初始化配置时出错: {str(e)}")
            # 使用默认配置
            self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
            logger.warning(f"使用默认配置，设备: {self.device}")
    
    def get_tokenizer(self) -> AutoTokenizer:
        """获取tokenizer，使用缓存避免重复加载"""
        if self._tokenizer_cache is None:
            try:
                model_path = getattr(self.config, 'model_name_or_path', 'hfl/chinese-roberta-wwm-ext')
                logger.info(f"加载tokenizer: {model_path}")
                
                # 确保使用本地路径
                if os.path.exists(model_path):
                    logger.info(f"使用本地tokenizer路径: {model_path}")
                    self._tokenizer_cache = AutoTokenizer.from_pretrained(model_path, local_files_only=True)
                else:
                    logger.info(f"本地路径不存在，尝试在线加载: {model_path}")
                    self._tokenizer_cache = AutoTokenizer.from_pretrained(model_path)
                    
                logger.info("Tokenizer 加载成功")
            except Exception as e:
                logger.error(f"加载tokenizer时出错: {str(e)}")
                logger.warning("尝试使用hfl/chinese-roberta-wwm-ext在线tokenizer")
                # 尝试使用在线模型
                try:
                    self._tokenizer_cache = AutoTokenizer.from_pretrained('hfl/chinese-roberta-wwm-ext')
                    logger.info("在线Tokenizer 加载成功")
                except Exception as e2:
                    logger.critical(f"无法加载任何tokenizer: {str(e2)}")
                    raise RuntimeError("无法初始化tokenizer，请检查模型路径和网络连接")
        return self._tokenizer_cache

    def normalize_model_type(self, model_type: Union[str, int, None]) -> str:
        if model_type is None:
            return 'roberta_base'
        if isinstance(model_type, int):
            return self.MODEL_ID_MAP.get(model_type, 'roberta_base')
        model_type_str = str(model_type).strip()
        if model_type_str.isdigit():
            return self.MODEL_ID_MAP.get(int(model_type_str), 'roberta_base')
        raw = model_type_str
        lowered = raw.lower().strip()
        lowered = re.sub(r'\s+', ' ', lowered)
        compact = re.sub(r'[^a-z0-9]+', '_', lowered).strip('_')

        alias_map = {
            'roberta_gru': 'roberta_gru_attention',
            'roberta_gru_att': 'roberta_gru_attention',
            'roberta_gru_attention': 'roberta_gru_attention',
            'roberta_bilstm': 'roberta_bilstm_attention',
            'roberta_lstm': 'roberta_bilstm_attention',
            'roberta_bilstm_att': 'roberta_bilstm_attention',
            'roberta_bilstm_attention': 'roberta_bilstm_attention'
        }
        if compact in alias_map:
            return alias_map[compact]

        if compact in self.MODEL_CLASSES:
            return compact

        if compact in ('roberta', 'roberta_base', 'base', 'baseline'):
            return 'roberta_base'

        if 'roberta' in compact and 'bilstm' in compact:
            return 'roberta_bilstm_attention'
        if 'roberta' in compact and 'gru' in compact:
            return 'roberta_gru_attention'

        if 'bilstm' in compact and 'attention' in compact and 'roberta' not in compact:
            return 'bilstm_attention'

        if ('bilstm' in compact or 'lstm' in compact) and 'roberta' not in compact and 'attention' not in compact:
            return 'bilstm_attention'

        return model_type_str
    
    def load_model(self, model_type: Union[str, int]) -> torch.nn.Module:
        """
        加载指定类型的模型
        
        Args:
            model_type: 模型类型，支持 'roberta_base', 'roberta_bilstm_attention', 'roberta_gru_attention'
            
        Returns:
            加载好的模型实例
            
        Raises:
            ValueError: 如果模型类型不支持
        """
        model_type = self.normalize_model_type(model_type)
        # 检查模型类型是否支持
        if model_type not in self.MODEL_CLASSES:
            error_msg = f"不支持的模型类型: {model_type}，支持的类型: {list(self.MODEL_CLASSES.keys())}"
            logger.error(error_msg)
            raise ValueError(error_msg)
        
        # 检查缓存
        if model_type in self._model_cache:
            logger.info(f"从缓存加载模型: {model_type}")
            return self._model_cache[model_type]
        
        try:
            # 获取模型类
            model_class = self.MODEL_CLASSES[model_type]
            
            # 创建特定模型的配置
            # 注意：Config类现在需要model_type参数来正确初始化特定模型的超参数(如lstm_hidden_size)
            model_config = None
            
            # 策略1: 关键字参数 (标准方式)
            if model_config is None:
                try:
                    model_config = Config(model_type=model_type)
                except Exception:
                    pass
            
            # 策略2: 位置参数
            if model_config is None:
                try:
                    model_config = Config(model_type)
                except Exception:
                    pass
            
            # 策略3: 无参数 + 手动设置属性 (兜底)
            if model_config is None:
                try:
                    model_config = Config()
                    if hasattr(model_config, 'model_type'):
                        model_config.model_type = model_type
                    else:
                        setattr(model_config, 'model_type', model_type)
                    
                    # 尝试应用第四阶段优化
                    apply_opt = getattr(model_config, '_apply_stage4_optimization', None)
                    if callable(apply_opt):
                        try:
                            apply_opt(model_type)
                        except Exception:
                            pass
                except Exception as e:
                    logger.error(f"Config初始化完全失败: {e}")
                    raise e
            
            if model_config is None:
                 raise ValueError("无法初始化模型配置")

            # 确保使用正确的设备
            # 确保使用正确的设备
            model_config.device = self.device
            
            # 创建模型实例
            logger.info(f"创建模型实例: {model_type}")
            model = model_class(model_config)
            
            # 尝试加载预训练权重
            try:
                # 1. Determine base directories to search
                # Ensure we look in the correct absolute path relative to project root
                # model_service.py is in .../model1/app/
                # models are in .../model1/model/
                current_file_dir = os.path.dirname(os.path.abspath(__file__))
                project_root = os.path.dirname(current_file_dir) # .../model1
                base_model_dir = os.path.join(project_root, 'model')
                
                logger.info(f"Using base model directory: {base_model_dir}")

                search_dirs_base = []
                
                # Direct match
                direct_path = os.path.join(base_model_dir, model_type)
                if os.path.exists(direct_path):
                    search_dirs_base.append(direct_path)
                
                # Unified prefix match
                unified_path = os.path.join(base_model_dir, f"unified_{model_type}")
                if os.path.exists(unified_path):
                    search_dirs_base.append(unified_path)
                
                # If no specific directory found, try the root (legacy)
                if not search_dirs_base:
                     search_dirs_base = [base_model_dir]
                     logger.warning(f"未找到特定模型目录，将尝试在根模型目录搜索: {base_model_dir}")

                final_search_dirs = []
                for base_dir in search_dirs_base:
                    # Check for timestamped subdirectories
                    if os.path.isdir(base_dir):
                         subdirs = [d for d in os.listdir(base_dir) if os.path.isdir(os.path.join(base_dir, d)) and d.startswith('202')]
                         if subdirs:
                             subdirs.sort(reverse=True)
                             latest_subdir = os.path.join(base_dir, subdirs[0])
                             final_search_dirs.append(latest_subdir)
                             logger.info(f"添加最新时间戳目录: {latest_subdir}")
                    
                    # Also search the base dir itself
                    final_search_dirs.append(base_dir)

                weight_loaded = False
                for search_dir in final_search_dirs:
                    if not search_dir or not os.path.exists(search_dir):
                        continue
                    
                    # 查找权重文件
                    possible_files = [
                        'model.safetensors',
                        'best_model.pt',
                        'pytorch_model.bin'
                    ]
                    
                    # 添加所有以best_model开头的文件
                    if os.path.exists(search_dir):
                        for f in os.listdir(search_dir):
                            if f.startswith('best_model') and f.endswith('.pt'):
                                possible_files.append(f)
                            elif f.endswith('.safetensors'):
                                possible_files.append(f)
                    
                    logger.info(f"在目录 {search_dir} 中搜索权重文件: {possible_files}")
                    
                    for model_file in possible_files:
                        model_path = os.path.join(search_dir, model_file)
                        if not os.path.exists(model_path):
                            continue
                        
                        try:
                            logger.info(f"尝试加载权重文件: {model_path}")
                            
                            if model_file.endswith('.safetensors'):
                                try:
                                    from safetensors.torch import load_file
                                    state_dict = load_file(model_path, device=str(self.device))
                                    logger.info(f"成功加载safetensors文件 ({len(state_dict)} 个参数)")
                                except ImportError:
                                    logger.error("未安装safetensors库，无法加载.safetensors文件")
                                    continue
                            else:
                                try:
                                    checkpoint = torch.load(model_path, map_location=self.device, weights_only=False)
                                except TypeError:
                                    checkpoint = torch.load(model_path, map_location=self.device)
                                
                                # 处理不同的权重格式
                                if isinstance(checkpoint, dict):
                                    if 'model_state_dict' in checkpoint:
                                        state_dict = checkpoint['model_state_dict']
                                        logger.info(f"从完整检查点提取模型权重 ({len(state_dict)} 个参数)")
                                    elif 'state_dict' in checkpoint:
                                        state_dict = checkpoint['state_dict']
                                        logger.info(f"从检查点提取模型权重 ({len(state_dict)} 个参数)")
                                    else:
                                        state_dict = checkpoint
                                        logger.info(f"加载纯模型权重 ({len(state_dict)} 个参数)")
                                else:
                                    logger.error("权重文件格式不正确")
                                    continue
                            
                            # 过滤掉不需要的参数
                            model_dict = model.state_dict()
                            filtered_state_dict = {k: v for k, v in state_dict.items() if k in model_dict and v.size() == model_dict[k].size()}
                            if len(filtered_state_dict) != len(model_dict):
                                logger.warning(f"权重参数匹配不完全: {len(filtered_state_dict)}/{len(model_dict)}")
                            
                            model.load_state_dict(filtered_state_dict, strict=False)
                            weight_loaded = True
                            logger.info(f"✅ 模型权重加载成功: {model_file}")
                            break
                        except Exception as e:
                            logger.warning(f"❌ 加载 {model_file} 时出错: {str(e)}")
                            continue
                    
                    if weight_loaded:
                        break
                
                if not weight_loaded:
                    logger.warning(f"⚠️ 未找到可加载的模型权重文件，使用随机初始化的模型")
                    logger.warning(f"搜索路径: {final_search_dirs}")
                else:
                    logger.info(f"✅ 模型权重加载完成")
                    
            except Exception as e:
                logger.error(f"加载模型权重过程中出错: {str(e)}")
                logger.warning("使用随机初始化的模型")
            
            # 移动到设备并设置为评估模式
            model.to(self.device)
            model.eval()
            
            # 缓存模型
            self._model_cache[model_type] = model
            logger.info(f"模型 {model_type} 加载完成")
            
            return model
        except Exception as e:
            logger.error(f"加载模型 {model_type} 时出错: {str(e)}")
            raise RuntimeError(f"模型加载失败: {str(e)}")
    
    def preprocess_text(self, text: str) -> Dict[str, torch.Tensor]:
        """
        预处理文本
        
        Args:
            text: 输入文本
            
        Returns:
            预处理后的张量字典
        """
        try:
            tokenizer = self.get_tokenizer()
            
            encoding = tokenizer(
                text,
                truncation=True,
                padding='max_length',
                max_length=getattr(self.config, 'max_seq_length', 256),
                return_tensors='pt'
            )
            
            # 移除token_type_ids，因为自定义模型不支持这个参数
            if 'token_type_ids' in encoding:
                del encoding['token_type_ids']
            
            # 移动到设备
            encoding = {key: val.to(self.device) for key, val in encoding.items()}
            
            return encoding
        except Exception as e:
            logger.error(f"预处理文本时出错: {str(e)}")
            raise RuntimeError(f"文本预处理失败: {str(e)}")
    
    def predict(self, text: str, model_type: Union[str, int] = 'roberta_base') -> Dict[str, Union[str, float, List[float]]]:
        """
        预测文本的情感
        
        Args:
            text: 要预测的文本
            model_type: 模型类型
            
        Returns:
            包含预测结果的字典
            
        Raises:
            ValueError: 如果输入参数无效
            RuntimeError: 如果预测过程失败
        """
        model_type = self.normalize_model_type(model_type)
        logger.info(f"开始预测，模型类型: {model_type}")
        
        try:
            # 加载模型
            model = self.load_model(model_type)
            
            # 预处理文本
            encoding = self.preprocess_text(text)
            
            # 预测
            with torch.no_grad():
                outputs = model(**encoding)
                logits = outputs.logits
                probabilities = torch.softmax(logits, dim=-1)
                predicted_class_id = torch.argmax(logits, dim=-1).item()
                confidence = torch.max(probabilities, dim=-1).values.item()
                probs = probabilities.cpu().numpy().flatten().tolist()
            
            # 获取标签
            predicted_label = self.LABEL_MAP.get(predicted_class_id, 'unknown')
            
            result = {
                'text': text,
                'predicted_label': predicted_label,
                'confidence': confidence,
                'probabilities': {self.LABEL_MAP[i]: prob for i, prob in enumerate(probs)},
                'model_type': model_type,
                'model_info': self.MODEL_INFO.get(model_type, {})
            }
            
            logger.info(f"预测完成: {predicted_label} (置信度: {confidence:.4f})")
            return result
            
        except Exception as e:
            logger.error(f"预测过程中出错: {str(e)}")
            return {
                'text': text,
                'error': str(e),
                'model_type': model_type
            }
    
    def batch_predict(self, texts: List[str], model_type: Union[str, int] = 'roberta_base') -> List[Dict[str, Union[str, float, List[float]]]]:
        """
        批量预测文本情感
        
        Args:
            texts: 要预测的文本列表
            model_type: 模型类型
            
        Returns:
            预测结果列表
        """
        model_type = self.normalize_model_type(model_type)
        logger.info(f"开始批量预测，文本数量: {len(texts)}, 模型类型: {model_type}")
        
        try:
            results = []
            for text in texts:
                result = self.predict(text, model_type)
                results.append(result)
            
            logger.info(f"批量预测完成，返回 {len(results)} 个结果")
            return results
        except Exception as e:
            logger.error(f"批量预测过程中出错: {str(e)}")
            raise RuntimeError(f"批量预测失败: {str(e)}")
    
    def get_available_models(self) -> List[Dict]:
        """获取可用模型列表"""
        models = []
        for model_id, info in self.MODEL_INFO.items():
            model_entry = info.copy()
            model_entry['id'] = model_id
            # 检查模型文件是否存在
            model_entry['available'] = True # 暂时默认都可用，实际应该检查文件
            model_entry['active'] = (model_id == 'roberta_base') # 默认激活
            models.append(model_entry)
        return models
    
    def clear_cache(self):
        """清除模型缓存"""
        self._model_cache.clear()
        self._tokenizer_cache = None
        logger.info("模型缓存已清除")
    
    def extract_keywords(self, text: str, top_k: int = 12) -> Dict[str, int]:
        try:
            if not text or not text.strip():
                return {}
            top_k = max(1, min(int(top_k or 12), 200))
            STOPWORDS = {
                '的','了','和','是','就','都','而','及','与','着','或','一个','没有','我们','你们','他们','它们','是否',
                '以及','因为','所以','虽然','但是','如果','不是','就是','只是','还是','只有','不要','不能','可以','这个','那个',
                '这些','那些','自己','什么','怎么','怎样','能够','应该','可能','必须','一定','非常','十分','特别','比较','感觉',
                '觉得','认为','以为','本来','原来','其实','结果','最后','一直','已经','曾经','正在','将要','有点','一点','一些',
                '[CLS]','[SEP]','[PAD]','[UNK]','[MASK]'
            }
            freq: Dict[str, int] = {}
            if JIEBA_AVAILABLE:
                try:
                    words = list(jieba.cut(text))
                    for w in words:
                        w = w.strip()
                        if not w or w in STOPWORDS:
                            continue
                        if len(w) <= 1 and not w.isalpha():
                            continue
                        freq[w] = freq.get(w, 0) + 1
                    try:
                        tfidf = jieba_analyse.extract_tags(text, topK=top_k*2, withWeight=True)
                        for w, wt in tfidf:
                            if w in STOPWORDS:
                                continue
                            inc = max(1, int(wt * 10))
                            freq[w] = freq.get(w, 0) + inc
                    except Exception:
                        pass
                except Exception:
                    pass
            if not freq:
                tokenizer = self.get_tokenizer()
                tokens = tokenizer.tokenize(text)
                import re
                for tok in tokens:
                    if tok.startswith('##'):
                        tok = tok[2:]
                    t = tok.strip()
                    if not t or t in STOPWORDS:
                        continue
                    if len(t) <= 1:
                        continue
                    if re.match(r"^[\u4e00-\u9fa5a-zA-Z0-9]+$", t) is None:
                        continue
                    freq[t] = freq.get(t, 0) + 1
            items = sorted(freq.items(), key=lambda x: x[1], reverse=True)[:top_k]
            return {k: v for k, v in items}
        except Exception as e:
            logger.error(str(e))
            return {}


# 创建全局模型服务实例
model_service = ModelService()


# 用于测试的主函数
if __name__ == "__main__":
    # 测试预测功能
    test_texts = [
        "今天天气真好，心情很愉快！",
        "工作压力很大，感觉很累。",
        "这个消息太令人惊讶了！"
    ]
    
    print("\n测试单个预测:")
    result = model_service.predict(test_texts[0])
    print(json.dumps(result, ensure_ascii=False, indent=2))
    
    print("\n测试批量预测:")
    results = model_service.predict_batch(test_texts)
    for i, res in enumerate(results):
        print(f"\n预测结果 {i+1}:")
        print(json.dumps(res, ensure_ascii=False, indent=2))
