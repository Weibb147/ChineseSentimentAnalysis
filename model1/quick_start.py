#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
FastAPI快速启动脚本 - 自动处理依赖问题
"""

def main():
    print("🚀 FastAPI情感分析服务启动器")
    print("=" * 40)
    
    # 检查依赖
    try:
        import fastapi
        import uvicorn  
        import torch
        import transformers
        import pydantic
        print("✅ 基础依赖检查通过")
    except ImportError as e:
        print(f"❌ 缺少依赖: {e}")
        print("请运行: python install_deps.py")
        return
    
    # 检查配置
    try:
        from config import settings
        print("✅ 配置文件加载成功")
    except ImportError:
        from config_simple import settings  
        print("✅ 使用简化配置文件")
    
    print(f"🌐 服务地址: {settings.host}:{settings.port}")
    print(f"📝 日志级别: {settings.log_level}")
    
    # 启动服务
    try:
        import uvicorn
        from main import app
        
        print("\n🔥 正在启动FastAPI服务...")
        print("按 Ctrl+C 停止服务")
        print("-" * 40)
        
        uvicorn.run(
            "main:app",
            host=settings.host,
            port=settings.port,
            log_level=settings.log_level.lower(),
            workers=settings.workers,
            access_log=True,
            reload=False
        )
        
    except KeyboardInterrupt:
        print("\n👋 服务已停止")
    except Exception as e:
        print(f"❌ 启动失败: {e}")


if __name__ == "__main__":
    main()