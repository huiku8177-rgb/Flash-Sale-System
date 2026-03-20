package com.flashsale.seckillservice.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息转换器配置
 *
 * @author strive_qin
 * @version 1.0
 * @description RabbitMqMessageConverterConfig
 * @date 2026/3/16 13:17
 */
@Configuration
public class RabbitMqMessageConverterConfig {

    // 统一使用 JSON 作为消息体序列化格式
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
