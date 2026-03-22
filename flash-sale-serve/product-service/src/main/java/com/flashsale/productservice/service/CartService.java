package com.flashsale.productservice.service;

import com.flashsale.common.domain.Result;
import com.flashsale.productservice.domain.dto.CartItemAddDTO;
import com.flashsale.productservice.domain.dto.CartItemUpdateDTO;
import com.flashsale.productservice.domain.vo.CartItemVO;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description CartService
 * @date 2026/3/22 00:00
 */
public interface CartService {

    /**
     * 查询当前用户的购物车列表。
     *
     * @param userId 用户ID
     * @return 购物车商品列表
     */
    Result<List<CartItemVO>> listCartItems(Long userId);

    /**
     * 新增商品到购物车。
     *
     * @param userId 用户ID
     * @param requestDTO 新增参数
     * @return 最新购物车商品信息
     */
    Result<CartItemVO> addCartItem(Long userId, CartItemAddDTO requestDTO);

    /**
     * 更新购物车商品数量或选中状态。
     *
     * @param userId 用户ID
     * @param id 购物车项ID
     * @param requestDTO 更新参数
     * @return 最新购物车商品信息
     */
    Result<CartItemVO> updateCartItem(Long userId, Long id, CartItemUpdateDTO requestDTO);

    /**
     * 删除单个购物车商品。
     *
     * @param userId 用户ID
     * @param id 购物车项ID
     * @return 删除结果
     */
    Result<Void> deleteCartItem(Long userId, Long id);

    /**
     * 清空购物车。
     *
     * @param userId 用户ID
     * @param selectedOnly 是否只清空已选中的商品
     * @return 清空结果
     */
    Result<Void> clearCart(Long userId, Boolean selectedOnly);
}
