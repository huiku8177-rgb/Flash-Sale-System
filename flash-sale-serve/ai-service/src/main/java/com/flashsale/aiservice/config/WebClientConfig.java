package com.flashsale.aiservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }

    @Bean
    public WebClient aiWebClient(WebClient.Builder builder, AiProperties aiProperties) {
        WebClient.Builder webClientBuilder = builder.clone()
                .baseUrl(aiProperties.getBaseUrl());

        if (StringUtils.hasText(aiProperties.getApiKey())) {
            webClientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getApiKey());
        }

        return webClientBuilder.build();
    }
}
