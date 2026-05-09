import json
import re
from typing import List, Optional
from sqlalchemy.orm import Session
from models import Article, ArticleAnalysis, AiSuggestion
from zhipu_client import zhipu_client


def extract_content_preview(content: Optional[str], max_length: int = 1500) -> str:
    if not content:
        return ""
    plain = re.sub(r"<[^>]+>", "", content)
    plain = re.sub(r"\s+", "", plain)
    return plain[:max_length]


def analyze_article(db: Session, article_id: int) -> ArticleAnalysis:
    article = db.query(Article).filter(Article.id == article_id).first()
    if not article:
        raise ValueError(f"Article not found: {article_id}")

    existing = db.query(ArticleAnalysis).filter(ArticleAnalysis.article_id == article_id).first()
    if existing:
        return existing

    preview = extract_content_preview(article.content)
    analysis = ArticleAnalysis(article_id=article_id, account_id=article.account_id)

    # Tags
    try:
        system = '你是微信公众号内容分析专家。根据文章标题和正文，提取最核心的3-5个内容标签，每个标签附带0-1之间的相关度权重。只返回JSON，格式：[{"tag":"标签名","weight":0.95},...]。'
        result = zhipu_client.chat_json(f"标题：{article.title}\n正文：{preview}", system, 0.3)
        analysis.tags = result if isinstance(result, list) else result.get("tags", [])
    except Exception as e:
        print(f"Tag extraction failed: {e}")
        analysis.tags = []

    # Quality
    try:
        system = '你是内容质量评审专家，请对以下公众号文章打分。只返回JSON，格式：{"info_density":8,"originality":7,"logic":9,"attraction":6,"practical_value":8,"overall":7.6}。'
        result = zhipu_client.chat_json(f"标题：{article.title}\n正文：{preview}", system, 0.3)
        analysis.quality_score = result.get("overall", 5.0)
        analysis.dimension_scores = result
    except Exception as e:
        print(f"Quality evaluation failed: {e}")
        analysis.quality_score = 5.0
        analysis.dimension_scores = {}

    # Summary
    try:
        system = "你是文章摘要专家。请为以下公众号文章生成一段150字以内的精炼摘要，突出核心观点。只返回摘要文本。"
        analysis.ai_summary = zhipu_client.chat(f"标题：{article.title}\n正文：{preview}", system, 0.5, 256)
    except Exception as e:
        print(f"Summary failed: {e}")
        analysis.ai_summary = ""

    # Title attraction
    try:
        system = "你是标题优化专家。请对以下公众号标题的吸引力进行评分（0-100），只返回一个整数数字。"
        result = zhipu_client.chat(f"标题：{article.title}", system, 0.3, 64).strip()
        nums = re.findall(r"\d+", result)
        analysis.title_attraction_score = int(nums[0]) if nums else 50
    except Exception as e:
        analysis.title_attraction_score = 50

    db.add(analysis)
    db.commit()
    db.refresh(analysis)
    return analysis


def generate_diagnosis(db: Session, account_id: int, days: int = 7) -> dict:
    from datetime import datetime, timedelta
    end = datetime.now()
    start = end - timedelta(days=days)

    articles = db.query(Article).filter(
        Article.account_id == account_id,
        Article.publish_time >= start,
        Article.publish_time <= end
    ).all()

    analyses = db.query(ArticleAnalysis).filter(
        ArticleAnalysis.account_id == account_id,
        ArticleAnalysis.created_at >= start,
        ArticleAnalysis.created_at <= end
    ).all()

    total_read = sum(a.read_count or 0 for a in articles)
    total_share = sum(a.share_count or 0 for a in articles)
    total_like = sum(a.like_count or 0 for a in articles)
    avg_quality = sum(a.quality_score or 0 for a in analyses) / len(analyses) if analyses else 0

    data_desc = f"""过去{days}天数据：
- 发布文章数：{len(articles)}篇
- 总阅读量：{total_read}
- 总分享数：{total_share}
- 总点赞数：{total_like}
- 平均质量分：{avg_quality:.1f}
"""
    if articles:
        data_desc += "- 最近文章标题：\n"
        for a in articles[:5]:
            data_desc += f"  {a.title}\n"

    system = '你是顶尖的公众号运营顾问。根据数据描述，指出账号当前最需要解决的问题，并给出具体可执行的改进建议。语气客观、直接，避免空话。返回JSON格式：{"summary":"...","suggestions":["..."],"trend":"上升/下降/平稳","riskPoints":["..."]}'

    try:
        result = zhipu_client.chat_json(data_desc, system, 0.5)
    except Exception as e:
        print(f"Diagnosis failed: {e}")
        result = {
            "summary": f"近{days}天共发布{len(articles)}篇文章，总阅读量{total_read}。AI服务暂时不可用。",
            "suggestions": ["保持内容更新频率", "关注用户互动数据", "优化标题吸引力"],
            "trend": "平稳",
            "riskPoints": ["AI额度可能不足"]
        }

    suggestion = AiSuggestion(
        account_id=account_id,
        suggestion_type="ANOMALY",
        content=result["summary"],
        related_data=result
    )
    db.add(suggestion)
    db.commit()

    return result


def recommend_topics(db: Session, account_id: int, hot_keywords: List[str]) -> List[str]:
    analyses = db.query(ArticleAnalysis).filter(ArticleAnalysis.account_id == account_id).all()
    tag_set = set()
    for a in analyses:
        if a.tags:
            for tag in (a.tags if isinstance(a.tags, list) else []):
                if isinstance(tag, dict):
                    tag_set.add(tag.get("tag", ""))
                elif isinstance(tag, str):
                    tag_set.add(tag)

    account_tags = "、".join(list(tag_set)[:5])
    hot_words = "、".join(hot_keywords)

    system = '根据提供的今日热点关键词和本账号长期内容风格，生成3个不同的切入角度，贴合账号调性，吸引目标读者。返回JSON数组：[{"topic":"选题","reason":"理由"}]'
    prompt = f"账号标签：{account_tags}\n今日热点关键词：{hot_words}\n请给出3个选题建议。"

    try:
        result = zhipu_client.chat_json(prompt, system, 0.7)
        topics = []
        for i, item in enumerate(result):
            topics.append(f"{i+1}. {item.get('topic', '')} —— {item.get('reason', '')}")

        suggestion = AiSuggestion(
            account_id=account_id,
            suggestion_type="TOPIC",
            content="\n".join(topics),
            related_data={"keywords": hot_keywords}
        )
        db.add(suggestion)
        db.commit()
        return topics
    except Exception as e:
        print(f"Topic recommendation failed: {e}")
        return [
            "1. 结合热点与专业视角的深度分析",
            "2. 用户痛点问题的实操解决方案",
            "3. 行业趋势的前瞻性解读"
        ]
