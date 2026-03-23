package com.flashsale.productservice.service.impl;

import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.common.util.AddressUtils;
import com.flashsale.productservice.client.AuthAddressClient;
import com.flashsale.productservice.client.OrderInternalClient;
import com.flashsale.productservice.domain.dto.CreateNormalOrderItemDTO;
import com.flashsale.productservice.domain.dto.CreateNormalOrderRequestDTO;
import com.flashsale.productservice.domain.dto.NormalOrderCheckoutDTO;
import com.flashsale.productservice.domain.dto.ProductQueryDTO;
import com.flashsale.productservice.domain.vo.CartItemVO;
import com.flashsale.productservice.domain.vo.NormalOrderVO;
import com.flashsale.productservice.domain.vo.ProductVO;
import com.flashsale.productservice.domain.vo.UserAddressVO;
import com.flashsale.productservice.mapper.CartMapper;
import com.flashsale.productservice.mapper.ProductMapper;
import com.flashsale.productservice.service.ProductOrderLocalTxService;
import com.flashsale.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
    private final AuthAddressClient authAddressClient;
    private final ProductOrderLocalTxService productOrderLocalTxService;

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

    @Override
    public Result<ProductVO> getProductDetail(Long id) {
        log.info("获取商品详情，商品ID={}", id);
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR, "商品ID不能为空");
        }

        ProductVO product = productMapper.getProductDetail(id);
        if (product == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "商品不存在");
        }

        return Result.success(product);
    }

    @Override
    public Result<NormalOrderVO> createNormalOrder(Long userId, NormalOrderCheckoutDTO checkoutDTO) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        if (checkoutDTO == null) {
            return Result.error(ResultCode.PARAM_ERROR, "请求参数不能为空");
        }

        List<CartItemVO> selectedCartItems = cartMapper.listSelectedCartItems(userId);
        if (CollectionUtils.isEmpty(selectedCartItems)) {
            return Result.error(ResultCode.PARAM_ERROR, "请至少勾选一件购物车商品");
        }

        UserAddressVO address = getValidatedAddress(userId, checkoutDTO.getAddressId());
        if (address == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "收货地址不存在或不可用");
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

        // 先在商品服务本地事务里预占库存，避免远程建单成功前库存已被并发抢空。
        try {
            productOrderLocalTxService.reserveStock(mergedItems);
        } catch (Exception ex) {
            log.warn("预占普通商品库存失败，userId={}, mergedItems={}", userId, mergedItems, ex);
            return Result.error(ResultCode.STOCK_EMPTY, "下单失败，部分商品库存不足，请刷新后重试");
        }

        String orderNo = generateOrderNo();
        CreateNormalOrderRequestDTO requestDTO = new CreateNormalOrderRequestDTO();
        requestDTO.setOrderNo(orderNo);
        requestDTO.setUserId(userId);
        requestDTO.setTotalAmount(totalAmount);
        requestDTO.setPayAmount(totalAmount);
        requestDTO.setRemark(trimToNull(checkoutDTO.getRemark()));
        requestDTO.setReceiver(address.getReceiver());
        requestDTO.setMobile(address.getMobile());
        requestDTO.setDetail(address.getDetail());
        requestDTO.setItems(orderItems);

        try {
            // 远程建单成功后再清理购物车；如果请求超时，则优先按 orderNo 回查兜底，避免重复建单。
            Result<NormalOrderVO> createResult = orderInternalClient.createNormalOrder(requestDTO);
            if (isSuccess(createResult)) {
                cleanupCheckedOutCartItems(userId, cartItemIds, orderNo);
                return Result.success(createResult.getData());
            }

            Result<NormalOrderVO> existingOrderResult = findByOrderNo(userId, orderNo);
            if (isSuccess(existingOrderResult)) {
                cleanupCheckedOutCartItems(userId, cartItemIds, orderNo);
                return Result.success(existingOrderResult.getData());
            }

            compensateStock(mergedItems, userId, orderNo);
            String message = createResult == null ? "普通订单创建失败" : createResult.getMessage();
            return Result.error(ResultCode.SERVER_ERROR, StringUtils.hasText(message) ? message : "普通订单创建失败");
        } catch (Exception ex) {
            Result<NormalOrderVO> existingOrderResult = findByOrderNo(userId, orderNo);
            if (isSuccess(existingOrderResult)) {
                cleanupCheckedOutCartItems(userId, cartItemIds, orderNo);
                return Result.success(existingOrderResult.getData());
            }

            compensateStock(mergedItems, userId, orderNo);
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

    private void compensateStock(Map<Long, Integer> mergedItems, Long userId, String orderNo) {
        try {
            productOrderLocalTxService.restoreStock(mergedItems);
        } catch (Exception ex) {
            log.error("普通订单失败后补偿库存失败，userId={}, orderNo={}, mergedItems={}", userId, orderNo, mergedItems, ex);
        }
    }

    private void cleanupCheckedOutCartItems(Long userId, List<Long> cartItemIds, String orderNo) {
        try {
            productOrderLocalTxService.removeCheckedOutCartItems(userId, cartItemIds);
        } catch (Exception ex) {
            log.error("普通订单创建成功后删除购物车失败，userId={}, orderNo={}, cartItemIds={}", userId, orderNo, cartItemIds, ex);
            try {
                productOrderLocalTxService.unselectCartItems(userId, cartItemIds);
            } catch (Exception secondaryEx) {
                log.error("普通订单创建成功后取消购物车勾选状态也失败，userId={}, orderNo={}, cartItemIds={}",
                        userId, orderNo, cartItemIds, secondaryEx);
            }
        }
    }

    private UserAddressVO getValidatedAddress(Long userId, Long addressId) {
        if (addressId == null) {
            return null;
        }

        try {
            Result<UserAddressVO> addressResult = authAddressClient.getAddressDetail(userId, addressId);
            if (addressResult == null
                    || addressResult.getCode() != ResultCode.SUCCESS.getCode()
                    || addressResult.getData() == null) {
                return null;
            }

            UserAddressVO address = addressResult.getData();
            String receiver = AddressUtils.trimToNull(address.getReceiver());
            String mobile = AddressUtils.trimToNull(address.getMobile());
            String detail = AddressUtils.trimToNull(address.getDetail());
            if (!AddressUtils.hasRequiredFields(receiver, mobile, detail) || !AddressUtils.isMobileValid(mobile)) {
                return null;
            }

            address.setReceiver(receiver);
            address.setMobile(mobile);
            address.setDetail(detail);
            return address;
        } catch (Exception ex) {
            log.warn("查询收货地址失败，userId={}, addressId={}", userId, addressId, ex);
            return null;
        }
    }

    private String generateOrderNo() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String getCartProductName(CartItemVO cartItem) {
        return StringUtils.hasText(cartItem.getProductName()) ? cartItem.getProductName() : "未知商品";
    }
}
