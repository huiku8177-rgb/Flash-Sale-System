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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmbeddingClientTests {

    @Test
    void embedReturnsVectorWhenResponseIsValid() {
        EmbeddingClient client = createClient(HttpStatus.OK, "{\"data\":[{\"embedding\":[0.1,0.2,0.3]}]}");

        List<Double> result = client.embed("test text");

        assertEquals(List.of(0.1, 0.2, 0.3), result);
    }

    @Test
    void embedThrowsWhenTextIsBlank() {
        EmbeddingClient client = createClient(HttpStatus.OK, "{\"data\":[{\"embedding\":[0.1]}]}");

        assertThrows(IllegalArgumentException.class, () -> client.embed(" "));
    }

    @Test
    void embedThrowsWhenModelNotConfigured() {
        EmbeddingClient client = createClientWithModel(null, HttpStatus.OK, "{}");

        assertThrows(ModelInvokeException.class, () -> client.embed("test text"));
    }

    @Test
    void embedThrowsWhenResponseIsEmpty() {
        EmbeddingClient client = createClient(HttpStatus.OK, null);

        assertThrows(ModelInvokeException.class, () -> client.embed("test text"));
    }

    @Test
    void embedThrowsWhenDataIsMissing() {
        EmbeddingClient client = createClient(HttpStatus.OK, "{}");

        assertThrows(ModelInvokeException.class, () -> client.embed("test text"));
    }

    @Test
    void embedThrowsWhenEmbeddingIsMissing() {
        EmbeddingClient client = createClient(HttpStatus.OK, "{\"data\":[{}]}");

        assertThrows(ModelInvokeException.class, () -> client.embed("test text"));
    }

    @Test
    void embedThrowsWhenHttpCallFails() {
        EmbeddingClient client = createClient(HttpStatus.BAD_REQUEST, "{\"error\":\"bad request\"}");

        assertThrows(ModelInvokeException.class, () -> client.embed("test text"));
    }

    private EmbeddingClient createClient(HttpStatus status, String responseBody) {
        return createClientWithModel("text-embedding-v1", status, responseBody);
    }

    private EmbeddingClient createClientWithModel(String model, HttpStatus status, String responseBody) {
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
                .baseUrl("http://localhost:18080")
                .build();

        AiProperties aiProperties = new AiProperties();
        aiProperties.setBaseUrl("http://localhost:18080");
        aiProperties.setEmbeddingModel(model);
        aiProperties.setApiKey("test-key");

        return new EmbeddingClient(webClient, aiProperties);
    }
}
