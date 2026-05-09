package com.weanalyzer.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_suggestions", indexes = {
    @Index(name = "idx_account_id", columnList = "account_id"),
    @Index(name = "idx_suggestion_type", columnList = "suggestion_type")
})
@Data
public class AiSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggestion_type", nullable = false, length = 20)
    private SuggestionType suggestionType;

    @Column(name = "content", columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "related_data", columnDefinition = "json")
    private String relatedData;

    @Column(name = "is_adopted")
    private Boolean isAdopted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum SuggestionType {
        DAILY_TIP,
        ANOMALY,
        TITLE_OPT,
        TOPIC
    }
}
