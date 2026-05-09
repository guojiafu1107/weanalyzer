import os
import uuid
from datetime import datetime, timedelta
from typing import List, Optional

from fastapi import FastAPI, Depends, HTTPException, Body
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from dotenv import load_dotenv
from pydantic import BaseModel

from database import engine, get_db
from models import Base, Article, ArticleAnalysis
from schemas import ApiResponse, AiAnalysisRequest, DiagnosisResponse, ArticleAnalysisOut
from services import analyze_article, generate_diagnosis, recommend_topics

os.chdir(os.path.dirname(os.path.abspath(__file__)))
load_dotenv(encoding='utf-8')

# Create tables
Base.metadata.create_all(bind=engine)

app = FastAPI(title="WeAnalyzer API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=[os.getenv("CORS_ORIGIN", "http://localhost:3000")],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def success_response(data=None):
    return {
        "code": 0,
        "message": "success",
        "data": data,
        "timestamp": int(datetime.now().timestamp() * 1000)
    }


class SubmitArticleRequest(BaseModel):
    account_id: int = 1
    title: str
    content: str
    author: str = ""


@app.post("/api/v1/analysis/articles/submit")
def submit_and_analyze_article(req: SubmitArticleRequest, db: Session = Depends(get_db)):
    """提交文章内容并触发AI分析，直接返回分析结果"""
    try:
        article = Article(
            account_id=req.account_id,
            title=req.title,
            content=req.content,
            author=req.author or "未知",
            read_count=0,
            like_count=0,
            share_count=0,
            publish_time=datetime.now()
        )
        db.add(article)
        db.commit()
        db.refresh(article)

        analysis = analyze_article(db, article.id)
        analysis_data = ArticleAnalysisOut.model_validate(analysis).model_dump()
        analysis_data["title"] = article.title

        return success_response(analysis_data)
    except Exception as e:
        import traceback
        error_detail = traceback.format_exc()
        print(f"=== SUBMIT ERROR ===\n{error_detail}")
        from fastapi.responses import JSONResponse
        return JSONResponse(
            status_code=500,
            content={"code": 500, "message": str(e), "detail": error_detail}
        )


@app.post("/api/v1/analysis/articles/ai")
def trigger_article_analysis(req: AiAnalysisRequest, db: Session = Depends(get_db)):
    article = db.query(Article).filter(Article.id == req.article_id).first()
    if not article:
        raise HTTPException(status_code=404, detail="Article not found")

    task_id = str(uuid.uuid4())
    analyze_article(db, req.article_id)
    return success_response({"task_id": task_id, "status": "completed"})


@app.get("/api/v1/analysis/tasks/{task_id}")
def get_task_status(task_id: str):
    return success_response({"task_id": task_id, "status": "completed"})


@app.get("/api/v1/analysis/accounts/{account_id}/diagnosis")
def get_account_diagnosis(account_id: int, days: int = 7, db: Session = Depends(get_db)):
    result = generate_diagnosis(db, account_id, days)
    return success_response(result)


@app.get("/api/v1/analysis/articles")
def get_article_analyses(
    account_id: int,
    start_date: Optional[str] = None,
    end_date: Optional[str] = None,
    page: int = 0,
    size: int = 20,
    db: Session = Depends(get_db)
):
    query = db.query(ArticleAnalysis).filter(ArticleAnalysis.account_id == account_id)
    if start_date:
        query = query.filter(ArticleAnalysis.created_at >= datetime.strptime(start_date, "%Y-%m-%d"))
    if end_date:
        query = query.filter(ArticleAnalysis.created_at <= datetime.strptime(end_date, "%Y-%m-%d") + timedelta(days=1))
    total = query.count()
    raw = query.offset(page * size).limit(size).all()
    items = []
    for a in raw:
        d = ArticleAnalysisOut.model_validate(a).model_dump()
        article = db.query(Article).filter(Article.id == a.article_id).first()
        if article:
            d["title"] = article.title
            d["read_count"] = article.read_count
            d["like_count"] = article.like_count
            d["share_count"] = article.share_count
        items.append(d)
    return success_response({"total": total, "items": items})


@app.get("/api/v1/dashboard")
def get_dashboard(account_id: int = 1, db: Session = Depends(get_db)):
    """数据概览"""
    articles = db.query(Article).filter(Article.account_id == account_id).all()
    analyses = db.query(ArticleAnalysis).filter(ArticleAnalysis.account_id == account_id).all()

    total_read = sum(a.read_count or 0 for a in articles)
    total_like = sum(a.like_count or 0 for a in articles)
    total_share = sum(a.share_count or 0 for a in articles)

    # Tag distribution
    tag_count = {}
    for a in analyses:
        if a.tags:
            for tag in (a.tags if isinstance(a.tags, list) else []):
                t = tag.get("tag", "") if isinstance(tag, dict) else str(tag)
                if t:
                    tag_count[t] = tag_count.get(t, 0) + 1

    # Recent articles with analysis
    recent = []
    for a in sorted(articles, key=lambda x: x.publish_time or datetime.min, reverse=True)[:10]:
        analysis = next((x for x in analyses if x.article_id == a.id), None)
        recent.append({
            "id": a.id,
            "title": a.title,
            "read_count": a.read_count,
            "like_count": a.like_count,
            "share_count": a.share_count,
            "quality_score": float(analysis.quality_score) if analysis and analysis.quality_score else None,
            "tags": analysis.tags if analysis and analysis.tags else [],
            "is_title_clickbaity": analysis.is_title_clickbaity if analysis else False,
            "title_attraction_score": analysis.title_attraction_score if analysis else None,
        })

    return success_response({
        "stats": {
            "total_read": total_read,
            "total_like": total_like,
            "total_share": total_share,
            "article_count": len(articles)
        },
        "tag_distribution": [{"name": k, "value": v} for k, v in sorted(tag_count.items(), key=lambda x: -x[1])],
        "recent_articles": recent
    })


@app.post("/api/v1/analysis/accounts/{account_id}/topics")
def get_topic_recommendations(account_id: int, hot_keywords: List[str], db: Session = Depends(get_db)):
    topics = recommend_topics(db, account_id, hot_keywords)
    return success_response(topics)


@app.get("/api/v1/articles")
def list_articles(account_id: Optional[int] = None, db: Session = Depends(get_db)):
    query = db.query(Article)
    if account_id:
        query = query.filter(Article.account_id == account_id)
    return success_response([{"id": a.id, "title": a.title, "read_count": a.read_count} for a in query.all()])


@app.get("/health")
def health_check():
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", "8080"))
    uvicorn.run(app, host="0.0.0.0", port=port)
