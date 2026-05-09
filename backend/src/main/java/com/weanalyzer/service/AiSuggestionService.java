package com.weanalyzer.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.weanalyzer.client.ZhipuAiClient;
import com.weanalyzer.dto.DiagnosisResponse;
import com.weanalyzer.entity.AiSuggestion;
import com.weanalyzer.entity.Article;
import com.weanalyzer.entity.ArticleAnalysis;
import com.weanalyzer.repository.AiSuggestionRepository;
import com.weanalyzer.repository.ArticleAnalysisRepository;
import com.weanalyzer.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSuggestionService {

    private final ZhipuAiClient zhipuAiClient;
    private final AiSuggestionRepository aiSuggestionRepository;
    private final ArticleRepository articleRepository;
    private final ArticleAnalysisRepository articleAnalysisRepository;

    @Transactional
    public DiagnosisResponse generateAccountDiagnosis(Long accountId, int days) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days);

        List<Article> articles = articleRepository.findByAccountIdAndPublishTimeBetween(accountId, start, end);
        List<ArticleAnalysis> analyses = articleAnalysisRepository.findByAccountIdAndDateRange(accountId, start, end);

        int totalRead = articles.stream().mapToInt(Article::getReadCount).sum();
        int totalShare = articles.stream().mapToInt(Article::getShareCount).sum();
        int totalLike = articles.stream().mapToInt(Article::getLikeCount).sum();
        double avgQuality = analyses.stream()
                .filter(a -> a.getQualityScore() != null)
                .mapToDouble(ArticleAnalysis::getQualityScore)
                .average().orElse(0.0);

        String dataDesc = buildDataDescription(articles, totalRead, totalShare, totalLike, avgQuality, days);

        String system = "你是顶尖的公众号运营顾问。根据数据描述，指出账号当前最需要解决的问题，并给出具体可执行的改进建议。语气客观、直接，避免空话。返回JSON格式：{\"summary\":\"...\",\"suggestions\":[\"...\"],\"trend\":\"上升/下降/平稳\",\"riskPoints\":[\"...\"]}";

        DiagnosisResponse response = new DiagnosisResponse();
        try {
            String result = zhipuAiClient.chat(dataDesc, system, 0.5, 1024);
            result = result.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            JSONObject json = JSON.parseObject(result);
            response.setSummary(json.getString("summary"));
            response.setSuggestions(json.getJSONArray("suggestions").toList(String.class));
            response.setTrend(json.getString("trend"));
            response.setRiskPoints(json.getJSONArray("riskPoints").toList(String.class));
        } catch (Exception e) {
            log.error("AI诊断失败", e);
            response.setSummary("近" + days + "天共发布" + articles.size() + "篇文章，总阅读量" + totalRead + "。AI服务暂时不可用，请稍后重试。");
            response.setSuggestions(Arrays.asList("保持内容更新频率", "关注用户互动数据", "优化标题吸引力"));
            response.setTrend("平稳");
            response.setRiskPoints(Arrays.asList("AI额度可能不足"));
        }

        AiSuggestion suggestion = new AiSuggestion();
        suggestion.setAccountId(accountId);
        suggestion.setSuggestionType(AiSuggestion.SuggestionType.ANOMALY);
        suggestion.setContent(response.getSummary());
        suggestion.setRelatedData(JSON.toJSONString(response));
        aiSuggestionRepository.save(suggestion);

        return response;
    }

    @Transactional
    public List<String> recommendTopics(Long accountId, List<String> hotKeywords) {
        List<ArticleAnalysis> recentAnalyses = articleAnalysisRepository.findByAccountId(accountId);
        Set<String> tagSet = new HashSet<>();
        for (ArticleAnalysis a : recentAnalyses) {
            if (a.getTags() != null) {
                try {
                    JSONArray arr = JSON.parseArray(a.getTags());
                    for (int i = 0; i < arr.size(); i++) {
                        tagSet.add(arr.getJSONObject(i).getString("tag"));
                    }
                } catch (Exception ignored) {}
            }
        }

        String accountTags = String.join("、", tagSet.stream().limit(5).collect(Collectors.toList()));
        String hotWords = String.join("、", hotKeywords);

        String system = "根据提供的今日热点关键词和本账号长期内容风格，生成3个不同的切入角度，贴合账号调性，吸引目标读者。返回JSON数组：[{\"topic\":\"选题\",\"reason\":\"理由\"}]";
        String prompt = "账号标签：" + accountTags + "\n今日热点关键词：" + hotWords + "\n请给出3个选题建议。";

        try {
            String result = zhipuAiClient.chat(prompt, system, 0.7, 1024);
            result = result.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            JSONArray array = JSON.parseArray(result);
            List<String> topics = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                JSONObject obj = array.getJSONObject(i);
                topics.add((i + 1) + ". " + obj.getString("topic") + " —— " + obj.getString("reason"));
            }

            AiSuggestion suggestion = new AiSuggestion();
            suggestion.setAccountId(accountId);
            suggestion.setSuggestionType(AiSuggestion.SuggestionType.TOPIC);
            suggestion.setContent(String.join("\n", topics));
            suggestion.setRelatedData(JSON.toJSONString(hotKeywords));
            aiSuggestionRepository.save(suggestion);

            return topics;
        } catch (Exception e) {
            log.error("选题推荐失败", e);
            return Arrays.asList("1. 结合热点与专业视角的深度分析", "2. 用户痛点问题的实操解决方案", "3. 行业趋势的前瞻性解读");
        }
    }

    private String buildDataDescription(List<Article> articles, int totalRead, int totalShare, int totalLike, double avgQuality, int days) {
        StringBuilder sb = new StringBuilder();
        sb.append("过去").append(days).append("天数据：\n");
        sb.append("- 发布文章数：").append(articles.size()).append("篇\n");
        sb.append("- 总阅读量：").append(totalRead).append("\n");
        sb.append("- 总分享数：").append(totalShare).append("\n");
        sb.append("- 总点赞数：").append(totalLike).append("\n");
        sb.append("- 平均质量分：").append(String.format("%.1f", avgQuality)).append("\n");
        if (!articles.isEmpty()) {
            sb.append("- 最近文章标题：\n");
            articles.stream().limit(5).forEach(a -> sb.append("  ").append(a.getTitle()).append("\n"));
        }
        return sb.toString();
    }
}
