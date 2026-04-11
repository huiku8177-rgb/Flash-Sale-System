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

/**
 * 秒杀商品知识客户端
 *
 * 通过 WebClient 调用远程秒杀商品服务，提供以下能力：
 * - 获取全量秒杀商品列表
 * - 根据商品ID获取单个秒杀商品详情
 * - 根据关键词模糊搜索秒杀商品
 *
 * 该类具备容错降级能力：任何异常均返回空数据，并记录错误日志，
 * 确保主业务流程不会因秒杀服务不可用而中断。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillKnowledgeClient {

  // HTTP 调用超时时间
  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final WebClient.Builder webClientBuilder;   // WebClient 构建器（由 Spring 注入）
  private final AiProperties aiProperties;            // AI 配置属性（含秒杀服务基础URL）

  /**
   * 获取全量秒杀商品列表
   *
   * 调用秒杀服务的全量接口，返回所有秒杀商品。
   * 若调用失败（超时、HTTP错误、响应异常），返回空列表并记录错误日志。
   *
   * @return 秒杀商品列表，失败时返回空列表
   */
  public List<SeckillKnowledgeDTO> getAllProducts() {
    String targetUrl = aiProperties.getSeckillServiceUrl() + "/seckill-product/products";
    try {
      Result<List<SeckillKnowledgeDTO>> result = webClientBuilder.build()
        .get()
        .uri(targetUrl)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Result<List<SeckillKnowledgeDTO>>>() {})
        .block(TIMEOUT);
      // 解包 Result，若 data 为空则返回空列表
      return result == null || result.getData() == null ? List.of() : result.getData();
    } catch (WebClientResponseException e) {
      log.error("Failed to fetch seckill products from {}, status={}, body={}",
        targetUrl, e.getStatusCode(), e.getResponseBodyAsString());
      return List.of();
    } catch (Exception e) {
      log.error("Failed to fetch seckill products from {}: {}", targetUrl, e.getMessage(), e);
      return List.of();
    }
  }

  /**
   * 根据商品ID获取单个秒杀商品详情
   *
   * 调用秒杀服务的详情接口，路径参数为商品ID。
   * 若调用失败，返回 null 并记录错误日志。
   *
   * @param productId 商品ID
   * @return 秒杀商品详情，失败时返回 null
   */
  public SeckillKnowledgeDTO getProductById(Long productId) {
    String targetUrl = aiProperties.getSeckillServiceUrl() + "/seckill-product/products/" + productId;
    try {
      Result<SeckillKnowledgeDTO> result = webClientBuilder.build()
        .get()
        .uri(targetUrl)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Result<SeckillKnowledgeDTO>>() {})
        .block(TIMEOUT);
      return result == null ? null : result.getData();
    } catch (WebClientResponseException e) {
      log.error("Failed to fetch seckill product {} from {}, status={}, body={}",
        productId, targetUrl, e.getStatusCode(), e.getResponseBodyAsString());
      return null;
    } catch (Exception e) {
      log.error("Failed to fetch seckill product {} from {}: {}", productId, targetUrl, e.getMessage(), e);
      return null;
    }
  }

  /**
   * 根据关键词模糊搜索秒杀商品
   *
   * 调用秒杀服务的搜索接口，通过 query 参数传递关键词。
   * 若调用失败，返回空列表并记录错误日志。
   *
   * @param keyword 搜索关键词（商品名称）
   * @return 匹配的秒杀商品列表，失败时返回空列表
   */
  public List<SeckillKnowledgeDTO> searchProducts(String keyword) {
    String targetUrl = aiProperties.getSeckillServiceUrl() + "/seckill-product/products?name={keyword}";
    try {
      Result<List<SeckillKnowledgeDTO>> result = webClientBuilder.build()
        .get()
        .uri(targetUrl, keyword)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Result<List<SeckillKnowledgeDTO>>>() {})
        .block(TIMEOUT);
      return result == null || result.getData() == null ? List.of() : result.getData();
    } catch (WebClientResponseException e) {
      log.error("Failed to search seckill products by keyword {} from {}, status={}, body={}",
        keyword, targetUrl, e.getStatusCode(), e.getResponseBodyAsString());
      return List.of();
    } catch (Exception e) {
      log.error("Failed to search seckill products by keyword {} from {}: {}", keyword, targetUrl, e.getMessage(), e);
      return List.of();
    }
  }
}
