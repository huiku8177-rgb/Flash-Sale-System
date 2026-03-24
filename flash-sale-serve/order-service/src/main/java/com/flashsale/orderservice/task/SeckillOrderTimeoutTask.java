package com.flashsale.orderservice.task;

import com.flashsale.orderservice.service.SeckillOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillOrderTimeoutTask
 * @date 2026/3/24 00:00
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderTimeoutTask {

    private final SeckillOrderService seckillOrderService;

    /**
     * 秒杀订单与普通订单统一采用“15 分钟未支付自动关闭”的策略。
     */
    @Scheduled(initialDelay = 60000L, fixedDelay = 60000L)
    public void cancelExpiredUnpaidSeckillOrders() {
        int cancelled = seckillOrderService.cancelTimeoutOrders();
        if (cancelled > 0) {
            log.info("本次超时取消秒杀订单数量 {}", cancelled);
        }
    }
}
