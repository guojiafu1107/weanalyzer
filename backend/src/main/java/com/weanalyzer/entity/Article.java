package com.weanalyzer.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "articles", indexes = {
    @Index(name = "idx_account_id", columnList = "account_id"),
    @Index(name = "idx_publish_time", columnList = "publish_time")
})
@Data
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "author", length = 100)
    private String author;

    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "read_count")
    private Integer readCount = 0;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "share_count")
    private Integer shareCount = 0;

    @Column(name = "comment_count")
    private Integer commentCount = 0;

    @Column(name = "publish_time")
    private LocalDateTime publishTime;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
