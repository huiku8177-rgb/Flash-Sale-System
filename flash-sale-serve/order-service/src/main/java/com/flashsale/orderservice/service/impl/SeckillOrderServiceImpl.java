package com.flashsale.orderservice.service.impl;

import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.orderservice.domain.po.SeckillOrderPO;
import com.flashsale.orderservice.domain.vo.SeckillOrderPayStatusVO;
import com.flashsale.orderservice.domain.vo.SeckillOrderVO;
import com.flashsale.orderservice.mapper.SeckillOrderMapper;
import com.flashsale.orderservice.mapper.SeckillProductMapper;
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
 * @date 2026/3/20 00:00
 */


@Service
@RequiredArgsConstructor
@Slf4j
public class SeckillOrderServiceImpl implements SeckillOrderService {

    private static final long DEFAULT_RESULT_TTL_SECONDS = 3600L;
    private static final int STATUS_CREATED = 0;
    private static final int STATUS_PAID = 1;

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillProductMapper seckillProductMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 查询当前用户的秒杀订单列表
     *
     * @param userId 用户ID
     * @return 秒杀订单列表
     */
    @Override
    public Result<List<SeckillOrderVO>> listOrders(Long userId) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        return Result.success(seckillOrderMapper.listOrders(userId));
    }

    /**
     * 查询秒杀订单详情
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 订单详情
     */
    @Override
    public Result<SeckillOrderVO> getOrderDetail(Long userId, Long id) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        SeckillOrderVO order = seckillOrderMapper.getOrderDetail(userId, id);
        if (order == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 模拟支付秒杀订单
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 支付后的订单详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SeckillOrderVO> mockPay(Long userId, Long id) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR, "订单ID不能为空");
        }

        SeckillOrderVO order = seckillOrderMapper.getOrderDetail(userId, id);
        if (order == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "订单不存在");
        }
        if (order.getStatus() != null && order.getStatus() == STATUS_PAID) {
            return Result.success(order);
        }
        if (order.getStatus() != null && order.getStatus() != STATUS_CREATED) {
            return Result.error(ResultCode.BUSINESS_ERROR, "当前秒杀订单状态不允许支付");
        }

        // 仅允许待支付订单流转到已支付状态
        int updated = seckillOrderMapper.updateStatus(id, userId, STATUS_CREATED, STATUS_PAID);
        if (updated <= 0) {
            return Result.error(ResultCode.BUSINESS_ERROR, "秒杀订单支付失败，请刷新后重试");
        }
        return Result.success(seckillOrderMapper.getOrderDetail(userId, id));
    }

    /**
     * 查询秒杀订单支付状态
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 支付状态
     */
    @Override
    public Result<SeckillOrderPayStatusVO> getPayStatus(Long userId, Long id) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR, "订单ID不能为空");
        }

        SeckillOrderVO order = seckillOrderMapper.getOrderDetail(userId, id);
        if (order == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "订单不存在");
        }

        SeckillOrderPayStatusVO payStatusVO = new SeckillOrderPayStatusVO();
        payStatusVO.setOrderId(order.getId());
        payStatusVO.setProductId(order.getProductId());
        payStatusVO.setStatus(order.getStatus());
        payStatusVO.setSeckillPrice(order.getSeckillPrice());
        boolean paid = order.getStatus() != null && order.getStatus() == STATUS_PAID;
        payStatusVO.setPaid(paid);
        payStatusVO.setMessage(paid ? "秒杀订单已支付" : "秒杀订单待支付");
        return Result.success(payStatusVO);
    }

    /**
     * 消费秒杀消息并创建订单
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
            // 先扣减数据库库存，再创建秒杀订单
            int updated = seckillProductMapper.decreaseStock(productId);
            if (updated <= 0) {
                throw new RuntimeException("扣减数据库库存失败");
            }

            SeckillOrderPO order = new SeckillOrderPO();
            order.setUserId(userId);
            order.setProductId(productId);
            order.setSeckillPrice(message.getSeckillPrice());
            order.setStatus(STATUS_CREATED);
            seckillOrderMapper.insert(order);

            stringRedisTemplate.opsForValue().set(orderKey, String.valueOf(order.getId()), ttlSeconds, TimeUnit.SECONDS);
            stringRedisTemplate.opsForValue().set(resultKey, "SUCCESS", ttlSeconds, TimeUnit.SECONDS);
            log.info("创建秒杀订单成功 messageId={}, userId={}, productId={}, orderId={}",
                    message.getMessageId(), userId, productId, order.getId());
        } catch (DuplicateKeyException e) {
            // 利用唯一索引和补查实现消息幂等
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

    /**
     * 处理死信队列中的秒杀失败消息
     *
     * @param message 秒杀消息
     */
    @Override
    public void handleSeckillFailure(SeckillMessage message) {
        Long userId = message.getUserId();
        Long productId = message.getProductId();
        long ttlSeconds = resolveTtlSeconds(message.getExpireAt());

        String orderKey = RedisKeys.seckillOrder(userId, productId);
        String resultKey = RedisKeys.seckillResult(userId, productId);
        String userKey = RedisKeys.seckillUser(productId);
        String stockKey = RedisKeys.seckillStock(productId);

        // 若订单已生成，则无需执行库存和资格补偿
        String orderId = stringRedisTemplate.opsForValue().get(orderKey);
        if (orderId != null) {
            log.warn("死信补偿跳过：订单已存在，messageId={}, userId={}, productId={}, orderId={}",
                    message.getMessageId(), userId, productId, orderId);
            return;
        }

        // 回滚用户秒杀资格、Redis 库存，并记录失败结果
        stringRedisTemplate.opsForSet().remove(userKey, String.valueOf(userId));
        stringRedisTemplate.opsForValue().increment(stockKey);
        stringRedisTemplate.opsForValue().set(resultKey, "FAIL", ttlSeconds, TimeUnit.SECONDS);
        log.error("秒杀消息进入死信并已补偿完成，请关注告警与人工排查。messageId={}, userId={}, productId={}",
                message.getMessageId(), userId, productId);
    }

    // 根据消息过期时间计算秒杀结果的缓存时长
    private long resolveTtlSeconds(LocalDateTime expireAt) {
        if (expireAt == null) {
            return DEFAULT_RESULT_TTL_SECONDS;
        }
        long ttlSeconds = Duration.between(
                LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant(),
                expireAt.atZone(ZoneId.systemDefault()).toInstant()
        ).getSeconds();
        return Math.max(ttlSeconds, DEFAULT_RESULT_TTL_SECONDS);
    }
}
