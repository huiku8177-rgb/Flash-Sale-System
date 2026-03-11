package com.flashsale.orderservice;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest

class OrderServiceApplicationTests {
    @Autowired
    private  StringRedisTemplate stringRedisTemplate;

    @Test
    void testRedisConnection() {
        // 测试连接是否正常
        stringRedisTemplate.opsForValue().set("test:key", "Hello Redis");

        String value = stringRedisTemplate.opsForValue().get("test:key");
        System.out.println("从 Redis 中获取的值：" + value);

        assert "Hello Redis".equals(value) : "Redis 测试失败";
    }

}
