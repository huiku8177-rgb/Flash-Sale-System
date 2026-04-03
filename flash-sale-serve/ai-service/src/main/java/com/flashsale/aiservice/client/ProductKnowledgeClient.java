package com.flashsale.aiservice.client;

import com.flashsale.aiservice.domain.dto.ProductKnowledgeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private static final String PRODUCT_SERVICE_URL = "http://localhost:8084";

    private final WebClient.Builder webClientBuilder;

    public List<ProductKnowledgeDTO> getAllProducts() {
        log.debug("Fetching all products from product-service");

        try {
            return webClientBuilder.build()
                    .get()
                    .uri(PRODUCT_SERVICE_URL + "/product/products")
                    .retrieve()
                    .bodyToFlux(ProductKnowledgeDTO.class)
                    .collectList()
                    .block(TIMEOUT);
        } catch (WebClientResponseException e) {
            log.error("Failed to fetch products, status={}, body={}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            log.error("Failed to fetch products: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public ProductKnowledgeDTO getProductById(Long productId) {
        log.debug("Fetching product by id: {}", productId);

        try {
            return webClientBuilder.build()
                    .get()
                    .uri(PRODUCT_SERVICE_URL + "/product/products/" + productId)
                    .retrieve()
                    .bodyToMono(ProductKnowledgeDTO.class)
                    .block(TIMEOUT);
        } catch (WebClientResponseException e) {
            log.error("Failed to fetch product {}, status={}, body={}", 
                    productId, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch product {}: {}", productId, e.getMessage(), e);
            return null;
        }
    }
}
