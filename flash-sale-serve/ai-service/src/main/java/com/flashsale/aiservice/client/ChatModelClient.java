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
public class ChatModelClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final WebClient aiWebClient;
    private final AiProperties aiProperties;

    public ChatModelClient(@Qualifier("aiWebClient") WebClient aiWebClient, AiProperties aiProperties) {
        this.aiWebClient = aiWebClient;
        this.aiProperties = aiProperties;
    }

    public String chat(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        if (!StringUtils.hasText(aiProperties.getChatModel())) {
            throw new ModelInvokeException("chat model is not configured");
        }

        log.debug("Requesting chat, model={}, promptLength={}", aiProperties.getChatModel(), prompt.length());

        ChatResponse response;
        try {
            response = aiWebClient.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(new ChatRequest(
                            aiProperties.getChatModel(),
                            List.of(new Message("user", prompt))))
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .block(TIMEOUT);
        } catch (WebClientResponseException e) {
            log.error("Chat API error, status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ModelInvokeException(
                    "Chat API HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new ModelInvokeException("Failed to call chat API: " + e.getMessage(), e);
        }

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new ModelInvokeException("Chat API returned empty choices");
        }

        Message message = response.getChoices().get(0).getMessage();
        if (message == null || !StringUtils.hasText(message.getContent())) {
            throw new ModelInvokeException("Chat API returned empty message content");
        }

        String answer = message.getContent();
        log.debug("Chat succeeded, answerLength={}", answer.length());
        return answer;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ChatRequest {
        private String model;
        private List<Message> messages;
    }

    @Data
    @NoArgsConstructor
    private static class ChatResponse {
        private List<Choice> choices;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Choice {
        private Message message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Message {
        private String role;
        private String content;
    }
}
