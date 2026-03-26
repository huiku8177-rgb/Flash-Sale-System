package com.flashsale.seckillservice.run;

import com.flashsale.seckillservice.service.SeckillProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "seckill.preload.enabled", havingValue = "true")
@Slf4j
public class StockPreheatRunner implements CommandLineRunner {

    private final SeckillProductService stockPreheatService;


    /**
     * 秒杀库存预热
     */
    @Override
    public void run(String... args) {
        log.info("seckill stock preheat started");
        stockPreheatService.loadStockToRedis();
        log.info("seckill stock preheat finished");
    }
}
