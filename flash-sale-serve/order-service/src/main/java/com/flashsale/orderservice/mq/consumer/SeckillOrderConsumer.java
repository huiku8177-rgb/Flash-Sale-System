package com.flashsale.orderservice.mq.consumer;

import com.flashsale.orderservice.mq.message.SeckillMessage;
import com.flashsale.orderservice.service.SeckillOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

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

    private final SeckillOrderService orderService;

    @RabbitListener(queues = "seckill.order.queue")
    public void consumeSeckillOrder(SeckillMessage message) {
        log.info("接收到秒杀消息: userId={}, productId={}, seckillPrice={}",
                message.getUserId(),
                message.getProductId(),
                message.getSeckillPrice());

        orderService.createSeckillOrder(message);
    }
}
