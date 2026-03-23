package com.flashsale.orderservice.task;

import com.flashsale.orderservice.service.NormalOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author strive_qin
 * @version 1.0
 * @description NormalOrderTimeoutTask
 * @date 2026/3/23 00:00
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NormalOrderTimeoutTask {

    private final NormalOrderService normalOrderService;

    /**
     * 固定频率扫描超时待支付订单，复用正常取消订单逻辑做库存回补和状态流转。
     */
    @Scheduled(initialDelay = 60000L, fixedDelay = 60000L)
    public void cancelExpiredUnpaidOrders() {
        int cancelled = normalOrderService.cancelTimeoutOrders();
        if (cancelled > 0) {
            log.info("本次超时取消普通订单数量={}", cancelled);
        }
    }
}
