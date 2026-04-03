package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.domain.dto.ChatRequestDTO;
import com.flashsale.aiservice.domain.vo.ChatResponseVO;
import com.flashsale.aiservice.exception.ModelInvokeException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagChatServiceImplTests {

    private static final String EMBEDDING_RESPONSE =
            "{\"data\":[{\"embedding\":[0.1,0.2,0.3]}]}";

    private static final String CHAT_RESPONSE =
            "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"支持七天无理由退货。\"}}]}";

    @Test
    void chatReturnsAnswer() {
        RagChatServiceImpl service = createService(EMBEDDING_RESPONSE, CHAT_RESPONSE);

        ChatRequestDTO request = new ChatRequestDTO();
        request.setQuestion("支持退货吗？");

        ChatResponseVO result = service.chat(request);

        assertNotNull(result.getAnswer());
        assertEquals("支持七天无理由退货。", result.getAnswer());
        assertEquals(List.of("暂无知识库参考"), result.getSources());
    }

    @Test
    void chatThrowsWhenEmbeddingFails() {
        RagChatServiceImpl service = createServiceWithStatus(HttpStatus.BAD_REQUEST,
                "{\"error\":\"bad request\"}", CHAT_RESPONSE);

        ChatRequestDTO request = new ChatRequestDTO();
        request.setQuestion("test");

        org.junit.jupiter.api.Assertions.assertThrows(ModelInvokeException.class, () -> service.chat(request));
    }

    @Test
    void chatThrowsWhenChatFails() {
        RagChatServiceImpl service = createServiceWithStatus(HttpStatus.OK,
                EMBEDDING_RESPONSE, "{\"error\":\"internal error\"}");

        ChatRequestDTO request = new ChatRequestDTO();
        request.setQuestion("test");

        org.junit.jupiter.api.Assertions.assertThrows(ModelInvokeException.class, () -> service.chat(request));
    }

    private RagChatServiceImpl createService(String embeddingBody, String chatBody) {
        return createServiceWithStatus(HttpStatus.OK, embeddingBody, chatBody);
    }

    private RagChatServiceImpl createServiceWithStatus(HttpStatus embeddingStatus, String embeddingBody,
                                                        String chatBody) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(createExchangeFunction(embeddingStatus, embeddingBody, chatBody))
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/")
                .build();

        AiProperties aiProperties = new AiProperties();
        aiProperties.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/");
        aiProperties.setApiKey("test-key");
        aiProperties.setEmbeddingModel("text-embedding-v2");
        aiProperties.setChatModel("qwen-turbo");

        com.flashsale.aiservice.client.EmbeddingClient embeddingClient =
                new com.flashsale.aiservice.client.EmbeddingClient(webClient, aiProperties);
        com.flashsale.aiservice.client.ChatModelClient chatModelClient =
                new com.flashsale.aiservice.client.ChatModelClient(webClient, aiProperties);

        return new RagChatServiceImpl(embeddingClient, chatModelClient);
    }

    private ExchangeFunction createExchangeFunction(HttpStatus embeddingStatus, String embeddingBody,
                                                     String chatBody) {
        return request -> {
            String uri = request.url().toString();
            if (uri.contains("/v1/embeddings")) {
                ClientResponse.Builder builder = ClientResponse.create(embeddingStatus)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                if (embeddingBody != null) {
                    builder.body(embeddingBody);
                }
                return Mono.just(builder.build());
            } else {
                ClientResponse.Builder builder = ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                if (chatBody != null) {
                    builder.body(chatBody);
                }
                return Mono.just(builder.build());
            }
        };
    }
}
