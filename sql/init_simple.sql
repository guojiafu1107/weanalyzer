-- PostgreSQL init for weanalyzer (simplified)
-- Run this in pgAdmin Query Tool on database 'weanalyzer'

CREATE TABLE IF NOT EXISTS accounts (
    id BIGSERIAL PRIMARY KEY,
    app_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    avatar_url VARCHAR(500),
    description TEXT,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS articles (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT,
    author VARCHAR(100),
    url VARCHAR(1000),
    read_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    share_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    publish_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_articles_account_id ON articles(account_id);
CREATE INDEX idx_articles_publish_time ON articles(publish_time);

CREATE TABLE IF NOT EXISTS article_analysis (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    tags JSON,
    quality_score NUMERIC(3,1),
    dimension_scores JSON,
    ai_summary TEXT,
    is_title_clickbaity BOOLEAN,
    title_attraction_score INT,
    analysis_version VARCHAR(20) DEFAULT 'v1.0',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_analysis_article_id ON article_analysis(article_id);
CREATE INDEX idx_analysis_account_id ON article_analysis(account_id);
CREATE INDEX idx_analysis_created_at ON article_analysis(created_at);

CREATE TABLE IF NOT EXISTS ai_suggestions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    suggestion_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    related_data JSON,
    is_adopted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_suggestions_account_id ON ai_suggestions(account_id);
CREATE INDEX idx_suggestions_type ON ai_suggestions(suggestion_type);

INSERT INTO accounts (app_id, name, description) VALUES
('wx_test_001', '产品沉思录', '专注产品经理成长与AI工具测评'),
('wx_test_002', '科技瞭望台', '前沿科技资讯与深度解读');

INSERT INTO articles (account_id, title, content, author, read_count, like_count, share_count, publish_time) VALUES
(1, '2024年AI工具盘点：这10款让我效率翻倍', '在人工智能快速发展的2024年，各类AI工具层出不穷...', '张产品', 12500, 320, 180, '2026-05-07 10:00:00'),
(1, '产品经理必懂的5个数据分析模型', '数据驱动决策已成为产品经理的核心能力...', '李数据', 8900, 210, 95, '2026-05-06 14:00:00'),
(2, '微信新功能深度体验报告', '微信近期上线了一系列新功能，我们进行了为期一周的深度体验...', '王体验', 15200, 450, 320, '2026-05-05 20:00:00');
