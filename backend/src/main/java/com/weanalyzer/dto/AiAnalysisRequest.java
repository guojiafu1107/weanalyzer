package com.weanalyzer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiAnalysisRequest {

    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    private Boolean forceUpdate = false;
}
