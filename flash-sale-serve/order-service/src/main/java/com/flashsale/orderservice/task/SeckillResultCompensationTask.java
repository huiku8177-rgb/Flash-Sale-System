package com.flashsale.orderservice.task;

import com.flashsale.common.redis.RedisKeys;
import com.flashsale.common.redis.SeckillResultState;
import com.flashsale.orderservice.domain.po.SeckillOrderPO;
import com.flashsale.orderservice.mapper.SeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 超时排队结果补偿任务：避免用户结果无限排队中。
 */
/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillResultCompensationTask
 * @date 2026/3/20 00:00
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class SeckillResultCompensationTask {

    private static final long RESULT_FAIL_TTL_SECONDS = 3600L;

    private final StringRedisTemplate stringRedisTemplate;
    private final SeckillOrderMapper seckillOrderMapper;

    /**
     * 定时扫描接近过期的 PROCESSING 状态：
     * 1) DB已有订单 -> 修正为 SUCCESS；
     * 2) DB无订单 -> 回滚Redis用户标记/库存并置 FAIL。
     */
    @Scheduled(fixedDelayString = "${flash-sale.order.tasks.seckill-result-compensation.fixed-delay-ms:30000}")
    public void compensateTimeoutProcessingResult() {
        ScanOptions options = ScanOptions.scanOptions().match(RedisKeys.PREFIX_SECKILL_RESULT + "*").count(200).build();
        try (Cursor<byte[]> cursor = stringRedisTemplate.getConnectionFactory().getConnection().scan(options)) {
            while (cursor.hasNext()) {
                String resultKey = new String(cursor.next(), StandardCharsets.UTF_8);
                String state = stringRedisTemplate.opsForValue().get(resultKey);
                if (!SeckillResultState.PROCESSING.equals(state)) {
                    continue;
                }

                Long ttl = stringRedisTemplate.getExpire(resultKey, TimeUnit.SECONDS);
                if (ttl == null || ttl > 60) {
                    continue;
                }

                String[] split = resultKey.split(":");
                if (split.length < 4) {
                    continue;
                }

                Long userId = Long.valueOf(split[2]);
                Long productId = Long.valueOf(split[3]);

                SeckillOrderPO order = seckillOrderMapper.getByUserIdAndProductId(userId, productId);
                if (order != null) {
                    stringRedisTemplate.opsForValue().set(
                            RedisKeys.seckillOrder(userId, productId),
                            String.valueOf(order.getId()),
                            RESULT_FAIL_TTL_SECONDS,
                            TimeUnit.SECONDS
                    );
                    stringRedisTemplate.opsForValue().set(resultKey, SeckillResultState.SUCCESS, RESULT_FAIL_TTL_SECONDS, TimeUnit.SECONDS);
                    continue;
                }

                String userKey = RedisKeys.seckillUser(productId);
                Boolean userMarked = stringRedisTemplate.opsForSet().isMember(userKey, String.valueOf(userId));
                if (Boolean.TRUE.equals(userMarked)) {
                    stringRedisTemplate.opsForSet().remove(userKey, String.valueOf(userId));
                    stringRedisTemplate.opsForValue().increment(RedisKeys.seckillStock(productId));
                }

                stringRedisTemplate.opsForValue().set(resultKey, SeckillResultState.FAIL, RESULT_FAIL_TTL_SECONDS, TimeUnit.SECONDS);
                log.warn("排队超时补偿完成 userId={}, productId={}, resultKey={}", userId, productId, resultKey);
            }
        } catch (Exception e) {
            log.error("执行秒杀结果补偿任务失败", e);
        }
    }
}
