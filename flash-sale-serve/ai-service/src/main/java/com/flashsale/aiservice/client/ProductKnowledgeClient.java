package com.flashsale.aiservice.client;

import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.domain.dto.ProductKnowledgeDTO;
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
public class ProductKnowledgeClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient.Builder webClientBuilder;
    private final AiProperties aiProperties;

    public List<ProductKnowledgeDTO> getAllProducts() {
        try {
            Result<List<ProductKnowledgeDTO>> result = webClientBuilder.build()
                    .get()
                    .uri(aiProperties.getProductServiceUrl() + "/product/products")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<List<ProductKnowledgeDTO>>>() {})
                    .block(TIMEOUT);
            return result == null || result.getData() == null ? List.of() : result.getData();
        } catch (WebClientResponseException e) {
            log.error("Failed to fetch products, status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            log.error("Failed to fetch products: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public ProductKnowledgeDTO getProductById(Long productId) {
        try {
            Result<ProductKnowledgeDTO> result = webClientBuilder.build()
                    .get()
                    .uri(aiProperties.getProductServiceUrl() + "/product/products/" + productId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<ProductKnowledgeDTO>>() {})
                    .block(TIMEOUT);
            return result == null ? null : result.getData();
        } catch (WebClientResponseException e) {
            log.error("Failed to fetch product {}, status={}, body={}", productId, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch product {}: {}", productId, e.getMessage(), e);
            return null;
        }
    }

    public List<ProductKnowledgeDTO> searchProducts(String keyword) {
        try {
            Result<List<ProductKnowledgeDTO>> result = webClientBuilder.build()
                    .get()
                    .uri(aiProperties.getProductServiceUrl() + "/product/products?name={keyword}", keyword)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<List<ProductKnowledgeDTO>>>() {})
                    .block(TIMEOUT);
            return result == null || result.getData() == null ? List.of() : result.getData();
        } catch (WebClientResponseException e) {
            log.error("Failed to search products by keyword {}, status={}, body={}", keyword, e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            log.error("Failed to search products by keyword {}: {}", keyword, e.getMessage(), e);
            return List.of();
        }
    }
}
