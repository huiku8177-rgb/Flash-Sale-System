package com.flashsale.seckillservice.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Redis Lua 脚本配置
 *
 * @author strive_qin
 * @version 1.0
 * @description RedisLuaConfig
 * @date 2026/3/16 12:07
 */
@Configuration
public class RedisLuaConfig {

    // 秒杀预扣库存与防重脚本
    @Bean
    public DefaultRedisScript<Long> seckillScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("lua/seckill.lua"));
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    // MQ 发送失败后的 Redis 回滚脚本
    @Bean
    public DefaultRedisScript<Long> seckillRollbackScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("lua/seckill_rollback.lua"));
        redisScript.setResultType(Long.class);
        return redisScript;
    }
}
