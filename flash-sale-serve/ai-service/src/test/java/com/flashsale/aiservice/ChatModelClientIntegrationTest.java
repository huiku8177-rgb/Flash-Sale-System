package com.flashsale.aiservice;

import com.flashsale.aiservice.client.ChatModelClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "ai.base-url=https://dashscope.aliyuncs.com/compatible-mode/",
        "ai.api-key=${FLASH_SALE_AI_API_KEY}",
        "ai.chat-model=qwen-turbo"
})
class ChatModelClientIntegrationTest {

    @Autowired
    private ChatModelClient chatModelClient;

    @Test
    void chatReturnsAnswer() {
        String answer = chatModelClient.chat("你好，请用一句话介绍自己");

        assertNotNull(answer);
        assertFalse(answer.isEmpty());
        System.out.println("Answer: " + answer);
    }
}
