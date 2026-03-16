package com.flashsale.orderservice.service.impl;

import com.flashsale.common.domain.Result;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.orderservice.domain.po.SeckillOrderPO;
import com.flashsale.orderservice.domain.vo.SeckillOrderVO;
import com.flashsale.orderservice.mapper.ProductMapper;
import com.flashsale.orderservice.mapper.SeckillOrderMapper;
import com.flashsale.orderservice.mq.message.SeckillMessage;
import com.flashsale.orderservice.service.SeckillOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillOrderServiceImpl
 * @date 2026/3/13 17:00
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeckillOrderServiceImpl implements SeckillOrderService {

    private static final long DEFAULT_RESULT_TTL_SECONDS = 3600L;

    private final SeckillOrderMapper seckillOrderMapper;
    private final ProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result<List<SeckillOrderVO>> listOrders() {
        return Result.success(seckillOrderMapper.listOrders());
    }

    @Override
    public Result<SeckillOrderVO> getOrderDetail(Long id) {
        return Result.success(seckillOrderMapper.getOrderDetail(id));
    }

    /**
     * 创建秒杀订单
     *
     * @param message 秒杀消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSeckillOrder(SeckillMessage message) {
        Long productId = message.getProductId();
        Long userId = message.getUserId();
        long ttlSeconds = resolveTtlSeconds(message.getExpireAt());
        String orderKey = RedisKeys.seckillOrder(userId, productId);
        String resultKey = RedisKeys.seckillResult(userId, productId);

        try {
            int updated = productMapper.decreaseStock(productId);
            if (updated <= 0) {
                throw new RuntimeException("扣减数据库库存失败");
            }

            SeckillOrderPO order = new SeckillOrderPO();
            order.setUserId(userId);
            order.setProductId(productId);
            order.setSeckillPrice(message.getSeckillPrice());
            order.setStatus(0);
            seckillOrderMapper.insert(order);

            stringRedisTemplate.opsForValue().set(orderKey, String.valueOf(order.getId()), ttlSeconds, TimeUnit.SECONDS);
            stringRedisTemplate.opsForValue().set(resultKey, "SUCCESS", ttlSeconds, TimeUnit.SECONDS);
            log.info("创建秒杀订单成功 messageId={}, userId={}, productId={}, orderId={}",
                    message.getMessageId(), userId, productId, order.getId());
        } catch (DuplicateKeyException e) {
            SeckillOrderPO existed = seckillOrderMapper.getByUserIdAndProductId(userId, productId);
            if (existed != null) {
                stringRedisTemplate.opsForValue().set(orderKey, String.valueOf(existed.getId()), ttlSeconds, TimeUnit.SECONDS);
                stringRedisTemplate.opsForValue().set(resultKey, "SUCCESS", ttlSeconds, TimeUnit.SECONDS);
                log.warn("重复下单消息幂等处理 messageId={}, userId={}, productId={}, orderId={}",
                        message.getMessageId(), userId, productId, existed.getId());
                return;
            }
            throw e;
        }
    }

    @Override
    public void handleSeckillFailure(SeckillMessage message) {
        Long userId = message.getUserId();
        Long productId = message.getProductId();
        long ttlSeconds = resolveTtlSeconds(message.getExpireAt());

        String orderKey = RedisKeys.seckillOrder(userId, productId);
        String resultKey = RedisKeys.seckillResult(userId, productId);
        String userKey = RedisKeys.seckillUser(productId);
        String stockKey = RedisKeys.seckillStock(productId);

        String orderId = stringRedisTemplate.opsForValue().get(orderKey);
        if (orderId != null) {
            log.warn("死信补偿跳过：订单已存在，messageId={}, userId={}, productId={}, orderId={}",
                    message.getMessageId(), userId, productId, orderId);
            return;
        }

        stringRedisTemplate.opsForSet().remove(userKey, String.valueOf(userId));
        stringRedisTemplate.opsForValue().increment(stockKey);
        stringRedisTemplate.opsForValue().set(resultKey, "FAIL", ttlSeconds, TimeUnit.SECONDS);
        log.error("秒杀消息进入死信并已补偿完成，请关注告警与人工排查。messageId={}, userId={}, productId={}",
                message.getMessageId(), userId, productId);
    }

    private long resolveTtlSeconds(LocalDateTime expireAt) {
        if (expireAt == null) {
            return DEFAULT_RESULT_TTL_SECONDS;
        }
        long ttlSeconds = Duration.between(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant(),
                expireAt.atZone(ZoneId.systemDefault()).toInstant()).getSeconds();
        return Math.max(ttlSeconds, DEFAULT_RESULT_TTL_SECONDS);
    }
}
