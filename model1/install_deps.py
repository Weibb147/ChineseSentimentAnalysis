#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
快速安装依赖脚本
"""
import subprocess
import sys

def install_package(package):
    """安装Python包"""
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", package])
        print(f"✅ 成功安装: {package}")
        return True
    except subprocess.CalledProcessError:
        print(f"❌ 安装失败: {package}")
        return False

def main():
    print("🚀 开始安装FastAPI依赖包...")
    print("=" * 50)
    
    # 基础依赖
    basic_packages = [
        "fastapi>=0.68.0",
        "uvicorn>=0.15.0", 
        "torch>=1.9.0",
        "transformers>=4.20.0",
        "pydantic>=1.8.0",
        "numpy>=1.21.0",
        "python-multipart>=0.0.6",
        "safetensors"
    ]
    
    print("📦 安装基础依赖...")
    for package in basic_packages:
        install_package(package)
    
    # 可选依赖
    optional_packages = [
        "pydantic-settings>=2.0.0"
    ]
    
    print("\n📦 尝试安装可选依赖...")
    for package in optional_packages:
        install_package(package)
    
    print("\n🎉 依赖安装完成！")
    print("现在可以运行: python start_fastapi.py")
    
    # 测试导入
    print("\n🧪 测试导入...")
    try:
        from config import settings
        print("✅ 配置文件导入成功")
    except ImportError:
        from config_simple import settings
        print("✅ 使用简化配置文件")
    
    try:
        import fastapi
        import uvicorn
        import torch
        import transformers
        print("✅ 核心依赖导入成功")
    except ImportError as e:
        print(f"❌ 导入失败: {e}")
        print("请手动安装缺失的包")

if __name__ == "__main__":
    main()