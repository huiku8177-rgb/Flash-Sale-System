package com.flashsale.aiservice;

import com.flashsale.aiservice.config.AiProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiServiceApplicationTests {

    @Test
    void aiPropertiesValidationHelpersWork() {
        AiProperties properties = new AiProperties();
        assertFalse(properties.isChatClientConfigured());
        assertFalse(properties.isEmbeddingClientConfigured());

        properties.setEnabled(true);
        properties.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/");
        properties.setApiKey("test-key");
        properties.setChatModel("qwen-turbo");
        properties.setEmbeddingModel("text-embedding-v2");

        assertTrue(properties.isChatClientConfigured());
        assertTrue(properties.isEmbeddingClientConfigured());
    }
}
