package com.flashsale.aiservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiConfigurationDiagnostics implements ApplicationRunner {

    private final AiProperties aiProperties;

    @Override
    public void run(ApplicationArguments args) {
        log.info(
                "AI configuration summary: enabled={}, baseUrl={}, chatModel={}, embeddingModel={}, apiKeyPresent={}, productServiceUrl={}, seckillServiceUrl={}",
                aiProperties.isEnabled(),
                safeValue(aiProperties.getBaseUrl()),
                safeValue(aiProperties.getChatModel()),
                safeValue(aiProperties.getEmbeddingModel()),
                aiProperties.hasApiKey(),
                safeValue(aiProperties.getProductServiceUrl()),
                safeValue(aiProperties.getSeckillServiceUrl())
        );

        if (!aiProperties.isChatClientConfigured()) {
            log.warn("Chat model client is not fully configured, missing={}", aiProperties.missingChatConfigSummary());
        }
        if (!aiProperties.isEmbeddingClientConfigured()) {
            log.warn("Embedding client is not fully configured, missing={}, local fallback depends on the active runtime profile",
                    aiProperties.missingEmbeddingConfigSummary());
        }

        warnIfServiceAlias(aiProperties.getProductServiceUrl(), "product-service-url");
        warnIfServiceAlias(aiProperties.getSeckillServiceUrl(), "seckill-service-url");
    }

    private void warnIfServiceAlias(String url, String propertyName) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                return;
            }
            boolean looksLikeServiceAlias = !host.contains(".") && !"localhost".equalsIgnoreCase(host) && !host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
            if (looksLikeServiceAlias) {
                log.warn("{} is set to {}, but current client uses plain WebClient URL calls. This only works if the host can be resolved directly in the current environment.",
                        propertyName, url);
            }
        } catch (Exception ex) {
            log.warn("Failed to inspect {}={}, reason={}", propertyName, url, ex.getMessage());
        }
    }

    private String safeValue(String value) {
        return StringUtils.hasText(value) ? value : "<empty>";
    }
}
