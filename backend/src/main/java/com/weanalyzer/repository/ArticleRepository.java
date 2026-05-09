package com.weanalyzer.repository;

import com.weanalyzer.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    List<Article> findByAccountIdOrderByPublishTimeDesc(Long accountId);

    List<Article> findByAccountIdAndPublishTimeBetween(Long accountId, LocalDateTime start, LocalDateTime end);
}
