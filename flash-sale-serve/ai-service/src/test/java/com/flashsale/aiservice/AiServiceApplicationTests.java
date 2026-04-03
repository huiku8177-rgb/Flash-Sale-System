package com.flashsale.aiservice;

import com.flashsale.aiservice.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class AiServiceApplicationTests {

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private WebClient aiWebClient;

    @Test
    void contextLoads() {
        assertNotNull(aiProperties);
        assertNotNull(webClientBuilder);
        assertNotNull(aiWebClient);
        assertEquals("http://localhost:18080", aiProperties.getBaseUrl());
        assertEquals("text-embedding-v1", aiProperties.getEmbeddingModel());
        assertEquals("qwen-turbo", aiProperties.getChatModel());
    }
}

@SpringBootTest(properties = {
        "ai.base-url=http://localhost:28080",
        "ai.api-key=test-key",
        "ai.embedding-model=embedding-test-model",
        "ai.chat-model=chat-test-model"
})
class AiPropertiesOverrideTests {

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private WebClient aiWebClient;

    @Test
    void propertiesCanBeOverridden() {
        assertNotNull(aiProperties);
        assertNotNull(aiWebClient);
        assertEquals("http://localhost:28080", aiProperties.getBaseUrl());
        assertEquals("test-key", aiProperties.getApiKey());
        assertEquals("embedding-test-model", aiProperties.getEmbeddingModel());
        assertEquals("chat-test-model", aiProperties.getChatModel());
    }
}
