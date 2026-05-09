# WeAnalyzer Python 后端

基于 FastAPI + SQLAlchemy + PostgreSQL 的轻量级后端，替代 Spring Boot。

## 环境要求
- Python 3.10+
- PostgreSQL 16（已安装）

## 安装依赖

```powershell
cd E:\微信公众号数据分析工具\backend-py
pip install -r requirements.txt
```

## 启动服务

```powershell
python main.py
```

服务启动在 http://localhost:8080

## API 文档

启动后访问：http://localhost:8080/docs

## 配置

修改 `.env` 文件：
- `DATABASE_URL`：PostgreSQL 连接地址
- `ZHIPU_API_KEY`：智谱 AI API Key
