# 基于RoBERTa的中文多类别情感分析系统

## 📖 项目简介
本项目是一个基于微服务架构的**中文多类别情感分析系统**。系统能够对中文文本进行深度语义分析，并将其归类为 6 种情感（喜、怒、哀、惧、惊、中性）之一。
项目采用了**前后端分离 + AI模型微服务**的架构设计，不仅保证了高并发业务场景下的稳定性，还发挥了 Python 在深度学习领域的生态优势，实现了良好的系统解耦与可扩展性。

## 🛠️ 系统架构与技术栈

### 1. 核心架构设计
- Web交互层 ：负责用户界面展示、交互逻辑以及数据可视化（词云、饼图、折线图）。
- 业务中枢层 ：负责用户鉴权、RBAC权限管理、文件解析、异步任务调度、数据持久化。
- 模型推理层 ：负责加载预训练语言模型（RoBERTa）及深度学习网络（BiLSTM/GRU/Attention），提供高性能的单条/批量文本预测接口。

### 2. 技术栈详细
| 模块 | 核心技术 | 说明 |
| :--- | :--- | :--- |
| **前端 (front)** | Vue 3, Vite, TypeScript, Pinia, Vue Router | 现代化的前端框架体系 |
| **前端 UI/图表** | Element Plus, ECharts | 提供优雅的UI组件与数据可视化能力 |
| **Java后端 (sa-sentiment02)** | Java 17, Spring Boot 3.1.3, Spring WebFlux | 提供高性能的 RESTful API 及异步模型调用 (`WebClient`) |
| **后端安全/存储** | Spring Security, JWT, MyBatis-Plus, MySQL, Redis | 实现无状态的安全认证与可靠的数据持久化 |
| **Python模型端 (model1)** | Python 3.9+, FastAPI | 提供高性能、支持并发的模型微服务接口 |
| **深度学习框架** | PyTorch, Transformers (HuggingFace) | 核心 AI 基础设施 |
| **核心算法模型** | RoBERTa-wwm-ext, BiLSTM, GRU, Attention | 融合预训练模型与经典序列网络的情感分类算法 |

---

## 📂 目录结构

```text
ChineseSentimentAnalysis/
├── front/                # 前端 Vue3 项目代码
│   ├── src/              # 前端源码 (components, views, api, stores)
│   ├── package.json      # 前端依赖配置
│   └── vite.config.ts    # Vite 打包配置
├── sa-sentiment02/       # Java Spring Boot 后端核心业务代码
│   ├── src/main/java/... # 后端源码 (controller, service, mapper, config)
│   ├── src/main/resources# 配置文件 (application.yml, SQL初始化脚本)
│   └── pom.xml           # Maven 依赖配置
├── model1/               # Python FastAPI 模型微服务代码
│   ├── app/              # FastAPI 接口路由与服务层
│   ├── train/            # 模型训练脚本与预训练权重
│   ├── main.py           # FastAPI 服务启动入口
│   └── requirements.txt  # Python 依赖清单
```

---

## ✨ 核心功能亮点

1. **多类别情感识别**：超越传统的正/负面二分类，支持精细化的6分类情感分析。
2. **微服务异步通信**：Java 业务层通过 WebFlux 异步调用 FastAPI 模型接口，完美解决深度学习推理耗时导致的线程阻塞问题。
3. **海量数据防 OOM 处理**：针对用户上传的批量测试文件（Excel/TXT），采用流式读取与分批次（Batch）发送机制，确保系统内存安全。
4. **JWT+Redis 安全认证**：双重校验机制，支持 JWT 主动失效与续期，保障接口调用安全。
5. **数据可视化大屏**：提供全方位的数据分析看板，包括情感占比饼图、时间序列趋势图、高频情感词云提取等。

---

## 🚀 快速开始 / 部署指南

### 1. 环境准备
确保您的开发环境已安装以下基础组件：
- **Node.js** (v18+) & npm/yarn/pnpm
- **JDK 17** & Maven
- **Python 3.9+**
- **MySQL 8.0+**
- **Redis 6.0+**
- **CUDA 11.x**

### 2. 数据库初始化
1. 在 MySQL 中创建数据库 `sentiment_analysis` (推荐 utf8mb4 编码)。
2. 导入 SQL 脚本：执行 `sa-sentiment02/src/main/resources/` 目录下的相关初始化 SQL 文件，生成用户表、任务表、结果表等。
3. 确保 Redis 服务已在本地 `localhost:6379` 启动。

### 3. 后端服务启动 (sa-sentiment02)
1. 进入 `sa-sentiment02` 目录。
2. 修改 `application.yml`  中的 MySQL、Redis以及邮箱配置。
3. 还要修改阿里云OSS 配置(阿里云oss配置于sa-sentiment02/src/main/java/com/wei/common/utils/AliOssUtil.java)。
4. 执行 Maven 编译：
   ```bash
   mvn clean install -DskipTests
   ```
4. 运行 `SystemApplication.java` 启动 Spring Boot 服务（默认端口 `8080`）。

### 4. 模型微服务启动 (model1)
1. 进入 `model1` 目录。
2. 创建并激活 Python 虚拟环境（推荐）：
   ```bash
   python -m venv venv
   source venv/Scripts/activate  # Windows
   ```
3. 安装依赖：
   ```bash
   pip install -r requirements.txt
   ```
4. 启动 FastAPI 服务：
   ```bash
   python main.py
   # 或者使用 uvicorn: uvicorn main:app --reload --host 0.0.0.0 --port 8000
   ```
   *注意：初次启动可能需要加载本地预训练模型权重。*

### 5. 前端服务启动 (front)
1. 进入 `front` 目录。
2. 安装依赖：
   ```bash
   npm install
   ```
3. 启动开发服务器：
   ```bash
   npm run dev
   ```
4. 打开浏览器访问 `http://localhost:5173` 即可体验完整系统。

---

## 📝 许可证
本项目作为毕业设计/个人学习项目开源，未经允许请勿用于商业用途。
