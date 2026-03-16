package com.flashsale.orderservice.mq.consumer;

import com.flashsale.common.redis.RedisKeys;
import com.flashsale.orderservice.mq.config.RabbitMqConfig;
import com.flashsale.orderservice.mq.message.SeckillMessage;
import com.flashsale.orderservice.service.SeckillOrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillOrderConsumer
 * @date 2026/3/16 12:51
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private static final long IDEMPOTENT_PROCESSING_TTL_SECONDS = 300L;
    private static final long IDEMPOTENT_DONE_TTL_SECONDS = 7 * 24 * 3600L;

    private final SeckillOrderService orderService;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitMqConfig.SECKILL_ORDER_QUEUE, containerFactory = "seckillListenerContainerFactory")
    public void consumeSeckillOrder(SeckillMessage message, Channel channel, Message amqpMessage) throws IOException {
        String messageId = message.getMessageId();
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();

        if (messageId == null || messageId.isBlank()) {
            log.error("收到无messageId消息，直接拒绝入死信 userId={}, productId={}", message.getUserId(), message.getProductId());
            channel.basicReject(deliveryTag, false);
            return;
        }

        String idempotentKey = RedisKeys.mqConsume(messageId);
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "PROCESSING", IDEMPOTENT_PROCESSING_TTL_SECONDS, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(locked)) {
            log.info("重复消费消息，已幂等跳过 messageId={}", messageId);
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            orderService.createSeckillOrder(message);
            stringRedisTemplate.opsForValue().set(idempotentKey, "DONE", IDEMPOTENT_DONE_TTL_SECONDS, TimeUnit.SECONDS);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            stringRedisTemplate.delete(idempotentKey);
            log.error("消费秒杀消息失败，等待重试 messageId={}", messageId, e);
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMqConfig.SECKILL_ORDER_DLQ)
    public void consumeDeadLetter(SeckillMessage message) {
        log.error("接收到秒杀死信消息 messageId={}, userId={}, productId={}",
                message.getMessageId(), message.getUserId(), message.getProductId());
        orderService.handleSeckillFailure(message);
        if (message.getMessageId() != null && !message.getMessageId().isBlank()) {
            stringRedisTemplate.opsForValue().set(
                    RedisKeys.mqConsume(message.getMessageId()),
                    "FAILED",
                    IDEMPOTENT_DONE_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
        }
    }
}
