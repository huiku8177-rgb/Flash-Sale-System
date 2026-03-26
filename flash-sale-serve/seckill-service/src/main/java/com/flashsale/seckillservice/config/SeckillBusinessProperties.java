package com.flashsale.seckillservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 秒杀业务参数，统一从配置读取，减少硬编码。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "flash-sale.seckill")
public class SeckillBusinessProperties {

    /**
     * 秒杀结束后继续保留结果缓存的缓冲时间。
     */
    private long resultBufferSeconds = 600L;

    /**
     * 秒杀结果缓存的最小 TTL。
     */
    private long defaultResultTtlSeconds = 3600L;

    private final Mq mq = new Mq();

    @Getter
    @Setter
    public static class Mq {
        private String exchange = "seckill.exchange";
        private String routingKey = "seckill.order";
    }
}
