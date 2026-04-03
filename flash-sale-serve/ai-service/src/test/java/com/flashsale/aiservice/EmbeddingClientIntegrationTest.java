package com.flashsale.aiservice;

import com.flashsale.aiservice.client.EmbeddingClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "ai.base-url=https://dashscope.aliyuncs.com/compatible-mode",
        "ai.api-key=sk-db9e1bd136c648018e2db2e37a74e11a",
        "ai.embedding-model=text-embedding-v2"
})
class EmbeddingClientIntegrationTest {

    @Autowired
    private EmbeddingClient embeddingClient;

    @Test
    void embedReturnsValidVector() {
        List<Double> vector = embeddingClient.embed("闪购商品推荐");

        assertNotNull(vector);
        assertFalse(vector.isEmpty());
        assertTrue(vector.stream().allMatch(v -> v >= -1.0 && v <= 1.0));
        System.out.println("Embedding dimensions: " + vector.size());
        System.out.println("First 5 values: " + vector.subList(0, Math.min(5, vector.size())));
    }

    @Test
    void embedDifferentTextsProduceDifferentVectors() {
        List<Double> v1 = embeddingClient.embed("手机");
        List<Double> v2 = embeddingClient.embed("电脑");

        assertNotNull(v1);
        assertNotNull(v2);
        assertFalse(v1.equals(v2));
    }
}
