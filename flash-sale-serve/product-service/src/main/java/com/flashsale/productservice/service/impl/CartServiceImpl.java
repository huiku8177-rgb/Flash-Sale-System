package com.flashsale.productservice.service.impl;

import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.productservice.domain.dto.CartItemAddDTO;
import com.flashsale.productservice.domain.dto.CartItemUpdateDTO;
import com.flashsale.productservice.domain.po.CartItemPO;
import com.flashsale.productservice.domain.vo.CartItemVO;
import com.flashsale.productservice.domain.vo.ProductVO;
import com.flashsale.productservice.mapper.CartMapper;
import com.flashsale.productservice.mapper.ProductMapper;
import com.flashsale.productservice.service.CartService;
import com.flashsale.productservice.service.ProductReadCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Slf4j
@Validated
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final int PRODUCT_ENABLED = 1;
    private static final int SELECTED = 1;
    private static final int UNSELECTED = 0;

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final ProductReadCacheService productReadCacheService;

    @Override
    public Result<List<CartItemVO>> listCartItems(Long userId) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        List<CartItemVO> cached = productReadCacheService.getCartItems(userId);
        if (cached != null) {
            return Result.success(cached);
        }

        List<CartItemVO> cartItems = cartMapper.listCartItems(userId);
        if (CollectionUtils.isEmpty(cartItems)) {
            productReadCacheService.cacheCartItems(userId, List.of());
            return Result.success(List.of());
        }

        productReadCacheService.cacheCartItems(userId, cartItems);
        return Result.success(cartItems);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<CartItemVO> addCartItem(Long userId, CartItemAddDTO requestDTO) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        if (requestDTO == null) {
            return Result.error(ResultCode.PARAM_ERROR, "请求参数不能为空");
        }

        ProductVO product = productMapper.getProductDetail(requestDTO.getProductId());
        if (product == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "商品不存在");
        }
        if (product.getStatus() == null || product.getStatus() != PRODUCT_ENABLED) {
            return Result.error(ResultCode.BUSINESS_ERROR, "商品已下架，无法加入购物车");
        }

        CartItemPO existingItem = cartMapper.findByUserIdAndProductId(userId, requestDTO.getProductId());
        int targetQuantity = requestDTO.getQuantity();
        if (existingItem != null) {
            targetQuantity += existingItem.getQuantity();
        }

        if (product.getStock() == null || product.getStock() < targetQuantity) {
            return Result.error(ResultCode.STOCK_EMPTY, "购物车数量不能超过当前库存");
        }

        Integer selected = toSelectedValue(requestDTO.getSelected(), existingItem == null ? SELECTED : existingItem.getSelected());
        CartItemVO result;
        if (existingItem == null) {
            CartItemPO cartItem = new CartItemPO();
            cartItem.setUserId(userId);
            cartItem.setProductId(requestDTO.getProductId());
            cartItem.setQuantity(requestDTO.getQuantity());
            cartItem.setSelected(selected);
            cartMapper.insertCartItem(cartItem);
            log.info("用户 {} 新增购物车商品，productId={}, quantity={}", userId, requestDTO.getProductId(), requestDTO.getQuantity());
            result = cartMapper.getCartItemDetail(userId, cartItem.getId());
        } else {
            cartMapper.updateCartItem(existingItem.getId(), userId, targetQuantity, selected);
            log.info("用户 {} 追加购物车商品，cartItemId={}, productId={}, quantity={}",
                    userId, existingItem.getId(), requestDTO.getProductId(), targetQuantity);
            result = cartMapper.getCartItemDetail(userId, existingItem.getId());
        }

        productReadCacheService.evictCartItems(userId);
        return Result.success(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<CartItemVO> updateCartItem(Long userId, Long id, CartItemUpdateDTO requestDTO) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        if (requestDTO == null || requestDTO.getQuantity() == null && requestDTO.getSelected() == null) {
            return Result.error(ResultCode.PARAM_ERROR, "至少需要传入一个可更新字段");
        }

        CartItemPO existingItem = cartMapper.getCartItem(userId, id);
        if (existingItem == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "购物车商品不存在");
        }

        int targetQuantity = requestDTO.getQuantity() == null ? existingItem.getQuantity() : requestDTO.getQuantity();
        int selected = toSelectedValue(requestDTO.getSelected(), existingItem.getSelected());

        if (requestDTO.getQuantity() != null) {
            ProductVO product = productMapper.getProductDetail(existingItem.getProductId());
            if (product == null) {
                return Result.error(ResultCode.BUSINESS_ERROR, "商品不存在");
            }
            if (product.getStatus() == null || product.getStatus() != PRODUCT_ENABLED) {
                return Result.error(ResultCode.BUSINESS_ERROR, "商品已下架，无法修改数量");
            }
            if (product.getStock() == null || product.getStock() < targetQuantity) {
                return Result.error(ResultCode.STOCK_EMPTY, "购物车数量不能超过当前库存");
            }
        }

        cartMapper.updateCartItem(id, userId, targetQuantity, selected);
        log.info("用户 {} 更新购物车商品，cartItemId={}, quantity={}, selected={}", userId, id, targetQuantity, selected);
        productReadCacheService.evictCartItems(userId);
        return Result.success(cartMapper.getCartItemDetail(userId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteCartItem(Long userId, Long id) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR, "购物车项ID不能为空");
        }

        int deleted = cartMapper.deleteCartItem(userId, id);
        if (deleted <= 0) {
            return Result.error(ResultCode.BUSINESS_ERROR, "购物车商品不存在");
        }

        log.info("用户 {} 删除购物车商品，cartItemId={}", userId, id);
        productReadCacheService.evictCartItems(userId);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> clearCart(Long userId, Boolean selectedOnly) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        boolean clearSelectedOnly = Boolean.TRUE.equals(selectedOnly);
        int deleted = clearSelectedOnly ? cartMapper.deleteSelectedCartItems(userId) : cartMapper.deleteAllCartItems(userId);
        log.info("用户 {} 清空购物车，selectedOnly={}, deleted={}", userId, clearSelectedOnly, deleted);
        productReadCacheService.evictCartItems(userId);
        return Result.success();
    }

    private int toSelectedValue(Boolean selected, Integer defaultValue) {
        if (selected == null) {
            return defaultValue == null ? SELECTED : defaultValue;
        }
        return selected ? SELECTED : UNSELECTED;
    }
}
