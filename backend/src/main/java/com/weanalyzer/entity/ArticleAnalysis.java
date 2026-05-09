package com.weanalyzer.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "article_analysis", indexes = {
    @Index(name = "idx_article_id", columnList = "article_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
public class ArticleAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "tags", columnDefinition = "json")
    private String tags;

    @Column(name = "quality_score", precision = 3, scale = 1)
    private Double qualityScore;

    @Column(name = "dimension_scores", columnDefinition = "json")
    private String dimensionScores;

    @Column(name = "ai_summary", columnDefinition = "text")
    private String aiSummary;

    @Column(name = "is_title_clickbaity")
    private Boolean isTitleClickbaity;

    @Column(name = "title_attraction_score")
    private Integer titleAttractionScore;

    @Column(name = "analysis_version", length = 20)
    private String analysisVersion = "v1.0";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
