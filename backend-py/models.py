from sqlalchemy import Column, BigInteger, String, Text, Integer, DateTime, Boolean, Numeric, JSON
from sqlalchemy.sql import func
from database import Base


class Account(Base):
    __tablename__ = "accounts"
    id = Column(BigInteger, primary_key=True, index=True)
    app_id = Column(String(100), unique=True, nullable=False)
    name = Column(String(200), nullable=False)
    avatar_url = Column(String(500))
    description = Column(Text)
    status = Column(Integer, default=1)
    created_at = Column(DateTime, server_default=func.now())
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now())


class Article(Base):
    __tablename__ = "articles"
    id = Column(BigInteger, primary_key=True, index=True)
    account_id = Column(BigInteger, nullable=False, index=True)
    title = Column(String(500), nullable=False)
    content = Column(Text)
    author = Column(String(100))
    url = Column(String(1000))
    read_count = Column(Integer, default=0)
    like_count = Column(Integer, default=0)
    share_count = Column(Integer, default=0)
    comment_count = Column(Integer, default=0)
    publish_time = Column(DateTime)
    created_at = Column(DateTime, server_default=func.now())
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now())


class ArticleAnalysis(Base):
    __tablename__ = "article_analysis"
    id = Column(BigInteger, primary_key=True, index=True)
    article_id = Column(BigInteger, nullable=False, index=True)
    account_id = Column(BigInteger, nullable=False, index=True)
    tags = Column(JSON)
    quality_score = Column(Numeric(3, 1))
    dimension_scores = Column(JSON)
    ai_summary = Column(Text)
    is_title_clickbaity = Column(Boolean)
    title_attraction_score = Column(Integer)
    analysis_version = Column(String(20), default="v1.0")
    created_at = Column(DateTime, server_default=func.now(), index=True)


class AiSuggestion(Base):
    __tablename__ = "ai_suggestions"
    id = Column(BigInteger, primary_key=True, index=True)
    account_id = Column(BigInteger, nullable=False, index=True)
    suggestion_type = Column(String(20), nullable=False, index=True)
    content = Column(Text, nullable=False)
    related_data = Column(JSON)
    is_adopted = Column(Boolean, default=False)
    created_at = Column(DateTime, server_default=func.now())
