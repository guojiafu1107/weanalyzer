package com.weanalyzer.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.weanalyzer.client.ZhipuAiClient;
import com.weanalyzer.entity.Article;
import com.weanalyzer.entity.ArticleAnalysis;
import com.weanalyzer.repository.ArticleAnalysisRepository;
import com.weanalyzer.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleAnalysisService {

    private final ArticleAnalysisRepository articleAnalysisRepository;
    private final ArticleRepository articleRepository;
    private final ZhipuAiClient zhipuAiClient;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TASK_PREFIX = "analysis:task:";

    @Async
    public void asyncAnalyzeArticle(Long articleId) {
        analyzeArticleInternal(articleId);
    }

    @Transactional
    public ArticleAnalysis analyzeArticleInternal(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在: " + articleId));

        Optional<ArticleAnalysis> existing = articleAnalysisRepository.findByArticleId(articleId);
        if (existing.isPresent()) {
            return existing.get();
        }

        String contentPreview = extractContentPreview(article.getContent(), 1500);

        ArticleAnalysis analysis = new ArticleAnalysis();
        analysis.setArticleId(articleId);
        analysis.setAccountId(article.getAccountId());

        try {
            JSONObject tagsResult = extractTags(article.getTitle(), contentPreview);
            analysis.setTags(tagsResult.toJSONString());
        } catch (Exception e) {
            log.warn("标签提取失败，使用本地备用方案", e);
            analysis.setTags("[]");
        }

        try {
            JSONObject qualityResult = evaluateQuality(article.getTitle(), contentPreview);
            analysis.setQualityScore(qualityResult.getDouble("overall"));
            analysis.setDimensionScores(qualityResult.toJSONString());
        } catch (Exception e) {
            log.warn("质量评分失败", e);
            analysis.setQualityScore(5.0);
            analysis.setDimensionScores("{}");
        }

        try {
            String summary = generateSummary(article.getTitle(), contentPreview);
            analysis.setAiSummary(summary);
        } catch (Exception e) {
            log.warn("摘要生成失败", e);
            analysis.setAiSummary("");
        }

        try {
            int attractionScore = analyzeTitleAttraction(article.getTitle());
            analysis.setTitleAttractionScore(attractionScore);
        } catch (Exception e) {
            analysis.setTitleAttractionScore(50);
        }

        articleAnalysisRepository.save(analysis);
        log.info("文章分析完成: articleId={}", articleId);
        return analysis;
    }

    public String createAnalysisTask(Long articleId) {
        String taskId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(TASK_PREFIX + taskId, "PROCESSING", 10, TimeUnit.MINUTES);
        kafkaTemplate.send("article-analysis", String.valueOf(articleId));
        return taskId;
    }

    public String getTaskStatus(String taskId) {
        String status = redisTemplate.opsForValue().get(TASK_PREFIX + taskId);
        return status != null ? status : "NOT_FOUND";
    }

    public List<ArticleAnalysis> findByAccountIdAndDateRange(Long accountId, LocalDateTime start, LocalDateTime end) {
        return articleAnalysisRepository.findByAccountIdAndDateRange(accountId, start, end);
    }

    private JSONObject extractTags(String title, String content) {
        String system = "你是微信公众号内容分析专家。根据文章标题和正文，提取最核心的3-5个内容标签，每个标签附带0-1之间的相关度权重。只返回JSON，格式：[{\"tag\":\"标签名\",\"weight\":0.95},...]。";
        String prompt = "标题：" + title + "\n正文：" + content;
        String result = zhipuAiClient.chat(prompt, system, 0.3, 512);

        // Clean markdown code block
        result = result.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        if (result.startsWith("[")) {
            JSONArray array = JSON.parseArray(result);
            return new JSONObject().fluentPut("tags", array);
        }
        return JSON.parseObject(result);
    }

    private JSONObject evaluateQuality(String title, String content) {
        String system = "你是内容质量评审专家，请对以下公众号文章打分。只返回JSON，格式：{\"info_density\":8,\"originality\":7,\"logic\":9,\"attraction\":6,\"practical_value\":8,\"overall\":7.6}。";
        String prompt = "标题：" + title + "\n正文：" + content;
        String result = zhipuAiClient.chat(prompt, system, 0.3, 512);
        result = result.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        return JSON.parseObject(result);
    }

    private String generateSummary(String title, String content) {
        String system = "你是文章摘要专家。请为以下公众号文章生成一段150字以内的精炼摘要，突出核心观点。只返回摘要文本。";
        String prompt = "标题：" + title + "\n正文：" + content;
        return zhipuAiClient.chat(prompt, system, 0.5, 256);
    }

    private int analyzeTitleAttraction(String title) {
        String system = "你是标题优化专家。请对以下公众号标题的吸引力进行评分（0-100），只返回一个整数数字。";
        String prompt = "标题：" + title;
        String result = zhipuAiClient.chat(prompt, system, 0.3, 64).trim();
        try {
            return Integer.parseInt(result.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 50;
        }
    }

    private String extractContentPreview(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        String plain = content.replaceAll("<[^>]+>", "").replaceAll("\\s+", "");
        return plain.length() > maxLength ? plain.substring(0, maxLength) : plain;
    }
}
