package com.weanalyzer.repository;

import com.weanalyzer.entity.ArticleAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleAnalysisRepository extends JpaRepository<ArticleAnalysis, Long> {

    Optional<ArticleAnalysis> findByArticleId(Long articleId);

    List<ArticleAnalysis> findByAccountId(Long accountId);

    @Query("SELECT a FROM ArticleAnalysis a WHERE a.accountId = :accountId AND a.createdAt BETWEEN :start AND :end")
    List<ArticleAnalysis> findByAccountIdAndDateRange(
        @Param("accountId") Long accountId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
}
