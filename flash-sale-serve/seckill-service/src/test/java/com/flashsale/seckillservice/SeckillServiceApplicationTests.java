package com.flashsale.seckillservice;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillServiceApplicationTests
 * @date 2026/3/20 00:00
 */


@SpringBootTest

class SeckillServiceApplicationTests {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Test
    void testRedisConnection() {
        // 测试连接是否正常
        stringRedisTemplate.opsForValue().set("test:key", "Hello Redis");

        String value = stringRedisTemplate.opsForValue().get("test:key");
        System.out.println("从 Redis 中获取的值：" + value);
        rabbitTemplate.convertAndSend("test_queue_temp", "hello_rabbitmq");
        System.out.println("RabbitMQ连接状态: 消息发送成功 (请去网页端查看是否多了个队列)");

        assert "Hello Redis".equals(value) : "Redis 测试失败";
    }


}
