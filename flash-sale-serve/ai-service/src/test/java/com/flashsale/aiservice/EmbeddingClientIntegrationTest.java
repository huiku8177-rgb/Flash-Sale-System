package com.flashsale.aiservice;

import com.flashsale.aiservice.client.EmbeddingClient;
import com.flashsale.aiservice.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EmbeddingClientIntegrationTest {

    @Test
    void embedFallsBackToLocalVectorWhenAiIsDisabled() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(false);
        EmbeddingClient client = new EmbeddingClient(WebClient.builder().build(), properties);

        List<Double> vector = client.embed("秒杀商品推荐");

        assertEquals(16, vector.size());
        assertFalse(vector.stream().allMatch(value -> value == 0d));
    }
}
