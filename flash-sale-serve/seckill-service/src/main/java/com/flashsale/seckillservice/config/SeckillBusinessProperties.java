package com.flashsale.seckillservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "flash-sale.seckill")
public class SeckillBusinessProperties {

    private long resultBufferSeconds = 600L;
    private long defaultResultTtlSeconds = 3600L;
    private final Mq mq = new Mq();
    private final Cache cache = new Cache();

    @Getter
    @Setter
    public static class Mq {
        private String exchange = "seckill.exchange";
        private String routingKey = "seckill.order";
    }

    @Getter
    @Setter
    public static class Cache {
        private long listTtlSeconds = 30L;
        private long detailTtlSeconds = 30L;
    }
}
