package com.flashsale.productservice.mapper;

import com.flashsale.productservice.domain.po.CartItemPO;
import com.flashsale.productservice.domain.vo.CartItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description CartMapper
 * @date 2026/3/22 00:00
 */
@Mapper
public interface CartMapper {

    List<CartItemVO> listCartItems(@Param("userId") Long userId);

    List<CartItemVO> listSelectedCartItems(@Param("userId") Long userId);

    CartItemVO getCartItemDetail(@Param("userId") Long userId, @Param("id") Long id);

    CartItemPO getCartItem(@Param("userId") Long userId, @Param("id") Long id);

    CartItemPO findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    int insertCartItem(CartItemPO cartItem);

    int updateCartItem(@Param("id") Long id,
                       @Param("userId") Long userId,
                       @Param("quantity") Integer quantity,
                       @Param("selected") Integer selected);

    int deleteCartItem(@Param("userId") Long userId, @Param("id") Long id);

    int deleteAllCartItems(@Param("userId") Long userId);

    int deleteSelectedCartItems(@Param("userId") Long userId);

    int deleteCartItemsByIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    int updateSelectedByIds(@Param("userId") Long userId,
                            @Param("ids") List<Long> ids,
                            @Param("selected") Integer selected);
}
