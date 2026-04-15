package com.flashsale.seckillservice.service.impl;

import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.seckillservice.domain.dto.SeckillProductQueryDTO;
import com.flashsale.seckillservice.domain.po.SeckillProductPO;
import com.flashsale.seckillservice.domain.vo.SeckillProductVO;
import com.flashsale.seckillservice.mapper.SeckillProductMapper;
import com.flashsale.seckillservice.service.SeckillProductCacheService;
import com.flashsale.seckillservice.service.SeckillProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeckillProductServiceImpl implements SeckillProductService {

    private final SeckillProductMapper seckillProductMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final SeckillProductCacheService seckillProductCacheService;

    @Override
    public Result<List<SeckillProductVO>> listProducts(SeckillProductQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new SeckillProductQueryDTO();
        }
        if (queryDTO.getStatus() == null) {
            queryDTO.setStatus(1);
        }

        List<SeckillProductVO> cached = seckillProductCacheService.getProductList(queryDTO);
        if (cached != null) {
            return Result.success(cached);
        }

        List<SeckillProductVO> products = seckillProductMapper.listProducts(queryDTO);
        seckillProductCacheService.cacheProductList(queryDTO, products);
        return Result.success(seckillProductCacheService.applyRealtimeStock(products));
    }

    @Override
    public Result<SeckillProductVO> getProductDetail(Long id) {
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR, "商品ID不能为空");
        }

        SeckillProductVO cached = seckillProductCacheService.getProductDetail(id);
        if (cached != null) {
            return Result.success(cached);
        }

        SeckillProductVO product = seckillProductMapper.getProductDetail(id);
        if (product == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "秒杀商品不存在");
        }

        seckillProductCacheService.cacheProductDetail(product);
        return Result.success(seckillProductCacheService.applyRealtimeStock(product));
    }

    @Override
    public void loadStockToRedis() {
        List<SeckillProductPO> products = seckillProductMapper.listAll();
        if (products == null || products.isEmpty()) {
            log.warn("没有需要预热的秒杀商品库存");
            return;
        }

        for (SeckillProductPO product : products) {
            String key = RedisKeys.seckillStock(product.getId());
            stringRedisTemplate.opsForValue().set(key, String.valueOf(product.getStock()));
            log.info("秒杀商品库存预热完成 productId={} stock={}", product.getId(), product.getStock());
        }
    }
}
