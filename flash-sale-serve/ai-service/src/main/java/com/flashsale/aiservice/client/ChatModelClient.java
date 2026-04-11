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

/**
 * 大模型对话客户端
 *
 * 负责通过 HTTP 调用远程部署的大语言模型服务（兼容 OpenAI API 格式）。
 * 将构建好的 Prompt 发送给模型，并返回模型生成的回答文本。
 *
 * 特性：
 * - 使用 WebClient 进行非阻塞 HTTP 调用，并设置较长的超时时间以适应大模型生成耗时。
 * - 在调用前会校验 Prompt 是否为空以及模型配置是否完整。
 * - 所有网络或响应异常均包装为 ModelInvokeException 抛出，便于上层业务统一处理。
 */
@Slf4j
@Component
public class ChatModelClient {

  // 调用超时时间（大模型生成回答可能耗时较长，设置为 60 秒）
  private static final Duration TIMEOUT = Duration.ofSeconds(60);

  // 专门用于调用 AI 服务的 WebClient（已在配置类中注入基础 URL 和认证头）
  private final WebClient aiWebClient;

  // AI 服务配置属性，包含模型名称、服务地址等
  private final AiProperties aiProperties;

  public ChatModelClient(@Qualifier("aiWebClient") WebClient aiWebClient, AiProperties aiProperties) {
    this.aiWebClient = aiWebClient;
    this.aiProperties = aiProperties;
  }

  /**
   * 调用大模型生成对话回答
   *
   * 执行流程：
   * 1. 校验 Prompt 是否为空。
   * 2. 检查大模型客户端配置是否完整（URL、API Key 等）。
   * 3. 构建 OpenAI 兼容的请求体，发送 POST 请求到 /v1/chat/completions。
   * 4. 解析响应，提取模型返回的消息内容。
   * 5. 若任何环节失败，抛出 ModelInvokeException。
   *
   * @param prompt 完整的提示词文本，包含系统指令、知识证据、对话历史和用户问题
   * @return 模型生成的回答字符串
   * @throws ModelInvokeException 当 Prompt 为空、配置缺失、网络异常或响应无效时抛出
   */
  public String chat(String prompt) {
    // 1. 校验 Prompt 不能为空
    if (!StringUtils.hasText(prompt)) {
      throw new IllegalArgumentException("prompt must not be blank");
    }

    // 2. 检查大模型客户端是否已正确配置（URL、API Key 等）
    if (!aiProperties.isChatClientConfigured()) {
      throw new ModelInvokeException("chat model is not configured, missing=" + aiProperties.missingChatConfigSummary());
    }

    try {
      log.info("Invoking chat model, model={}, promptChars={}", aiProperties.getChatModel(), prompt.length());

      // 3. 构建请求体并发起调用
      ChatResponse response = aiWebClient.post()
        .uri("/v1/chat/completions")
        .bodyValue(new ChatRequest(aiProperties.getChatModel(), List.of(new Message("user", prompt))))
        .retrieve()
        .bodyToMono(ChatResponse.class)
        .block(TIMEOUT);

      // 4. 校验响应内容的完整性
      if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
        throw new ModelInvokeException("Chat API returned empty choices");
      }
      Message message = response.getChoices().get(0).getMessage();
      if (message == null || !StringUtils.hasText(message.getContent())) {
        throw new ModelInvokeException("Chat API returned empty message content");
      }

      log.info("Chat model invocation succeeded, model={}, answerChars={}", aiProperties.getChatModel(), message.getContent().length());
      return message.getContent();

    } catch (WebClientResponseException e) {
      // HTTP 错误响应（如 4xx、5xx）
      log.error("Chat API error, status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new ModelInvokeException("Chat API HTTP " + e.getStatusCode(), e);
    } catch (Exception e) {
      // 如果已经是业务异常，直接抛出避免重复包装
      if (e instanceof ModelInvokeException modelInvokeException) {
        throw modelInvokeException;
      }
      // 其他网络或解析异常
      throw new ModelInvokeException("Failed to call chat API: " + e.getMessage(), e);
    }
  }

  // ==================== 内部数据类（OpenAI API 兼容格式） ====================

  /**
   * 对话请求体
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  private static class ChatRequest {
    private String model;               // 模型名称，如 gpt-3.5-turbo
    private List<Message> messages;     // 对话消息列表
  }

  /**
   * 对话响应体
   */
  @Data
  @NoArgsConstructor
  private static class ChatResponse {
    private List<Choice> choices;       // 模型生成的候选回复列表
  }

  /**
   * 单个候选回复
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  private static class Choice {
    private Message message;            // 该候选回复包含的消息对象
  }

  /**
   * 对话消息
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  private static class Message {
    private String role;                // 角色：system / user / assistant
    private String content;             // 消息具体内容
  }
}
