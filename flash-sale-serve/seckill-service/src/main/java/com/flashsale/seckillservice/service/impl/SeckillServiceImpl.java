package com.flashsale.seckillservice.service.impl;

import com.flashsale.common.domain.Result;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.common.redis.SeckillResultState;
import com.flashsale.seckillservice.config.SeckillBusinessProperties;
import com.flashsale.seckillservice.domain.dto.SeckillRequestDTO;
import com.flashsale.seckillservice.domain.po.SeckillOrderPO;
import com.flashsale.seckillservice.domain.po.SeckillProductPO;
import com.flashsale.seckillservice.domain.vo.SeckillResultVO;
import com.flashsale.seckillservice.domain.vo.SeckillStatusVO;
import com.flashsale.seckillservice.mapper.SeckillMapper;
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
 * @date 2026/3/20 00:00
 */
@Service
@Slf4j
public class SeckillServiceImpl implements SeckillService {

    private static final int STATUS_CREATED = 0;
    private static final int STATUS_PAID = 1;
    private static final int STATUS_CANCELLED = 2;

    private final SeckillProductMapper seckillProductMapper;
    private final SeckillMapper seckillMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> seckillScript;
    private final DefaultRedisScript<Long> seckillRollbackScript;
    private final SeckillProducer seckillProducer;
    private final SeckillBusinessProperties seckillBusinessProperties;

    public SeckillServiceImpl(
            SeckillProductMapper seckillProductMapper,
            SeckillMapper seckillMapper,
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("seckillScript") DefaultRedisScript<Long> seckillScript,
            @Qualifier("seckillRollbackScript") DefaultRedisScript<Long> seckillRollbackScript,
            SeckillProducer seckillProducer,
            SeckillBusinessProperties seckillBusinessProperties) {
        this.seckillProductMapper = seckillProductMapper;
        this.seckillMapper = seckillMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.seckillScript = seckillScript;
        this.seckillRollbackScript = seckillRollbackScript;
        this.seckillProducer = seckillProducer;
        this.seckillBusinessProperties = seckillBusinessProperties;
    }

