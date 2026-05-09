package com.weanalyzer.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.service.v4.model.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ZhipuAiClient {

    private final ClientV4 client;
    private final String model;
    private final int maxTokensPerDay;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    private static final String DAILY_TOKEN_KEY = "zhipu:daily_tokens:";
    private static final String CACHE_KEY_PREFIX = "zhipu:cache:";

    public ZhipuAiClient(
            @Value("${zhipu.api-key}") String apiKey,
            @Value("${zhipu.model}") String model,
            @Value("${zhipu.max-tokens-per-day}") int maxTokensPerDay,
            StringRedisTemplate redisTemplate,
            MeterRegistry meterRegistry) {
        this.client = new ClientV4.Builder(apiKey).build();
        this.model = model;
        this.maxTokensPerDay = maxTokensPerDay;
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    public String chat(String prompt, String systemMessage) {
        return chat(prompt, systemMessage, 0.7, 1024);
    }

    public String chat(String prompt, String systemMessage, double temperature, int maxTokens) {
        String cacheKey = CACHE_KEY_PREFIX + md5(prompt + systemMessage + temperature + maxTokens);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("Zhipu AI cache hit");
            return cached;
        }

        if (!checkDailyQuota()) {
            log.warn("Zhipu AI daily token quota exceeded, fallback to local mode");
            throw new RuntimeException("AI额度已耗尽，已降级至本地算法模式");
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMessage));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .stream(Boolean.FALSE)
                    .messages(messages)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .build();

            ModelApiResponse response = client.invokeModelApi(request);
            sample.stop(meterRegistry.timer("zhipu.api.call", "status", "success"));

            if (response.isSuccess()) {
                String content = response.getData().getChoices().get(0).getMessage().getContent().toString();
                updateDailyTokenUsage(estimateTokens(prompt + content));
                redisTemplate.opsForValue().set(cacheKey, content, 1, TimeUnit.HOURS);
                return content;
            } else {
                log.error("Zhipu API error: {}", response.getMsg());
                throw new RuntimeException("AI调用失败: " + response.getMsg());
            }
        } catch (Exception e) {
            sample.stop(meterRegistry.timer("zhipu.api.call", "status", "error"));
            log.error("Zhipu AI call failed", e);
            throw e;
        }
    }

    public JSONObject chatJson(String prompt, String systemMessage) {
        String content = chat(prompt, systemMessage, 0.3, 1024);
        return JSON.parseObject(content);
    }

    private boolean checkDailyQuota() {
        String key = DAILY_TOKEN_KEY + java.time.LocalDate.now();
        String used = redisTemplate.opsForValue().get(key);
        if (used == null) {
            return true;
        }
        return Integer.parseInt(used) < maxTokensPerDay;
    }

    private void updateDailyTokenUsage(int tokens) {
        String key = DAILY_TOKEN_KEY + java.time.LocalDate.now();
        redisTemplate.opsForValue().increment(key, tokens);
        redisTemplate.expire(key, 25, TimeUnit.HOURS);
    }

    private int estimateTokens(String text) {
        return text.length() / 2;
    }

    private String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
