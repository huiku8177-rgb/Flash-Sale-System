package com.flashsale.seckillservice.service.impl;

import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.seckillservice.domain.dto.SeckillProductQueryDTO;
import com.flashsale.seckillservice.domain.po.SeckillProductPO;
import com.flashsale.seckillservice.domain.vo.SeckillProductVO;
import com.flashsale.seckillservice.mapper.SeckillProductMapper;
import com.flashsale.seckillservice.service.SeckillProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillProductServiceImpl
 * @date 2026/3/20 00:00
 */


@Service
@RequiredArgsConstructor
@Slf4j
public class SeckillProductServiceImpl implements SeckillProductService {

    private final SeckillProductMapper seckillProductMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 查询秒杀商品列表
     *
     * @param queryDTO 查询参数
     * @return 商品列表
     */
    @Override
    public Result<List<SeckillProductVO>> listProducts(SeckillProductQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new SeckillProductQueryDTO();
        }
        // 默认只查询启用中的秒杀商品
        if (queryDTO.getStatus() == null) {
            queryDTO.setStatus(1);
        }
        return Result.success(seckillProductMapper.listProducts(queryDTO));
    }

    /**
     * 查询秒杀商品详情
     *
     * @param id 商品ID
     * @return 商品详情
     */
    @Override
    public Result<SeckillProductVO> getProductDetail(Long id) {
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR, "商品ID不能为空");
        }
        SeckillProductVO product = seckillProductMapper.getProductDetail(id);
        if (product == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "秒杀商品不存在");
        }
        return Result.success(product);
    }

    /**
     * 启动时将秒杀库存同步到 Redis
     */
    @Override
    public void loadStockToRedis() {
        List<SeckillProductPO> products = seckillProductMapper.listAll();

        if (products == null || products.isEmpty()) {
            log.warn("没有需要预热的秒杀商品库存");
            return;
        }

        // 按商品维度初始化 Redis 库存键
        for (SeckillProductPO product : products) {
            String key = RedisKeys.seckillStock(product.getId());
            stringRedisTemplate.opsForValue().set(key, String.valueOf(product.getStock()));
            log.info("秒杀商品库存预热完成 productId={} stock={}", product.getId(), product.getStock());
        }
    }
}
