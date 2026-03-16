package com.flashsale.seckillservice.service.impl;

import com.flashsale.common.domain.Result;
import com.flashsale.seckillservice.domain.dto.SeckillRequestDTO;
import com.flashsale.seckillservice.domain.po.ProductPO;
import com.flashsale.seckillservice.domain.vo.SeckillResultVO;
import com.flashsale.seckillservice.domain.vo.SeckillStatusVO;
import com.flashsale.seckillservice.mapper.ProductMapper;
import com.flashsale.seckillservice.mq.SeckillProducer;
import com.flashsale.seckillservice.mq.message.SeckillMessage;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.seckillservice.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillServiceImpl
 * @date 2026/3/13 17:00
 */
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final ProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> seckillScript;
    private final SeckillProducer seckillProducer;

    @Override
    public Result<SeckillResultVO> seckill(SeckillRequestDTO requestDTO) {
        Long productId = requestDTO.getProductId();
        Long userId = requestDTO.getUserId();
        SeckillResultVO result = new SeckillResultVO();
        result.setProductId(productId);
        // 1. 参数校验
        if (productId == null || userId == null) {
            result.setSuccess(false);
            result.setMessage("请求参数错误");
            return Result.success(result);
        }
        // 2. 查询商品
        ProductPO product = productMapper.getById(productId);
        if (product == null) {
            result.setSuccess(false);
            result.setMessage("商品不存在");
            return Result.success(result);
        }

        // 3. 商品状态校验
        if (product.getStatus() == null || product.getStatus() != 1) {
            result.setSuccess(false);
            result.setMessage("商品已下架");
            return Result.success(result);
        }

        // 4. 秒杀时间校验
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
        // 5. 执行 Lua 脚本：校验库存 + 防重复秒杀 + 扣库存
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

        // 6. 发送 MQ，异步创建订单
        SeckillMessage sm=new SeckillMessage();
        sm.setUserId(userId);
        sm.setProductId(productId);
        sm.setSeckillPrice(product.getSeckillPrice());
        sm.setCreateTime(now);
        seckillProducer.sendSeckillMessage(sm);
        // 7. 立即返回结果
        result.setSuccess(true);
        result.setMessage("秒杀成功，订单处理中");
        return Result.success(result);
    }


    /**
     * 获取秒杀结果
     * @param userId
     * @param productId
     * @return
     */
    @Override
    public Result<SeckillStatusVO> getSeckillResult(Long userId, Long productId) {
        SeckillStatusVO result = new SeckillStatusVO();

        if (userId == null || productId == null) {
            result.setStatus(-1);
            result.setMessage("请求参数错误");
            return Result.success(result);
        }

        // 1. 先查是否已经生成订单
        String orderKey = RedisKeys.seckillOrder(userId, productId);
        String orderIdStr = stringRedisTemplate.opsForValue().get(orderKey);

        if (orderIdStr != null) {
            result.setStatus(1);
            result.setMessage("秒杀成功");
            result.setOrderId(Long.valueOf(orderIdStr));
            return Result.success(result);
        }

        // 2. 再查用户是否已经抢购过（说明请求已进入异步流程，订单可能还在创建中）
        String userKey = RedisKeys.seckillUser(productId);
        Boolean exists = stringRedisTemplate.opsForSet().isMember(userKey, String.valueOf(userId));

        if (Boolean.TRUE.equals(exists)) {
            result.setStatus(0);
            result.setMessage("排队中");
            return Result.success(result);
        }

        // 3. 都没有，说明秒杀失败
        result.setStatus(-1);
        result.setMessage("秒杀失败");
        return Result.success(result);

    }
}
