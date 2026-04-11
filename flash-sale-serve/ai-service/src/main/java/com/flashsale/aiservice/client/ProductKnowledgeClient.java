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

/**
 * 普通商品知识客户端
 *
 * 通过 WebClient 调用远程普通商品服务，提供以下能力：
 * - 获取全量普通商品列表
 * - 根据商品ID获取单个普通商品详情
 * - 根据关键词模糊搜索普通商品
 *
 * 该类具备容错降级能力：任何异常均返回空数据，并记录错误日志，
 * 确保主业务流程不会因商品服务不可用而中断。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductKnowledgeClient {

  // HTTP 调用超时时间
  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final WebClient.Builder webClientBuilder;   // WebClient 构建器（由 Spring 注入）
  private final AiProperties aiProperties;            // AI 配置属性（含商品服务基础URL）

  /**
   * 获取全量普通商品列表
   *
   * 调用商品服务的全量接口，返回所有普通商品。
   * 若调用失败（超时、HTTP错误、响应异常），返回空列表并记录错误日志。
   *
   * @return 普通商品列表，失败时返回空列表
   */
  public List<ProductKnowledgeDTO> getAllProducts() {
    String targetUrl = aiProperties.getProductServiceUrl() + "/product/products";
    try {
      Result<List<ProductKnowledgeDTO>> result = webClientBuilder.build()
        .get()
        .uri(targetUrl)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Result<List<ProductKnowledgeDTO>>>() {})
        .block(TIMEOUT);
      // 解包 Result，若 data 为空则返回空列表
      return result == null || result.getData() == null ? List.of() : result.getData();
    } catch (WebClientResponseException e) {
      log.error("Failed to fetch products from {}, status={}, body={}", targetUrl, e.getStatusCode(), e.getResponseBodyAsString());
      return List.of();
    } catch (Exception e) {
      log.error("Failed to fetch products from {}: {}", targetUrl, e.getMessage(), e);
      return List.of();
    }
  }

  /**
   * 根据商品ID获取单个普通商品详情
   *
   * 调用商品服务的详情接口，路径参数为商品ID。
   * 若调用失败，返回 null 并记录错误日志。
   *
   * @param productId 商品ID
   * @return 普通商品详情，失败时返回 null
   */
  public ProductKnowledgeDTO getProductById(Long productId) {
    String targetUrl = aiProperties.getProductServiceUrl() + "/product/products/" + productId;
    try {
      Result<ProductKnowledgeDTO> result = webClientBuilder.build()
        .get()
        .uri(targetUrl)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Result<ProductKnowledgeDTO>>() {})
        .block(TIMEOUT);
      return result == null ? null : result.getData();
    } catch (WebClientResponseException e) {
      log.error("Failed to fetch product {} from {}, status={}, body={}", productId, targetUrl, e.getStatusCode(), e.getResponseBodyAsString());
      return null;
    } catch (Exception e) {
      log.error("Failed to fetch product {} from {}: {}", productId, targetUrl, e.getMessage(), e);
      return null;
    }
  }

  /**
   * 根据关键词模糊搜索普通商品
   *
   * 调用商品服务的搜索接口，通过 query 参数传递关键词。
   * 若调用失败，返回空列表并记录错误日志。
   *
   * @param keyword 搜索关键词（商品名称）
   * @return 匹配的普通商品列表，失败时返回空列表
   */
  public List<ProductKnowledgeDTO> searchProducts(String keyword) {
    String targetUrl = aiProperties.getProductServiceUrl() + "/product/products?name={keyword}";
    try {
      Result<List<ProductKnowledgeDTO>> result = webClientBuilder.build()
        .get()
        .uri(targetUrl, keyword)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Result<List<ProductKnowledgeDTO>>>() {})
        .block(TIMEOUT);
      return result == null || result.getData() == null ? List.of() : result.getData();
    } catch (WebClientResponseException e) {
      log.error("Failed to search products by keyword {} from {}, status={}, body={}",
        keyword, targetUrl, e.getStatusCode(), e.getResponseBodyAsString());
      return List.of();
    } catch (Exception e) {
      log.error("Failed to search products by keyword {} from {}: {}", keyword, targetUrl, e.getMessage(), e);
      return List.of();
    }
  }
}
