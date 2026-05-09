package com.weanalyzer.task;

import com.weanalyzer.entity.Article;
import com.weanalyzer.repository.ArticleRepository;
import com.weanalyzer.service.ArticleAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchAnalysisTask {

    private final ArticleRepository articleRepository;
    private final ArticleAnalysisService articleAnalysisService;

    @Scheduled(cron = "0 30 2 * * ?")
    public void dailyBatchAnalysis() {
        log.info("开始每日批量文章分析任务");
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        LocalDateTime start = yesterday.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = yesterday.withHour(23).withMinute(59).withSecond(59);

        List<Article> articles = articleRepository.findAll().stream()
                .filter(a -> a.getPublishTime() != null)
                .filter(a -> !a.getPublishTime().isBefore(start) && !a.getPublishTime().isAfter(end))
                .toList();

        log.info("待分析文章数量: {}", articles.size());
        for (Article article : articles) {
            try {
                articleAnalysisService.asyncAnalyzeArticle(article.getId());
                Thread.sleep(200);
            } catch (Exception e) {
                log.error("文章分析失败: articleId={}", article.getId(), e);
            }
        }
    }

    @KafkaListener(topics = "article-analysis", groupId = "weanalyzer-group")
    public void handleAnalysisMessage(String articleId) {
        try {
            Long id = Long.parseLong(articleId);
            articleAnalysisService.analyzeArticleInternal(id);
        } catch (Exception e) {
            log.error("消费分析消息失败: {}", articleId, e);
        }
    }
}
