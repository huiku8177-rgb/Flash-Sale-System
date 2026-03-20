package com.flashsale.productservice.service;

import com.flashsale.common.domain.Result;
import com.flashsale.productservice.domain.dto.NormalOrderCheckoutDTO;
import com.flashsale.productservice.domain.dto.ProductQueryDTO;
import com.flashsale.productservice.domain.vo.NormalOrderVO;
import com.flashsale.productservice.domain.vo.ProductVO;

import java.util.List;
/**
 * @author strive_qin
 * @version 1.0
 * @description ProductService
 * @date 2026/3/20 00:00
 */


public interface ProductService {

    /**
     * 查询商品列表
     *
     * @param queryDTO 查询参数
     * @return 商品列表
     */
    Result<List<ProductVO>> listProducts(ProductQueryDTO queryDTO);
    /**
     * 查询商品详情
     *
     * @param id 商品ID
     * @return 商品详情
     */
    Result<ProductVO> getProductDetail(Long id);
    /**
     * 创建普通订单
     *
     * @param userId       用户ID
     * @param checkoutDTO  订单参数
     * @return 订单信息
     */
    Result<NormalOrderVO> createNormalOrder(Long userId, NormalOrderCheckoutDTO checkoutDTO);
}
