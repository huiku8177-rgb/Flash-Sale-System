package com.flashsale.aiservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private boolean enabled;
    private String baseUrl;
    private String apiKey;
    private String embeddingModel;
    private String chatModel;
    private String productServiceUrl;
    private String seckillServiceUrl;
    private int retrievalTopK = 5;
    private int historyLimit = 6;
    private int historyCacheSize = 6;
    private int sessionQueryLimit = 50;
    private int sessionTtlDays = 7;
    private double minConfidence = 0.55d;
    private CleanupProperties cleanup = new CleanupProperties();
    private List<RuleDocumentProperties> ruleDocuments = new ArrayList<>();

    public boolean isChatClientConfigured() {
        return enabled
                && StringUtils.hasText(baseUrl)
                && StringUtils.hasText(apiKey)
                && StringUtils.hasText(chatModel);
    }

    public boolean isEmbeddingClientConfigured() {
        return enabled
                && StringUtils.hasText(baseUrl)
                && StringUtils.hasText(apiKey)
                && StringUtils.hasText(embeddingModel);
    }

    @Data
    public static class RuleDocumentProperties {
        private String title;
        private String sourceType;
        private String content;
    }

    @Data
    public static class CleanupProperties {
        private long initialDelayMs = 60_000L;
        private long fixedDelayMs = 3_600_000L;
    }
}
