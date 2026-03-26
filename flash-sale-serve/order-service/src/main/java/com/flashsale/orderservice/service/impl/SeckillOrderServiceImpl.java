package com.flashsale.orderservice.service.impl;

import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.common.redis.SeckillResultState;
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
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
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
    private static final int STATUS_CANCELLED = 2;
    private static final int TIMEOUT_MINUTES = 15;
    private static final int TIMEOUT_SCAN_LIMIT = 100;
    private static final String CANCEL_REASON_MANUAL = "用户主动取消";
    private static final String CANCEL_REASON_TIMEOUT = "超时自动取消";

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillProductMapper seckillProductMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result<List<SeckillOrderVO>> listOrders(Long userId) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        return Result.success(seckillOrderMapper.listOrders(userId));
    }

    @Override
    public Result<SeckillOrderVO> getOrderDetail(Long userId, Long id) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR, "订单ID不能为空");
        }

        SeckillOrderVO order = seckillOrderMapper.getOrderDetail(userId, id);
        if (order == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "秒杀订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 秒杀订单支付只允许从待支付状态流转，避免重复点击带来脏写。
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
            return Result.error(ResultCode.BUSINESS_ERROR, "秒杀订单不存在");
        }
        if (isPaid(order.getStatus())) {
            return Result.success(order);
        }
        if (isCancelled(order.getStatus())) {
            return Result.error(ResultCode.BUSINESS_ERROR, "已取消订单不能支付");
        }
        if (!isCreated(order.getStatus())) {
            return Result.error(ResultCode.BUSINESS_ERROR, "当前秒杀订单状态不允许支付");
        }

        LocalDateTime payTime = LocalDateTime.now();
        int updated = seckillOrderMapper.updatePayStatus(id, userId, STATUS_CREATED, STATUS_PAID, payTime);
        if (updated <= 0) {
            SeckillOrderVO latestOrder = seckillOrderMapper.getOrderDetail(userId, id);
            if (latestOrder != null) {
                if (isPaid(latestOrder.getStatus())) {
                    return Result.success(latestOrder);
                }
                if (isCancelled(latestOrder.getStatus())) {
                    return Result.error(ResultCode.BUSINESS_ERROR, "秒杀订单已取消，无法继续支付");
                }
            }
            return Result.error(ResultCode.BUSINESS_ERROR, "秒杀订单支付失败，请刷新后重试");
        }
        return Result.success(seckillOrderMapper.getOrderDetail(userId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SeckillOrderVO> cancelOrder(Long userId, Long id) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR, "订单ID不能为空");
        }
        return doCancelOrder(userId, id, CANCEL_REASON_MANUAL);
    }

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
            return Result.error(ResultCode.BUSINESS_ERROR, "秒杀订单不存在");
        }

        SeckillOrderPayStatusVO payStatusVO = new SeckillOrderPayStatusVO();
        payStatusVO.setOrderId(order.getId());
        payStatusVO.setOrderNo(order.getOrderNo());
        payStatusVO.setProductId(order.getProductId());
        payStatusVO.setStatus(order.getStatus());
        payStatusVO.setSeckillPrice(order.getSeckillPrice());
        payStatusVO.setPayTime(order.getPayTime());
        payStatusVO.setCancelReason(order.getCancelReason());
        payStatusVO.setCancelTime(order.getCancelTime());

        boolean paid = isPaid(order.getStatus());
        payStatusVO.setPaid(paid);
        if (isCancelled(order.getStatus())) {
            payStatusVO.setMessage(buildCancelledMessage(order));
        } else {
            payStatusVO.setMessage(paid ? "秒杀订单已支付" : "秒杀订单待支付");
        }
        return Result.success(payStatusVO);
    }

    @Override
    public int cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        List<SeckillOrderPO> timeoutOrders = seckillOrderMapper.listTimeoutOrders(deadline, STATUS_CREATED, TIMEOUT_SCAN_LIMIT);
        if (CollectionUtils.isEmpty(timeoutOrders)) {
            return 0;
        }

        int cancelled = 0;
        for (SeckillOrderPO timeoutOrder : timeoutOrders) {
            if (timeoutOrder == null || timeoutOrder.getUserId() == null || timeoutOrder.getId() == null) {
                continue;
            }
            Result<SeckillOrderVO> cancelResult = doCancelOrder(timeoutOrder.getUserId(), timeoutOrder.getId(), CANCEL_REASON_TIMEOUT);
            if (cancelResult.getCode() == ResultCode.SUCCESS.getCode()
                    && cancelResult.getData() != null
                    && isCancelled(cancelResult.getData().getStatus())) {
                cancelled++;
            }
        }
        return cancelled;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSeckillOrder(SeckillMessage message) {
        Long productId = message.getProductId();
        Long userId = message.getUserId();
        long ttlSeconds = resolveTtlSeconds(message.getExpireAt());
        String orderKey = RedisKeys.seckillOrder(userId, productId);
        String resultKey = RedisKeys.seckillResult(userId, productId);

        try {
            // 先扣减数据库库存，再创建待支付秒杀订单，确保数据库状态先落地。
            int updated = seckillProductMapper.decreaseStock(productId);
            if (updated <= 0) {
                throw new RuntimeException("扣减数据库库存失败");
            }

            SeckillOrderPO order = new SeckillOrderPO();
            order.setUserId(userId);
            order.setProductId(productId);
            order.setOrderNo(generateOrderNo());
            order.setSeckillPrice(message.getSeckillPrice());
            order.setStatus(STATUS_CREATED);
            seckillOrderMapper.insert(order);

            stringRedisTemplate.opsForValue().set(orderKey, String.valueOf(order.getId()), ttlSeconds, TimeUnit.SECONDS);
            // 秒杀订单创建成功后立即进入“待支付”阶段，避免结果口径再落回“伪成功”。
            stringRedisTemplate.opsForValue().set(resultKey, SeckillResultState.PENDING_PAYMENT, ttlSeconds, TimeUnit.SECONDS);
            log.info("创建秒杀订单成功，messageId={}, userId={}, productId={}, orderId={}",
                    message.getMessageId(), userId, productId, order.getId());
        } catch (DuplicateKeyException e) {
            SeckillOrderPO existed = seckillOrderMapper.getByUserIdAndProductId(userId, productId);
            if (existed != null) {
                cacheExistingOrderResult(orderKey, resultKey, ttlSeconds, existed);
                log.warn("重复秒杀消息命中已有订单，messageId={}, userId={}, productId={}, orderId={}, status={}",
                        message.getMessageId(), userId, productId, existed.getId(), existed.getStatus());
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
        stringRedisTemplate.opsForValue().set(resultKey, SeckillResultState.FAIL, ttlSeconds, TimeUnit.SECONDS);
        log.error("秒杀消息进入死信并完成补偿，请关注告警并排查原因，messageId={}, userId={}, productId={}",
                message.getMessageId(), userId, productId);
    }

    /**
     * 取消订单时先保证数据库订单状态与数据库库存一致，再尽力清理 Redis 态。
     * 这样即使 Redis 操作偶发失败，也不会破坏订单和库存这两份核心数据。
     */
    @Transactional(rollbackFor = Exception.class)
    protected Result<SeckillOrderVO> doCancelOrder(Long userId, Long id, String cancelReason) {
        SeckillOrderVO order = seckillOrderMapper.getOrderDetail(userId, id);
        if (order == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "秒杀订单不存在");
        }
        if (isCancelled(order.getStatus())) {
            return Result.success(order);
        }
        if (isPaid(order.getStatus())) {
            return Result.error(ResultCode.BUSINESS_ERROR, "已支付秒杀订单不能取消");
        }
        if (!isCreated(order.getStatus())) {
            return Result.error(ResultCode.BUSINESS_ERROR, "当前秒杀订单状态不允许取消");
        }

        LocalDateTime cancelTime = LocalDateTime.now();
        int updated = seckillOrderMapper.updateOrderStatus(
                id,
                userId,
                STATUS_CREATED,
                STATUS_CANCELLED,
                cancelReason,
                cancelTime
        );
        if (updated <= 0) {
            SeckillOrderVO latestOrder = seckillOrderMapper.getOrderDetail(userId, id);
            if (latestOrder != null) {
                if (isCancelled(latestOrder.getStatus())) {
                    return Result.success(latestOrder);
                }
                if (isPaid(latestOrder.getStatus())) {
                    return Result.error(ResultCode.BUSINESS_ERROR, "秒杀订单已支付，无法取消");
                }
            }
            return Result.error(ResultCode.SERVER_ERROR, "取消秒杀订单失败，请稍后重试");
        }

        if (seckillProductMapper.increaseStock(order.getProductId()) <= 0) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("取消秒杀订单后恢复数据库库存失败，orderId={}, productId={}", id, order.getProductId());
            return Result.error(ResultCode.SERVER_ERROR, "取消秒杀订单失败，请稍后重试");
        }

        cleanupCancelledOrderRedisState(order.getUserId(), order.getProductId());

        SeckillOrderVO cancelledOrder = seckillOrderMapper.getOrderDetail(userId, id);
        if (cancelledOrder == null) {
            return Result.error(ResultCode.SERVER_ERROR, "取消成功，但查询秒杀订单失败");
        }

        log.info("{}成功，userId={}, orderId={}, productId={}", cancelReason, userId, id, order.getProductId());
        return Result.success(cancelledOrder);
    }

    private void cleanupCancelledOrderRedisState(Long userId, Long productId) {
        String orderKey = RedisKeys.seckillOrder(userId, productId);
        String resultKey = RedisKeys.seckillResult(userId, productId);
        String stockKey = RedisKeys.seckillStock(productId);

        try {
            stringRedisTemplate.delete(orderKey);
            stringRedisTemplate.opsForValue().set(resultKey, SeckillResultState.FAIL, DEFAULT_RESULT_TTL_SECONDS, TimeUnit.SECONDS);
            stringRedisTemplate.opsForValue().increment(stockKey);
        } catch (Exception ex) {
            log.warn("同步秒杀订单取消后的 Redis 状态失败，userId={}, productId={}", userId, productId, ex);
        }
    }

    private String buildCancelledMessage(SeckillOrderVO order) {
        if (order == null || !StringUtils.hasText(order.getCancelReason())) {
            return "秒杀订单已取消";
        }
        if (order.getCancelTime() == null) {
            return order.getCancelReason();
        }
        return order.getCancelReason() + "，取消时间：" + order.getCancelTime();
    }

    private void cacheExistingOrderResult(String orderKey, String resultKey, long ttlSeconds, SeckillOrderPO order) {
        if (order == null) {
            return;
        }
        if (isCreated(order.getStatus())) {
            stringRedisTemplate.opsForValue().set(orderKey, String.valueOf(order.getId()), ttlSeconds, TimeUnit.SECONDS);
            stringRedisTemplate.opsForValue().set(resultKey, SeckillResultState.PENDING_PAYMENT, ttlSeconds, TimeUnit.SECONDS);
            return;
        }
        stringRedisTemplate.delete(orderKey);
        if (isPaid(order.getStatus())) {
            stringRedisTemplate.opsForValue().set(resultKey, SeckillResultState.ALREADY_PAID, ttlSeconds, TimeUnit.SECONDS);
            return;
        }
        if (isCancelled(order.getStatus())) {
            stringRedisTemplate.opsForValue().set(resultKey, SeckillResultState.CANCELLED, ttlSeconds, TimeUnit.SECONDS);
            return;
        }
        stringRedisTemplate.opsForValue().set(resultKey, SeckillResultState.FAIL, ttlSeconds, TimeUnit.SECONDS);
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

    private String generateOrderNo() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
