package com.flashsale.productservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.productservice.client.OrderInternalClient;
import com.flashsale.productservice.domain.dto.CreateNormalOrderItemDTO;
import com.flashsale.productservice.domain.dto.CreateNormalOrderRequestDTO;
import com.flashsale.productservice.domain.dto.NormalOrderCheckoutDTO;
import com.flashsale.productservice.domain.dto.ProductQueryDTO;
import com.flashsale.productservice.domain.vo.CartItemVO;
import com.flashsale.productservice.domain.vo.NormalOrderVO;
import com.flashsale.productservice.domain.vo.ProductVO;
import com.flashsale.productservice.mapper.CartMapper;
import com.flashsale.productservice.mapper.ProductMapper;
import com.flashsale.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author strive_qin
 * @version 1.0
 * @description ProductServiceImpl
 * @date 2026/3/20 00:00
 */
@Service
@Slf4j
@Validated
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final int PRODUCT_ENABLED = 1;

    private final ProductMapper productMapper;
    private final CartMapper cartMapper;
    private final OrderInternalClient orderInternalClient;
    private final ObjectMapper objectMapper;

    /**
     * 列表商品。
     *
     * @param queryDTO 查询参数
     * @return 商品列表
     */
    @Override
    public Result<List<ProductVO>> listProducts(ProductQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new ProductQueryDTO();
        }

        if (queryDTO.getStatus() == null) {
            queryDTO.setStatus(PRODUCT_ENABLED);
        }

        return Result.success(productMapper.listProducts(queryDTO));
    }

    /**
     * 获取商品详情。
     *
     * @param id 商品ID
     * @return 商品详情
     */
    @Override
    public Result<ProductVO> getProductDetail(Long id) {
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR, "商品ID不能为空");
        }

        ProductVO product = productMapper.getProductDetail(id);
        if (product == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "商品不存在");
        }

        return Result.success(product);
    }

    /**
     * 基于购物车已选商品创建普通订单。
     *
     * @param userId 用户ID
     * @param checkoutDTO 订单参数
     * @return 订单信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<NormalOrderVO> createNormalOrder(Long userId,
                                                   NormalOrderCheckoutDTO checkoutDTO) {
        if (checkoutDTO == null) {
            return Result.error(ResultCode.PARAM_ERROR, "请求参数不能为空");
        }

        List<CartItemVO> selectedCartItems = cartMapper.listSelectedCartItems(userId);
        if (CollectionUtils.isEmpty(selectedCartItems)) {
            return Result.error(ResultCode.PARAM_ERROR, "请至少勾选一件购物车商品");
        }

        String addressSnapshot = normalizeAddressSnapshot(checkoutDTO.getAddressSnapshot());
        if (StringUtils.hasText(checkoutDTO.getAddressSnapshot()) && addressSnapshot == null) {
            return Result.error(ResultCode.PARAM_ERROR, "地址快照必须是合法的JSON");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<CreateNormalOrderItemDTO> orderItems = new ArrayList<>();
        List<Long> cartItemIds = new ArrayList<>();
        Map<Long, Integer> mergedItems = new LinkedHashMap<>();

        for (CartItemVO cartItem : selectedCartItems) {
            if (cartItem.getId() != null) {
                cartItemIds.add(cartItem.getId());
            }
            if (cartItem.getProductId() == null) {
                return Result.error(ResultCode.BUSINESS_ERROR, "购物车中存在无效商品");
            }
            if (cartItem.getQuantity() == null || cartItem.getQuantity() <= 0) {
                return Result.error(ResultCode.PARAM_ERROR, "购物车商品数量必须大于0");
            }
            if (cartItem.getStatus() == null || cartItem.getStatus() != PRODUCT_ENABLED) {
                return Result.error(ResultCode.BUSINESS_ERROR, "商品已下架：" + getCartProductName(cartItem));
            }
            if (cartItem.getStock() == null || cartItem.getStock() < cartItem.getQuantity()) {
                return Result.error(ResultCode.STOCK_EMPTY, "库存不足：" + getCartProductName(cartItem));
            }
            if (cartItem.getPrice() == null) {
                return Result.error(ResultCode.BUSINESS_ERROR, "商品价格异常，无法创建订单");
            }

            BigDecimal itemAmount = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(itemAmount);
            mergedItems.put(cartItem.getProductId(), cartItem.getQuantity());

            CreateNormalOrderItemDTO itemDTO = new CreateNormalOrderItemDTO();
            itemDTO.setProductId(cartItem.getProductId());
            itemDTO.setProductName(getCartProductName(cartItem));
            itemDTO.setProductSubtitle(cartItem.getProductSubtitle());
            itemDTO.setProductImage(cartItem.getProductImage());
            itemDTO.setSalePrice(cartItem.getPrice());
            itemDTO.setQuantity(cartItem.getQuantity());
            itemDTO.setItemAmount(itemAmount);
            orderItems.add(itemDTO);
        }

        if (mergedItems.isEmpty()) {
            return Result.error(ResultCode.PARAM_ERROR, "请至少勾选一件购物车商品");
        }

        for (Map.Entry<Long, Integer> entry : mergedItems.entrySet()) {
            int updated = productMapper.decreaseStock(entry.getKey(), entry.getValue());
            if (updated <= 0) {
                throw new IllegalStateException("扣减普通商品库存失败，商品ID=" + entry.getKey());
            }
        }

        String orderNo = generateOrderNo();
        CreateNormalOrderRequestDTO requestDTO = new CreateNormalOrderRequestDTO();
        requestDTO.setOrderNo(orderNo);
        requestDTO.setUserId(userId);
        requestDTO.setTotalAmount(totalAmount);
        requestDTO.setPayAmount(totalAmount);
        requestDTO.setRemark(trimToNull(checkoutDTO.getRemark()));
        requestDTO.setAddressSnapshot(addressSnapshot);
        requestDTO.setItems(orderItems);

        try {
            Result<NormalOrderVO> createResult = orderInternalClient.createNormalOrder(requestDTO);
            if (isSuccess(createResult)) {
                removeCheckedOutCartItems(userId, cartItemIds);
                return Result.success(createResult.getData());
            }

            Result<NormalOrderVO> existingOrderResult = findByOrderNo(userId, orderNo);
            if (isSuccess(existingOrderResult)) {
                removeCheckedOutCartItems(userId, cartItemIds);
                return Result.success(existingOrderResult.getData());
            }

            restoreStock(mergedItems);
            String message = createResult == null ? "普通订单创建失败" : createResult.getMessage();
            return Result.error(ResultCode.SERVER_ERROR, StringUtils.hasText(message) ? message : "普通订单创建失败");
        } catch (Exception ex) {
            Result<NormalOrderVO> existingOrderResult = findByOrderNo(userId, orderNo);
            if (isSuccess(existingOrderResult)) {
                removeCheckedOutCartItems(userId, cartItemIds);
                return Result.success(existingOrderResult.getData());
            }

            restoreStock(mergedItems);
            log.error("调用订单服务创建普通订单失败，userId={}, orderNo={}", userId, orderNo, ex);
            return Result.error(ResultCode.SERVER_ERROR, "普通订单创建失败");
        }
    }

    private Result<NormalOrderVO> findByOrderNo(Long userId, String orderNo) {
        try {
            return orderInternalClient.getNormalOrderByOrderNo(userId, orderNo);
        } catch (Exception ex) {
            log.warn("建单后按订单号查询普通订单失败，userId={}, orderNo={}", userId, orderNo, ex);
            return null;
        }
    }

    private boolean isSuccess(Result<NormalOrderVO> result) {
        return result != null
                && result.getCode() == ResultCode.SUCCESS.getCode()
                && result.getData() != null;
    }

    private void restoreStock(Map<Long, Integer> mergedItems) {
        for (Map.Entry<Long, Integer> entry : mergedItems.entrySet()) {
            productMapper.increaseStock(entry.getKey(), entry.getValue());
        }
    }

    private void removeCheckedOutCartItems(Long userId, List<Long> cartItemIds) {
        if (CollectionUtils.isEmpty(cartItemIds)) {
            return;
        }
        cartMapper.deleteCartItemsByIds(userId, cartItemIds);
    }

    private String generateOrderNo() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeAddressSnapshot(String addressSnapshot) {
        String value = trimToNull(addressSnapshot);
        if (value == null) {
            return null;
        }

        try {
            objectMapper.readTree(value);
            return value;
        } catch (JsonProcessingException ex) {
            log.warn("地址快照不是合法的JSON：{}", value);
            return null;
        }
    }

    private String getCartProductName(CartItemVO cartItem) {
        return StringUtils.hasText(cartItem.getProductName()) ? cartItem.getProductName() : "未知商品";
    }
}
