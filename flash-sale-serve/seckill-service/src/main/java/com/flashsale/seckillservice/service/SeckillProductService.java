package com.flashsale.seckillservice.service;

import com.flashsale.common.domain.Result;
import com.flashsale.seckillservice.domain.dto.SeckillProductQueryDTO;
import com.flashsale.seckillservice.domain.vo.SeckillProductVO;

import java.util.List;
/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillProductService
 * @date 2026/3/20 00:00
 */


public interface SeckillProductService {

    /**
     * 查询秒杀商品列表
     *
     * @param queryDTO 查询参数
     * @return 商品列表
     */
    Result<List<SeckillProductVO>> listProducts(SeckillProductQueryDTO queryDTO);

    /**
     * 查询秒杀商品详情
     *
     * @param id 商品ID
     * @return 商品详情
     */
    Result<SeckillProductVO> getProductDetail(Long id);

    /**
     * 将秒杀库存预热到 Redis
     */
    void loadStockToRedis();
}
