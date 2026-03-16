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

import java.util.List;

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

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillOrderMapper orderMapper;
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
    public void createSeckillOrder(SeckillMessage message) {
        Long productId = message.getProductId();
        Long userId = message.getUserId();
        try {
            SeckillOrderPO order = new SeckillOrderPO();
            order.setUserId(userId);
            order.setProductId(productId);
            order.setSeckillPrice(message.getSeckillPrice());
            order.setStatus(0);

            orderMapper.insert(order);

            int updated = productMapper.decreaseStock(productId);
            if (updated <= 0) {
                throw new RuntimeException("扣减数据库库存失败");
            }
            // 写入秒杀结果缓存
            stringRedisTemplate.opsForValue().set(
                    RedisKeys.seckillOrder(message.getUserId(), message.getProductId()),
                    String.valueOf(order.getId())
            );
            String orderKey = RedisKeys.seckillOrder(message.getUserId(), message.getProductId());
            log.info("写入秒杀结果缓存 key={}, value={}", orderKey, order.getId());
            log.info("创建秒杀订单成功 userId={}, productId={}, orderId={}",
                    message.getUserId(),
                    message.getProductId(),
                    order.getId());

        } catch (DuplicateKeyException e) {
            log.warn("重复下单，已忽略: userId={}, productId={}", userId, productId);
        }
    }
    }

