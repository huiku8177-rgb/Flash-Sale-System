package com.flashsale.aiservice.client;

import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.exception.ModelInvokeException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatModelClientTests {

    private static final String RESPONSE_BODY =
            "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"hello\"}}]}";

    @Test
    void chatReturnsAnswerWhenResponseIsValid() {
        ChatModelClient client = createClient(HttpStatus.OK, RESPONSE_BODY);

        String result = client.chat("hi");

        assertEquals("hello", result);
    }

    @Test
    void chatThrowsWhenPromptIsBlank() {
        ChatModelClient client = createClient(HttpStatus.OK, RESPONSE_BODY);

        assertThrows(IllegalArgumentException.class, () -> client.chat(" "));
    }

    @Test
    void chatThrowsWhenModelNotConfigured() {
        ChatModelClient client = createClientWithModel(null, HttpStatus.OK, RESPONSE_BODY);

        assertThrows(ModelInvokeException.class, () -> client.chat("test"));
    }

    @Test
    void chatThrowsWhenChoicesIsEmpty() {
        ChatModelClient client = createClient(HttpStatus.OK, "{\"choices\":[]}");

        assertThrows(ModelInvokeException.class, () -> client.chat("test"));
    }

    @Test
    void chatThrowsWhenMessageIsNull() {
        ChatModelClient client = createClient(HttpStatus.OK, "{\"choices\":[{}]}");

        assertThrows(ModelInvokeException.class, () -> client.chat("test"));
    }

    @Test
    void chatThrowsWhenContentIsNull() {
        ChatModelClient client = createClient(HttpStatus.OK,
                "{\"choices\":[{\"message\":{\"role\":\"assistant\"}}]}");

        assertThrows(ModelInvokeException.class, () -> client.chat("test"));
    }

    @Test
    void chatThrowsWhenHttpCallFails() {
        ChatModelClient client = createClient(HttpStatus.BAD_REQUEST,
                "{\"error\":{\"message\":\"invalid request\",\"type\":\"invalid_request_error\"}}");

        ModelInvokeException ex = assertThrows(ModelInvokeException.class, () -> client.chat("test"));
        assertNotNull(ex.getCause());
    }

    private ChatModelClient createClient(HttpStatus status, String responseBody) {
        return createClientWithModel("qwen-turbo", status, responseBody);
    }

    private ChatModelClient createClientWithModel(String model, HttpStatus status, String responseBody) {
        ExchangeFunction exchangeFunction = request -> {
            ClientResponse.Builder builder = ClientResponse.create(status)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            if (responseBody != null) {
                builder.body(responseBody);
            }
            return Mono.just(builder.build());
        };

        WebClient webClient = WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/")
                .build();

        AiProperties aiProperties = new AiProperties();
        aiProperties.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/");
        aiProperties.setApiKey("test-key");
        aiProperties.setChatModel(model);

        return new ChatModelClient(webClient, aiProperties);
    }
}
