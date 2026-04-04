package com.flashsale.aiservice.client;

import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.domain.dto.SeckillKnowledgeDTO;
import com.flashsale.common.domain.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillKnowledgeClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient.Builder webClientBuilder;
    private final AiProperties aiProperties;

    public List<SeckillKnowledgeDTO> getAllProducts() {
        try {
            Result<List<SeckillKnowledgeDTO>> result = webClientBuilder.build()
                    .get()
                    .uri(aiProperties.getSeckillServiceUrl() + "/seckill-product/products")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<List<SeckillKnowledgeDTO>>>() {})
                    .block(TIMEOUT);
            return result == null || result.getData() == null ? List.of() : result.getData();
        } catch (WebClientResponseException e) {
            log.error("Failed to fetch seckill products, status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            log.error("Failed to fetch seckill products: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public SeckillKnowledgeDTO getProductById(Long productId) {
        try {
            Result<SeckillKnowledgeDTO> result = webClientBuilder.build()
                    .get()
                    .uri(aiProperties.getSeckillServiceUrl() + "/seckill-product/products/" + productId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<SeckillKnowledgeDTO>>() {})
                    .block(TIMEOUT);
            return result == null ? null : result.getData();
        } catch (WebClientResponseException e) {
            log.error("Failed to fetch seckill product {}, status={}, body={}", productId, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch seckill product {}: {}", productId, e.getMessage(), e);
            return null;
        }
    }

    public List<SeckillKnowledgeDTO> searchProducts(String keyword) {
        try {
            Result<List<SeckillKnowledgeDTO>> result = webClientBuilder.build()
                    .get()
                    .uri(aiProperties.getSeckillServiceUrl() + "/seckill-product/products?name={keyword}", keyword)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<List<SeckillKnowledgeDTO>>>() {})
                    .block(TIMEOUT);
            return result == null || result.getData() == null ? List.of() : result.getData();
        } catch (WebClientResponseException e) {
            log.error("Failed to search seckill products by keyword {}, status={}, body={}", keyword, e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            log.error("Failed to search seckill products by keyword {}: {}", keyword, e.getMessage(), e);
            return List.of();
        }
    }
}
