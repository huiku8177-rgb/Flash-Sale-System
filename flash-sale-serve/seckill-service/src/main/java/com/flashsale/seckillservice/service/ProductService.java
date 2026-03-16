package com.flashsale.seckillservice.service;

import com.flashsale.common.domain.Result;
import com.flashsale.seckillservice.domain.dto.ProductQueryDTO;
import com.flashsale.seckillservice.domain.vo.ProductVO;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description ProductService
 * @date 2026/3/13 16:07
 */
public interface ProductService {

    Result<List<ProductVO>> listProducts(ProductQueryDTO queryDTO);

    Result<ProductVO> getProductDetail(Long id);
    void loadStockToRedis();
}
