package com.flashsale.seckillservice.service.impl;

import com.flashsale.common.domain.Result;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.seckillservice.domain.dto.SeckillRequestDTO;
import com.flashsale.seckillservice.domain.po.SeckillProductPO;
import com.flashsale.seckillservice.domain.vo.SeckillResultVO;
import com.flashsale.seckillservice.domain.vo.SeckillStatusVO;
import com.flashsale.seckillservice.mapper.SeckillProductMapper;
import com.flashsale.seckillservice.mq.SeckillProducer;
import com.flashsale.seckillservice.mq.message.SeckillMessage;
import com.flashsale.seckillservice.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillServiceImpl
 * @date 2026/3/13 17:00
 */
@Service
@Slf4j
public class SeckillServiceImpl implements SeckillService {

    /** 活动结束后额外保留结果缓存的缓冲时长（秒）。 */
    private static final long RESULT_BUFFER_SECONDS = 600L;
    /** 结果缓存兜底 TTL（秒）。 */
    private static final long DEFAULT_RESULT_TTL_SECONDS = 3600L;

    private final SeckillProductMapper seckillProductMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> seckillScript;
    private final DefaultRedisScript<Long> seckillRollbackScript;
    private final SeckillProducer seckillProducer;

    public SeckillServiceImpl(
            SeckillProductMapper seckillProductMapper,
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("seckillScript") DefaultRedisScript<Long> seckillScript,
            @Qualifier("seckillRollbackScript") DefaultRedisScript<Long> seckillRollbackScript,
            SeckillProducer seckillProducer) {
        this.seckillProductMapper = seckillProductMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.seckillScript = seckillScript;
        this.seckillRollbackScript = seckillRollbackScript;
        this.seckillProducer = seckillProducer;
    }

    /**
     * 秒杀入口：完成参数/活动校验、Lua 原子扣减、写入 PROCESSING 状态并异步投递 MQ。
     */
    @Override
    public Result<SeckillResultVO> seckill(SeckillRequestDTO requestDTO) {
        Long productId = requestDTO.getProductId();
        Long userId = requestDTO.getUserId();
        SeckillResultVO result = new SeckillResultVO();
        result.setProductId(productId);

        if (productId == null || userId == null) {
            result.setSuccess(false);
            result.setMessage("请求参数错误");
            return Result.success(result);
        }

        SeckillProductPO product = seckillProductMapper.getById(productId);
        if (product == null) {
            result.setSuccess(false);
            result.setMessage("商品不存在");
            return Result.success(result);
        }

        if (product.getStatus() == null || product.getStatus() != 1) {
            result.setSuccess(false);
            result.setMessage("商品已下架");
            return Result.success(result);
        }

        LocalDateTime now = LocalDateTime.now();
        if (product.getStartTime() != null && now.isBefore(product.getStartTime())) {
            result.setSuccess(false);
            result.setMessage("秒杀尚未开始");
            return Result.success(result);
        }

        if (product.getEndTime() != null && now.isAfter(product.getEndTime())) {
            result.setSuccess(false);
            result.setMessage("秒杀已结束");
            return Result.success(result);
        }

        String stockKey = RedisKeys.seckillStock(productId);
        String userKey = RedisKeys.seckillUser(productId);

        Long luaResult = stringRedisTemplate.execute(
                seckillScript,
                Arrays.asList(stockKey, userKey),
                String.valueOf(userId)
        );

        if (luaResult == null) {
            result.setSuccess(false);
            result.setMessage("系统繁忙，请稍后重试");
            return Result.success(result);
        }

        if (luaResult == 0L) {
            result.setSuccess(false);
            result.setMessage("库存不足");
            return Result.success(result);
        }

        if (luaResult == 2L) {
            result.setSuccess(false);
            result.setMessage("请勿重复秒杀");
            return Result.success(result);
        }

        long ttlSeconds = calculateResultTtlSeconds(product.getEndTime(), now);
        String resultKey = RedisKeys.seckillResult(userId, productId);
        stringRedisTemplate.opsForValue().set(resultKey, "PROCESSING", ttlSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.expire(userKey, ttlSeconds, TimeUnit.SECONDS);

        SeckillMessage sm = new SeckillMessage();
        sm.setMessageId(UUID.randomUUID().toString());
        sm.setUserId(userId);
        sm.setProductId(productId);
        sm.setSeckillPrice(product.getSeckillPrice());
        sm.setCreateTime(now);
        sm.setExpireAt(now.plusSeconds(ttlSeconds));

        try {
            seckillProducer.sendSeckillMessage(sm);
        } catch (Exception e) {
            stringRedisTemplate.execute(
                    seckillRollbackScript,
                    Arrays.asList(stockKey, userKey),
                    String.valueOf(userId)
            );
            stringRedisTemplate.opsForValue().set(resultKey, "FAIL", DEFAULT_RESULT_TTL_SECONDS, TimeUnit.SECONDS);
            log.error("发送秒杀MQ失败，已回滚Redis库存和用户标记，messageId={}", sm.getMessageId(), e);
            result.setSuccess(false);
            result.setMessage("系统繁忙，请稍后重试");
            return Result.success(result);
        }

        result.setSuccess(true);
        result.setMessage("秒杀成功，订单处理中");
        return Result.success(result);
    }

    /**
     * 获取秒杀结果
     */
    @Override
    public Result<SeckillStatusVO> getSeckillResult(Long userId, Long productId) {
        SeckillStatusVO result = new SeckillStatusVO();

        if (userId == null || productId == null) {
            result.setStatus(-1);
            result.setMessage("请求参数错误");
            return Result.success(result);
        }

        String orderKey = RedisKeys.seckillOrder(userId, productId);
        String orderIdStr = stringRedisTemplate.opsForValue().get(orderKey);

        if (orderIdStr != null) {
            result.setStatus(1);
            result.setMessage("秒杀成功");
            result.setOrderId(Long.valueOf(orderIdStr));
            return Result.success(result);
        }

        String resultKey = RedisKeys.seckillResult(userId, productId);
        String status = stringRedisTemplate.opsForValue().get(resultKey);
        if ("PROCESSING".equals(status)) {
            result.setStatus(0);
            result.setMessage("排队中");
            return Result.success(result);
        }

        if ("FAIL".equals(status)) {
            result.setStatus(-1);
            result.setMessage("秒杀失败");
            return Result.success(result);
        }

        String userKey = RedisKeys.seckillUser(productId);
        Boolean exists = stringRedisTemplate.opsForSet().isMember(userKey, String.valueOf(userId));
        if (Boolean.TRUE.equals(exists)) {
            result.setStatus(0);
            result.setMessage("排队中");
            return Result.success(result);
        }

        result.setStatus(-1);
        result.setMessage("秒杀失败");
        return Result.success(result);
    }

    private long calculateResultTtlSeconds(LocalDateTime endTime, LocalDateTime now) {
        if (endTime == null) {
            return DEFAULT_RESULT_TTL_SECONDS;
        }
        LocalDateTime expireTime = endTime.plusSeconds(RESULT_BUFFER_SECONDS);
        long ttl = Duration.between(
                now.atZone(ZoneId.systemDefault()).toInstant(),
                expireTime.atZone(ZoneId.systemDefault()).toInstant()
        ).getSeconds();
        return Math.max(ttl, DEFAULT_RESULT_TTL_SECONDS);
    }
}
