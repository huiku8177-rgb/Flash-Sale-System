package com.flashsale.orderservice.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author strive_qin
 * @version 1.0
 * @description RabbitMqMessageConverterConfig
 * @date 2026/3/16 13:17
 */
@Configuration
public class RabbitMqMessageConverterConfig {

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
