# -*- coding: utf-8 -*-
"""
FastAPI服务启动配置文件
"""
import os

# 尝试导入不同版本的pydantic设置类
try:
    from pydantic_settings import BaseSettings
except ImportError:
    try:
        from pydantic import BaseSettings
    except ImportError:
        # 如果都没有，创建一个简单的替代类
        class BaseSettings:
            def __init__(self, **kwargs):
                for key, value in kwargs.items():
                    setattr(self, key, value)

class Settings(BaseSettings):
    """FastAPI服务配置"""
    
    # 服务配置
    host: str = "0.0.0.0"
    port: int = 8000
    
    # 模型配置
    default_model: str = "roberta_base"
    model_cache_size: int = 3
    max_text_length: int = 1024
    max_batch_size: int = 100
    
    # 日志配置
    log_level: str = "INFO"
    log_file: str = "logs/fastapi.log"
    
    # CORS配置
    cors_origins: list = ["*"]
    cors_methods: list = ["*"]
    cors_headers: list = ["*"]
    
    # 性能配置
    timeout: int = 30
    workers: int = 1

    db_host: str = os.getenv("DB_HOST", "localhost")
    db_port: int = int(os.getenv("DB_PORT", 3306))
    db_user: str = os.getenv("DB_USERNAME", "root")
    db_password: str = os.getenv("DB_PASSWORD", "123456")
    db_name: str = os.getenv("DB_NAME", "bishe003")
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"

# 创建全局配置实例
settings = Settings()
