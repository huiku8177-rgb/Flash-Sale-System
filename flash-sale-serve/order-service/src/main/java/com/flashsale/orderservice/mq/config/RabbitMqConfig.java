package com.flashsale.orderservice.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author strive_qin
 * @version 1.0
 * @description RabbitMqConfig
 * @date 2026/3/16 12:50
 */

@Configuration
public class RabbitMqConfig {

    public static final String SECKILL_EXCHANGE = "seckill.exchange";
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order";

    @Bean
    public TopicExchange seckillExchange() {
        return new TopicExchange(SECKILL_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillOrderQueue() {
        return new Queue(SECKILL_ORDER_QUEUE, true);
    }

    @Bean
    public Binding seckillOrderBinding(Queue seckillOrderQueue, TopicExchange seckillExchange) {
        return BindingBuilder
                .bind(seckillOrderQueue)
                .to(seckillExchange)
                .with(SECKILL_ORDER_ROUTING_KEY);
    }
}
