package com.flashsale.seckillservice.run;

import com.flashsale.seckillservice.service.SeckillProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 应用启动后按配置决定是否执行秒杀库存预热
 *
 * @author strive_qin
 * @version 1.0
 * @description StockPreHeatRunner
 * @date 2026/3/16 12:17
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "seckill.preload.enabled", havingValue = "true")
public class StockPreheatRunner implements CommandLineRunner {

    private final SeckillProductService stockPreheatService;

    // 启动后触发秒杀库存预热
    @Override
    public void run(String... args) {
        stockPreheatService.loadStockToRedis();
    }
}
