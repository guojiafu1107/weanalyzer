import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

# Render 部署时自动注入 DATABASE_URL，否则使用本地默认连接
DATABASE_URL = os.getenv("DATABASE_URL", "postgresql+pg8000://postgres:guo1988jiafu@localhost:5432/weanalyzer")

# Render 提供的 DATABASE_URL 格式为 postgresql://...，需加上 +pg8000
if DATABASE_URL.startswith("postgresql://") and "+pg8000" not in DATABASE_URL:
    DATABASE_URL = DATABASE_URL.replace("postgresql://", "postgresql+pg8000://", 1)

engine = create_engine(DATABASE_URL, echo=False)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
