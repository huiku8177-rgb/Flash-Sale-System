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
 * 秒杀订单消息消费者。
 *
 * <p>职责：</p>
 * <ul>
 *     <li>消费主队列消息，执行下单逻辑；</li>
 *     <li>基于 Redis messageId 做消费幂等；</li>
 *     <li>手动 ACK，失败抛异常触发重试；</li>
 *     <li>消费死信并触发补偿逻辑。</li>
 * </ul>
 */
/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillOrderConsumer
 * @date 2026/3/20 00:00
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    /** 幂等键“处理中”状态 TTL，避免消费者异常退出后长期锁死。 */
    private static final long IDEMPOTENT_PROCESSING_TTL_SECONDS = 300L;
    /** 幂等键“完成/失败”状态 TTL，覆盖一段活动回溯周期。 */
    private static final long IDEMPOTENT_DONE_TTL_SECONDS = 7 * 24 * 3600L;

    private final SeckillOrderService orderService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 消费正常秒杀下单消息。
     *
     * <p>处理顺序：</p>
     * <ol>
     *     <li>校验 messageId；</li>
     *     <li>抢占 Redis 幂等锁；</li>
     *     <li>执行下单服务；</li>
     *     <li>成功后写 DONE 并手动 ACK；</li>
     *     <li>失败删除幂等锁并抛出异常给重试拦截器。</li>
     * </ol>
     */
    @RabbitListener(queues = RabbitMqConfig.SECKILL_ORDER_QUEUE, containerFactory = "seckillListenerContainerFactory")
    public void consumeSeckillOrder(SeckillMessage message, Channel channel, Message amqpMessage) throws IOException {
        String messageId = message.getMessageId();
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        log.info("seckill order message received, messageId={}, userId={}, productId={}",
                messageId, message.getUserId(), message.getProductId());

        if (messageId == null || messageId.isBlank()) {
            // messageId 缺失时无法幂等，拒绝并让 broker 按队列策略进入死信。
            log.error("收到无messageId消息，直接拒绝入死信 userId={}, productId={}", message.getUserId(), message.getProductId());
            channel.basicReject(deliveryTag, false);
            return;
        }

        String idempotentKey = RedisKeys.mqConsume(messageId);
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "PROCESSING", IDEMPOTENT_PROCESSING_TTL_SECONDS, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(locked)) {
            // 已有消费者处理过该消息（或正在处理），直接 ACK 避免重复执行业务。
            log.info("重复消费消息，已幂等跳过 messageId={}", messageId);
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            orderService.createSeckillOrder(message);
            stringRedisTemplate.opsForValue().set(idempotentKey, "DONE", IDEMPOTENT_DONE_TTL_SECONDS, TimeUnit.SECONDS);
            channel.basicAck(deliveryTag, false);
            log.info("seckill order message consumed successfully, messageId={}", messageId);
        } catch (Exception e) {
            // 删除锁后抛异常，交由重试拦截器；超过最大次数后转入 DLQ。
            stringRedisTemplate.delete(idempotentKey);
            log.error("消费秒杀消息失败，等待重试 messageId={}", messageId, e);
            throw e;
        }
    }

    /**
     * 死信队列消费：执行补偿并标记幂等键为 FAILED。
     */
    @RabbitListener(queues = RabbitMqConfig.SECKILL_ORDER_DLQ)
    public void consumeDeadLetter(SeckillMessage message) {
        log.error("接收到秒杀死信消息 messageId={}, userId={}, productId={}",
                message.getMessageId(), message.getUserId(), message.getProductId());
        orderService.handleSeckillFailure(message);
        log.warn("dead letter compensation finished, messageId={}, userId={}, productId={}",
                message.getMessageId(), message.getUserId(), message.getProductId());
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
