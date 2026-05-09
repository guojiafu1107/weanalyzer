from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime


class ApiResponse(BaseModel):
    code: int = 0
    message: str = "success"
    data: Optional[dict] = None
    timestamp: int = 0

    class Config:
        from_attributes = True


class AiAnalysisRequest(BaseModel):
    article_id: int
    force_update: bool = False


class DiagnosisResponse(BaseModel):
    summary: str
    suggestions: List[str]
    trend: str
    risk_points: List[str]


class ArticleAnalysisOut(BaseModel):
    id: int
    article_id: int
    account_id: int
    tags: Optional[list]
    quality_score: Optional[float]
    dimension_scores: Optional[dict]
    ai_summary: Optional[str]
    is_title_clickbaity: Optional[bool]
    title_attraction_score: Optional[int]
    created_at: datetime

    class Config:
        from_attributes = True
