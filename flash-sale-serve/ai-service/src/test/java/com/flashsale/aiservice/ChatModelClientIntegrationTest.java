package com.flashsale.aiservice;

import com.flashsale.aiservice.client.ChatModelClient;
import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.exception.ModelInvokeException;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatModelClientIntegrationTest {

    @Test
    void chatThrowsWhenAiIsDisabled() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(false);
        ChatModelClient client = new ChatModelClient(WebClient.builder().build(), properties);

        assertThrows(ModelInvokeException.class, () -> client.chat("hello"));
    }
}
