package com.weanalyzer.controller;

import com.weanalyzer.dto.AiAnalysisRequest;
import com.weanalyzer.dto.ApiResponse;
import com.weanalyzer.dto.DiagnosisResponse;
import com.weanalyzer.entity.ArticleAnalysis;
import com.weanalyzer.entity.AiSuggestion;
import com.weanalyzer.service.ArticleAnalysisService;
import com.weanalyzer.service.AiSuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final ArticleAnalysisService articleAnalysisService;
    private final AiSuggestionService aiSuggestionService;

    @PostMapping("/articles/ai")
    public ApiResponse<Map<String, Object>> triggerArticleAnalysis(@Valid @RequestBody AiAnalysisRequest request) {
        String taskId = articleAnalysisService.createAnalysisTask(request.getArticleId());
        Map<String, Object> data = new HashMap<>();
        data.put("task_id", taskId);
        data.put("status", "processing");
        return ApiResponse.success(data);
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        String status = articleAnalysisService.getTaskStatus(taskId);
        Map<String, Object> data = new HashMap<>();
        data.put("task_id", taskId);
        data.put("status", status);
        return ApiResponse.success(data);
    }

    @GetMapping("/accounts/{accountId}/diagnosis")
    public ApiResponse<DiagnosisResponse> getAccountDiagnosis(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "7") int days) {
        DiagnosisResponse diagnosis = aiSuggestionService.generateAccountDiagnosis(accountId, days);
        return ApiResponse.success(diagnosis);
    }

    @GetMapping("/articles")
    public ApiResponse<List<ArticleAnalysis>> getArticleAnalyses(
            @RequestParam Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(
            articleAnalysisService.findByAccountIdAndDateRange(accountId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
        );
    }

    @PostMapping("/accounts/{accountId}/topics")
    public ApiResponse<List<String>> recommendTopics(
            @PathVariable Long accountId,
            @RequestBody List<String> hotKeywords) {
        List<String> topics = aiSuggestionService.recommendTopics(accountId, hotKeywords);
        return ApiResponse.success(topics);
    }
}
