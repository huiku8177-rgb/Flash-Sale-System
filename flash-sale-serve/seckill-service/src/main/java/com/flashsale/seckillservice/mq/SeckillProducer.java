package com.flashsale.seckillservice.mq;

import com.flashsale.seckillservice.mq.message.SeckillMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillProducer
 * @date 2026/3/16 12:31
 */
@Component
@RequiredArgsConstructor
public class SeckillProducer {
    private final RabbitTemplate rabbitTemplate;

    public void sendSeckillMessage(SeckillMessage seckillMessage) {
        rabbitTemplate.convertAndSend(
                "seckill.exchange",
                "seckill.order",
                seckillMessage
        );
    }
}
