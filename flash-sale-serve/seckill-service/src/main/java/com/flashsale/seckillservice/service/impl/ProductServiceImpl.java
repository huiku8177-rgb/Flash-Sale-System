package com.flashsale.seckillservice.service.impl;

import com.flashsale.common.domain.Result;
import com.flashsale.seckillservice.domain.dto.ProductQueryDTO;
import com.flashsale.seckillservice.domain.po.ProductPO;
import com.flashsale.seckillservice.domain.vo.ProductVO;
import com.flashsale.seckillservice.mapper.ProductMapper;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.seckillservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description ProductServiceImpl
 * @date 2026/3/13 17:00
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result<List<ProductVO>> listProducts(ProductQueryDTO queryDTO) {
        return Result.success(productMapper.listProducts(queryDTO));
    }

    @Override
    public Result<ProductVO> getProductDetail(Long id) {
        return Result.success(productMapper.getProductDetail(id));
    }


    /**
     * 预热商品库存到 Redis
     */
    @Override
    public void loadStockToRedis() {
        List<ProductPO> products = productMapper.listAll();

        if (products == null || products.isEmpty()) {
            log.warn("没有需要预热的商品库存");
            return;
        }

        for (ProductPO product : products) {

            String key = RedisKeys.seckillStock(product.getId());

            stringRedisTemplate.opsForValue().set(
                    key,
                    String.valueOf(product.getStock())
            );

            log.info("库存预热完成 productId={} stock={}",
                    product.getId(),
                    product.getStock());
        }

    }
}
