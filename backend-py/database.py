from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

# Use pg8000 (pure Python) to avoid psycopg2 encoding issues with Chinese paths
engine = create_engine(
    "postgresql+pg8000://postgres:guo1988jiafu@localhost:5432/weanalyzer",
    echo=False
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
