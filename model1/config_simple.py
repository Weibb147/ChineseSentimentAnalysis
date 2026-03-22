# -*- coding: utf-8 -*-
"""
FastAPI服务启动配置文件 - 简化版本
"""
import os

class Settings:
    """FastAPI服务配置"""
    
    def __init__(self):
        # 服务配置
        self.host = os.getenv("FASTAPI_HOST", "0.0.0.0")
        self.port = int(os.getenv("FASTAPI_PORT", 8000))
        
        # 模型配置
        self.default_model = os.getenv("DEFAULT_MODEL", "roberta_base")
        self.model_cache_size = int(os.getenv("MODEL_CACHE_SIZE", 3))
        self.max_text_length = int(os.getenv("MAX_TEXT_LENGTH", 1024))
        self.max_batch_size = int(os.getenv("MAX_BATCH_SIZE", 100))
        
        # 日志配置
        self.log_level = os.getenv("LOG_LEVEL", "INFO")
        self.log_file = os.getenv("LOG_FILE", "logs/fastapi.log")
        
        # CORS配置
        self.cors_origins = os.getenv("CORS_ORIGINS", "*").split(",")
        self.cors_methods = os.getenv("CORS_METHODS", "*").split(",")
        self.cors_headers = os.getenv("CORS_HEADERS", "*").split(",")
        
        # 性能配置
        self.timeout = int(os.getenv("TIMEOUT", 30))
        self.workers = int(os.getenv("WORKERS", 1))

        self.db_host = os.getenv("DB_HOST", "localhost")
        self.db_port = int(os.getenv("DB_PORT", 3306))
        self.db_user = os.getenv("DB_USERNAME", "root")
        self.db_password = os.getenv("DB_PASSWORD", "123456")
        self.db_name = os.getenv("DB_NAME", "bishe003")

# 创建全局配置实例
settings = Settings()
