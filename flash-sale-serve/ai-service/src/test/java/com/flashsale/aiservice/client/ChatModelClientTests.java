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
        ChatModelClient client = createClient(true, HttpStatus.OK, RESPONSE_BODY);

        String result = client.chat("hi");

        assertEquals("hello", result);
    }

    @Test
    void chatThrowsWhenPromptIsBlank() {
        ChatModelClient client = createClient(true, HttpStatus.OK, RESPONSE_BODY);

        assertThrows(IllegalArgumentException.class, () -> client.chat(" "));
    }

    @Test
    void chatThrowsWhenModelNotConfigured() {
        ChatModelClient client = createClient(false, HttpStatus.OK, RESPONSE_BODY);

        assertThrows(ModelInvokeException.class, () -> client.chat("test"));
    }

    @Test
    void chatThrowsWhenChoicesIsEmpty() {
        ChatModelClient client = createClient(true, HttpStatus.OK, "{\"choices\":[]}");

        assertThrows(ModelInvokeException.class, () -> client.chat("test"));
    }

    @Test
    void chatThrowsWhenHttpCallFails() {
        ChatModelClient client = createClient(true, HttpStatus.BAD_REQUEST, "{\"error\":\"invalid request\"}");

        ModelInvokeException ex = assertThrows(ModelInvokeException.class, () -> client.chat("test"));
        assertNotNull(ex.getCause());
    }

    private ChatModelClient createClient(boolean enabled, HttpStatus status, String responseBody) {
        ExchangeFunction exchangeFunction = request -> {
            ClientResponse.Builder builder = ClientResponse.create(status)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            if (responseBody != null) {
                builder.body(responseBody);
            }
            return Mono.just(builder.build());
        };

        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        AiProperties aiProperties = new AiProperties();
        aiProperties.setEnabled(enabled);
        aiProperties.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/");
        aiProperties.setApiKey("test-key");
        aiProperties.setChatModel("qwen-turbo");
        return new ChatModelClient(webClient, aiProperties);
    }
}
