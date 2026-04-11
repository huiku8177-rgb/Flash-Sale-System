package com.flashsale.aiservice.client;

import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.exception.ModelInvokeException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文本向量化客户端。
 *
 * <p>聊天链路应优先调用 {@link #embedSafely(String)}，由上层根据返回状态决定是否降级。
 * 严格模式 {@link #embed(String)} 仍然保留给知识同步等不希望隐式降级的调用方。</p>
 */
@Slf4j
@Component
public class EmbeddingClient {

  private static final Duration TIMEOUT = Duration.ofSeconds(30);
  private static final AtomicBoolean LOCAL_FALLBACK_WARNED = new AtomicBoolean(false);

  private static final String REASON_TEXT_BLANK = "TEXT_BLANK";
  private static final String REASON_LOCAL_FALLBACK = "LOCAL_EMBEDDING_FALLBACK";
  private static final String REASON_CONFIG_MISSING = "EMBEDDING_CONFIG_MISSING";
  private static final String REASON_EMPTY_RESPONSE = "EMPTY_EMBEDDING_RESPONSE";
  private static final String REASON_HTTP_ERROR = "EMBEDDING_HTTP_ERROR";
  private static final String REASON_CLIENT_ERROR = "EMBEDDING_CLIENT_ERROR";

  private final WebClient aiWebClient;
  private final AiProperties aiProperties;
  private final Environment environment;

  public EmbeddingClient(@Qualifier("aiWebClient") WebClient aiWebClient,
                         AiProperties aiProperties,
                         Environment environment) {
    this.aiWebClient = aiWebClient;
    this.aiProperties = aiProperties;
    this.environment = environment;
  }

  /**
   * 严格模式接口。
   *
   * <p>当向量化失败时直接抛异常，供知识同步等必须拿到真实向量的链路使用。</p>
   */
  public List<Double> embed(String text) {
    EmbeddingResult result = embedSafely(text);
    if (!result.isSuccess()) {
      throw new ModelInvokeException(result.getReason(), result.getError());
    }
    return result.getVector();
  }

  /**
   * 安全模式接口。
   *
   * <p>聊天链路不应因为 embedding 失败直接中断，因此这里返回结构化结果而不是直接抛异常。</p>
   */
  public EmbeddingResult embedSafely(String text) {
    if (!StringUtils.hasText(text)) {
      return EmbeddingResult.failure(REASON_TEXT_BLANK, "Embedding text must not be blank", null);
    }

    // 只有 local/dev 环境允许使用本地 hash 向量作为开发期兜底。
    if (!aiProperties.isEmbeddingClientConfigured()) {
      if (isLocalEmbeddingFallbackAllowed()) {
        if (LOCAL_FALLBACK_WARNED.compareAndSet(false, true)) {
          log.warn(
            "Embedding client is not fully configured, missing={}, activeProfiles={}, system will use local embedding fallback",
            aiProperties.missingEmbeddingConfigSummary(),
            activeProfilesSummary()
          );
        }
        return EmbeddingResult.success(
          localEmbedding(text),
          true,
          REASON_LOCAL_FALLBACK,
          "Embedding client not configured, local fallback vector was used"
        );
      }

      log.warn(
        "Embedding client is not fully configured, missing={}, activeProfiles={}, local embedding fallback is disabled",
        aiProperties.missingEmbeddingConfigSummary(),
        activeProfilesSummary()
      );
      return EmbeddingResult.failure(
        REASON_CONFIG_MISSING,
        "Embedding client not configured and local fallback is disabled in the current profile",
        null
      );
    }

    try {
      log.info("Invoking embedding model, model={}, textChars={}", aiProperties.getEmbeddingModel(), text.length());

      EmbeddingResponse response = aiWebClient.post()
        .uri("/v1/embeddings")
        .bodyValue(new EmbeddingRequest(aiProperties.getEmbeddingModel(), text))
        .retrieve()
        .bodyToMono(EmbeddingResponse.class)
        .block(TIMEOUT);

      if (response == null || response.getData() == null || response.getData().isEmpty()) {
        return EmbeddingResult.failure(REASON_EMPTY_RESPONSE, "Embedding API returned empty response", null);
      }

      List<Double> embedding = response.getData().get(0).getEmbedding();
      if (embedding == null || embedding.isEmpty()) {
        return EmbeddingResult.failure(REASON_EMPTY_RESPONSE, "Embedding vector is empty", null);
      }

      log.info("Embedding model invocation succeeded, model={}, dimensions={}", aiProperties.getEmbeddingModel(), embedding.size());
      return EmbeddingResult.success(embedding, false, null, null);
    } catch (WebClientResponseException e) {
      log.error("Embedding API error, status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
      return EmbeddingResult.failure(REASON_HTTP_ERROR, "Embedding API HTTP " + e.getStatusCode(), e);
    } catch (Exception e) {
      log.error("Failed to call embedding API: {}", e.getMessage(), e);
      return EmbeddingResult.failure(REASON_CLIENT_ERROR, "Failed to call embedding API: " + e.getMessage(), e);
    }
  }

  /**
   * 开发期本地兜底向量。
   *
   * <p>这里只是为了本地联调不断链，不代表语义质量可用。</p>
   */
  private List<Double> localEmbedding(String text) {
    double[] values = new double[16];
    char[] chars = text.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      values[i % values.length] += chars[i];
    }

    List<Double> vector = new ArrayList<>(values.length);
    double norm = 0d;
    for (double value : values) {
      norm += value * value;
    }
    norm = Math.sqrt(norm);
    if (norm == 0d) {
      norm = 1d;
    }
    for (double value : values) {
      vector.add(value / norm);
    }
    return vector;
  }

  private boolean isLocalEmbeddingFallbackAllowed() {
    for (String profile : environment.getActiveProfiles()) {
      if ("local".equalsIgnoreCase(profile) || "dev".equalsIgnoreCase(profile)) {
        return true;
      }
    }
    return false;
  }

  private String activeProfilesSummary() {
    String[] profiles = environment.getActiveProfiles();
    return profiles.length == 0 ? "<none>" : String.join(",", profiles);
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EmbeddingResult {
    private boolean success;
    private List<Double> vector;
    private boolean degraded;
    private String reason;
    private String message;
    private Throwable error;

    public static EmbeddingResult success(List<Double> vector, boolean degraded, String reason, String message) {
      return new EmbeddingResult(true, vector, degraded, reason, message, null);
    }

    public static EmbeddingResult failure(String reason, String message, Throwable error) {
      return new EmbeddingResult(false, List.of(), false, reason, message, error);
    }
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
