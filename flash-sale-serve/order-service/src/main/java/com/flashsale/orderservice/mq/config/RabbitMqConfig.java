package com.flashsale.orderservice.mq.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 基础设施配置。
 *
 * <p>说明：
 * - 主流程：exchange -> seckill.order.queue
 * - 异常流程：重试耗尽后 republish 到 DLX -> seckill.order.dlq
 * </p>
 */
@Configuration
public class RabbitMqConfig {

    /** 正常业务交换机。 */
    public static final String SECKILL_EXCHANGE = "seckill.exchange";
    /** 秒杀下单主消费队列。 */
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";
    /** 主流程路由键。 */
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order";

    /** 死信交换机。 */
    public static final String SECKILL_DLX_EXCHANGE = "seckill.dlx.exchange";
    /** 秒杀下单死信队列。 */
    public static final String SECKILL_ORDER_DLQ = "seckill.order.dlq";
    /** 死信路由键。 */
    public static final String SECKILL_ORDER_DLQ_ROUTING_KEY = "seckill.order.dlq";

    @Bean
    public TopicExchange seckillExchange() {
        return new TopicExchange(SECKILL_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange seckillDeadLetterExchange() {
        return new TopicExchange(SECKILL_DLX_EXCHANGE, true, false);
    }

    /**
     * 主队列声明：绑定死信交换机参数，保证异常消息可追踪。
     */
    @Bean
    public Queue seckillOrderQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", SECKILL_DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", SECKILL_ORDER_DLQ_ROUTING_KEY);
        return new Queue(SECKILL_ORDER_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue seckillOrderDeadLetterQueue() {
        return new Queue(SECKILL_ORDER_DLQ, true);
    }

    @Bean
    public Binding seckillOrderBinding(Queue seckillOrderQueue, TopicExchange seckillExchange) {
        return BindingBuilder
                .bind(seckillOrderQueue)
                .to(seckillExchange)
                .with(SECKILL_ORDER_ROUTING_KEY);
    }

    @Bean
    public Binding seckillOrderDeadLetterBinding(Queue seckillOrderDeadLetterQueue, TopicExchange seckillDeadLetterExchange) {
        return BindingBuilder
                .bind(seckillOrderDeadLetterQueue)
                .to(seckillDeadLetterExchange)
                .with(SECKILL_ORDER_DLQ_ROUTING_KEY);
    }

    /**
     * 秒杀消费者容器工厂。
     *
     * <p>关键策略：</p>
     * <ul>
     *     <li>MANUAL ACK：业务成功后显式确认；</li>
     *     <li>defaultRequeueRejected=false：异常不直接回原队列，避免无限堆积；</li>
     *     <li>stateless retry：最多重试 3 次；</li>
     *     <li>重试耗尽后由 recoverer 转发到 DLX。</li>
     * </ul>
     */
    @Bean
    public SimpleRabbitListenerContainerFactory seckillListenerContainerFactory(
            ConnectionFactory connectionFactory,
            RabbitTemplate rabbitTemplate,
            MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setMessageConverter(messageConverter);

        MessageRecoverer recoverer = new RepublishMessageRecoverer(
                rabbitTemplate,
                SECKILL_DLX_EXCHANGE,
                SECKILL_ORDER_DLQ_ROUTING_KEY
        );

        factory.setAdviceChain(
                RetryInterceptorBuilder.stateless()
                        .maxAttempts(3)
                        .recoverer(recoverer)
                        .build()
        );
        return factory;
    }
}
