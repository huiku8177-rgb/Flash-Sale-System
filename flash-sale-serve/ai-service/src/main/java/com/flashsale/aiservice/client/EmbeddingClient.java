package com.flashsale.aiservice.client;

import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.exception.ModelInvokeException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class EmbeddingClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final WebClient aiWebClient;
    private final AiProperties aiProperties;

    public EmbeddingClient(@Qualifier("aiWebClient") WebClient aiWebClient, AiProperties aiProperties) {
        this.aiWebClient = aiWebClient;
        this.aiProperties = aiProperties;
    }

    public List<Double> embed(String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("text must not be blank");
        }
        if (!StringUtils.hasText(aiProperties.getEmbeddingModel())) {
            throw new ModelInvokeException("embedding model is not configured");
        }

        log.debug("Requesting embedding, model={}, textLength={}", aiProperties.getEmbeddingModel(), text.length());

        EmbeddingResponse response;
        try {
            response = aiWebClient.post()
                    .uri("/v1/embeddings")
                    .bodyValue(new EmbeddingRequest(aiProperties.getEmbeddingModel(), text))
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .block(TIMEOUT);
        } catch (WebClientResponseException e) {
            log.error("Embedding API error, status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ModelInvokeException(
                    "Embedding API HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new ModelInvokeException("Failed to call embedding API: " + e.getMessage(), e);
        }

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            throw new ModelInvokeException("Embedding API returned empty response");
        }

        List<Double> embedding = response.getData().get(0).getEmbedding();
        if (embedding == null || embedding.isEmpty()) {
            throw new ModelInvokeException("Embedding vector is empty");
        }

        log.debug("Embedding succeeded, dimensions={}", embedding.size());
        return embedding;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class EmbeddingRequest {
        private String model;
        private String input;
    }

    @Data
    @NoArgsConstructor
    private static class EmbeddingResponse {
        private List<EmbeddingData> data;
    }

    @Data
    @NoArgsConstructor
    private static class EmbeddingData {
        private List<Double> embedding;
    }
}
