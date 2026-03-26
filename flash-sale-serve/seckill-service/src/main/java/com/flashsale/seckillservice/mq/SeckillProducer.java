package com.flashsale.seckillservice.mq;

import com.flashsale.common.web.RequestHeaderNames;
import com.flashsale.seckillservice.config.SeckillBusinessProperties;
import com.flashsale.seckillservice.mq.message.SeckillMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 秒杀消息发送器
 *
 * @author strive_qin
 * @version 1.0
 * @description SeckillProducer
 * @date 2026/3/16 12:31
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SeckillProducer {

    private static final String REQUEST_ID_HEADER = RequestHeaderNames.X_REQUEST_ID;

    private final RabbitTemplate rabbitTemplate;
    private final SeckillBusinessProperties seckillBusinessProperties;

    // 注册消息投递确认与退回回调
    @jakarta.annotation.PostConstruct
    public void initCallbacks() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                String id = correlationData == null ? "unknown" : correlationData.getId();
                log.error("秒杀消息投递到Exchange失败 messageId={}, cause={}", id, cause);
            }
        });
        rabbitTemplate.setReturnsCallback(returned ->
                log.error("秒杀消息路由失败 messageId={}, exchange={}, routingKey={}, replyText={}",
                        returned.getMessage().getMessageProperties().getMessageId(),
                        returned.getExchange(),
                        returned.getRoutingKey(),
                        returned.getReplyText())
        );
    }

    // 发送秒杀建单消息到 RabbitMQ
    public void sendSeckillMessage(SeckillMessage seckillMessage) {
        CorrelationData correlationData = new CorrelationData(seckillMessage.getMessageId());
        rabbitTemplate.convertAndSend(
                seckillBusinessProperties.getMq().getExchange(),
                seckillBusinessProperties.getMq().getRoutingKey(),
                seckillMessage,
                (Message message) -> {
                    message.getMessageProperties().setMessageId(seckillMessage.getMessageId());
                    String requestId = MDC.get("requestId");
                    if (requestId != null && !requestId.isBlank()) {
                        message.getMessageProperties().setHeader(REQUEST_ID_HEADER, requestId);
                    }
                    return message;
                },
                correlationData
        );
        log.info("seckill message published, messageId={}, userId={}, productId={}",
                seckillMessage.getMessageId(), seckillMessage.getUserId(), seckillMessage.getProductId());
    }
}