    /**
     * 发起秒杀请求时，先通过 Redis + Lua 做原子校验和预扣，再把真正建单交给 MQ 异步处理。
     */
    @Override
    public Result<SeckillResultVO> seckill(SeckillRequestDTO requestDTO) {
        if (requestDTO == null) {
            SeckillResultVO result = new SeckillResultVO();
            result.setSuccess(false);
            result.setMessage("请求参数错误");
            return Result.success(result);
        }

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
        stringRedisTemplate.opsForValue().set(resultKey, SeckillResultState.PROCESSING, ttlSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.expire(userKey, ttlSeconds, TimeUnit.SECONDS);

        SeckillMessage message = new SeckillMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setUserId(userId);
        message.setProductId(productId);
        message.setSeckillPrice(product.getSeckillPrice());
        message.setCreateTime(now);
        message.setExpireAt(now.plusSeconds(ttlSeconds));

        try {
            seckillProducer.sendSeckillMessage(message);
        } catch (Exception ex) {
            stringRedisTemplate.execute(
                    seckillRollbackScript,
                    Arrays.asList(stockKey, userKey),
                    String.valueOf(userId)
            );
            stringRedisTemplate.opsForValue().set(
                    resultKey,
                    SeckillResultState.FAIL,
                    seckillBusinessProperties.getDefaultResultTtlSeconds(),
                    TimeUnit.SECONDS
            );
            log.error("发送秒杀 MQ 失败，已回滚 Redis 库存与用户标记，messageId={}", message.getMessageId(), ex);
            result.setSuccess(false);
            result.setMessage("系统繁忙，请稍后重试");
            return Result.success(result);
        }

        result.setSuccess(true);
        result.setMessage("秒杀成功，订单处理中");
        return Result.success(result);
    }

    /**
     * 查询秒杀结果时会优先参考 Redis，但真正返回给前端前会以数据库订单状态为准，
     * 避免旧订单继续被伪装成“本次新抢购成功”。
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
            SeckillOrderPO order = seckillMapper.getOrderByUserIdAndProductId(userId, productId);
            if (order != null) {
                return buildOrderResult(result, order);
            }
            result.setStatus(0);
            result.setMessage("秒杀订单处理中，请稍后刷新结果");
            return Result.success(result);
        }

        String resultKey = RedisKeys.seckillResult(userId, productId);
        String status = stringRedisTemplate.opsForValue().get(resultKey);
        if (SeckillResultState.PROCESSING.equals(status)) {
            result.setStatus(0);
            result.setMessage("排队中");
            return Result.success(result);
        }
        if (SeckillResultState.FAIL.equals(status)) {
            result.setStatus(-1);
            result.setMessage("秒杀失败");
            return Result.success(result);
        }
        if (SeckillResultState.ALREADY_PAID.equals(status)) {
            result.setStatus(-1);
            result.setMessage("你已成功购买过该秒杀商品，请勿重复抢购");
            return Result.success(result);
        }
        if (SeckillResultState.CANCELLED.equals(status)) {
            result.setStatus(-1);
            result.setMessage("该秒杀订单已取消，当前不能重复抢购");
            return Result.success(result);
        }
        if (SeckillResultState.PENDING_PAYMENT.equals(status)) {
            SeckillOrderPO order = seckillMapper.getOrderByUserIdAndProductId(userId, productId);
            if (order != null) {
                return buildOrderResult(result, order);
            }
            result.setStatus(0);
            result.setMessage("你已有待支付秒杀订单，请前往订单中心继续支付");
            return Result.success(result);
        }

        String userKey = RedisKeys.seckillUser(productId);
        Boolean exists = stringRedisTemplate.opsForSet().isMember(userKey, String.valueOf(userId));
        if (Boolean.TRUE.equals(exists)) {
            result.setStatus(0);
            result.setMessage("排队中");
            return Result.success(result);
        }

        SeckillOrderPO order = seckillMapper.getOrderByUserIdAndProductId(userId, productId);
        if (order != null) {
            cacheOrderResult(orderKey, resultKey, order);
            return buildOrderResult(result, order);
        }

        result.setStatus(-1);
        result.setMessage("秒杀失败");
        return Result.success(result);
    }

    private void cacheOrderResult(String orderKey, String resultKey, SeckillOrderPO order) {
        if (order == null) {
            return;
        }
        if (isCreated(order.getStatus())) {
            stringRedisTemplate.opsForValue().set(
                    orderKey,
                    String.valueOf(order.getId()),
                    seckillBusinessProperties.getDefaultResultTtlSeconds(),
                    TimeUnit.SECONDS
            );
            stringRedisTemplate.opsForValue().set(
                    resultKey,
                    SeckillResultState.PENDING_PAYMENT,
                    seckillBusinessProperties.getDefaultResultTtlSeconds(),
                    TimeUnit.SECONDS
            );
            return;
        }

        stringRedisTemplate.delete(orderKey);
        if (isPaid(order.getStatus())) {
            stringRedisTemplate.opsForValue().set(
                    resultKey,
                    SeckillResultState.ALREADY_PAID,
                    seckillBusinessProperties.getDefaultResultTtlSeconds(),
                    TimeUnit.SECONDS
            );
            return;
        }
        if (isCancelled(order.getStatus())) {
            stringRedisTemplate.opsForValue().set(
                    resultKey,
                    SeckillResultState.CANCELLED,
                    seckillBusinessProperties.getDefaultResultTtlSeconds(),
                    TimeUnit.SECONDS
            );
            return;
        }
        stringRedisTemplate.opsForValue().set(
                resultKey,
                SeckillResultState.FAIL,
                seckillBusinessProperties.getDefaultResultTtlSeconds(),
                TimeUnit.SECONDS
        );
    }

    private Result<SeckillStatusVO> buildOrderResult(SeckillStatusVO result, SeckillOrderPO order) {
        if (order == null) {
            result.setStatus(-1);
            result.setMessage("秒杀失败");
            return Result.success(result);
        }

        result.setOrderId(order.getId());
        result.setOrderNo(order.getOrderNo());
        if (isCreated(order.getStatus())) {
            result.setStatus(1);
            result.setMessage("你已有待支付秒杀订单，请前往订单中心继续支付");
            return Result.success(result);
        }
        if (isPaid(order.getStatus())) {
            result.setStatus(-1);
            result.setMessage("你已成功购买过该秒杀商品，请勿重复抢购");
            return Result.success(result);
        }
        if (isCancelled(order.getStatus())) {
            result.setStatus(-1);
            result.setMessage("该秒杀订单已取消，当前不能重复抢购");
            return Result.success(result);
        }

        result.setStatus(-1);
        result.setMessage("秒杀失败");
        return Result.success(result);
    }

    private long calculateResultTtlSeconds(LocalDateTime endTime, LocalDateTime now) {
        if (endTime == null) {
            return seckillBusinessProperties.getDefaultResultTtlSeconds();
        }
        LocalDateTime expireTime = endTime.plusSeconds(seckillBusinessProperties.getResultBufferSeconds());
        long ttl = Duration.between(
                now.atZone(ZoneId.systemDefault()).toInstant(),
                expireTime.atZone(ZoneId.systemDefault()).toInstant()
        ).getSeconds();
        return Math.max(ttl, seckillBusinessProperties.getDefaultResultTtlSeconds());
    }

    private boolean isCreated(Integer status) {
        return status != null && status == STATUS_CREATED;
    }

    private boolean isPaid(Integer status) {
        return status != null && status == STATUS_PAID;
    }

    private boolean isCancelled(Integer status) {
        return status != null && status == STATUS_CANCELLED;
    }
}
