package com.flashsale.productservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.productservice.client.OrderInternalClient;
import com.flashsale.productservice.domain.dto.CreateNormalOrderItemDTO;
import com.flashsale.productservice.domain.dto.CreateNormalOrderRequestDTO;
import com.flashsale.productservice.domain.dto.NormalOrderCheckoutDTO;
import com.flashsale.productservice.domain.dto.NormalOrderItemRequestDTO;
import com.flashsale.productservice.domain.dto.ProductQueryDTO;
import com.flashsale.productservice.domain.po.ProductPO;
import com.flashsale.productservice.domain.vo.NormalOrderVO;
import com.flashsale.productservice.domain.vo.ProductVO;
import com.flashsale.productservice.mapper.ProductMapper;
import com.flashsale.productservice.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
import java.util.stream.Collectors;
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
    private final OrderInternalClient orderInternalClient;
    private final ObjectMapper objectMapper;
    /**
     * 列表商品
     *
     * @param queryDTO 查询参数
     * @return 商品列表
     */
    @Override
    public Result<List<ProductVO>> listProducts(ProductQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new ProductQueryDTO();
        }

        // 默认只查询上架商品
        if (queryDTO.getStatus() == null) {
            queryDTO.setStatus(PRODUCT_ENABLED);
        }

        return Result.success(productMapper.listProducts(queryDTO));
    }
    /**
     * 获取商品详情
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
     * 创建普通订单
     *
     * @param userId       用户ID
     * @param checkoutDTO  订单参数
     * @return 订单信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<NormalOrderVO> createNormalOrder(@NotNull Long userId,
                                                   @Valid @NotNull NormalOrderCheckoutDTO checkoutDTO) {
        // 基础参数校验
        if (checkoutDTO == null || CollectionUtils.isEmpty(checkoutDTO.getItems())) {
            return Result.error(ResultCode.PARAM_ERROR, "请至少选择一件商品");
        }

        // 合并重复商品项，避免同一商品重复计算和重复扣库存
        Map<Long, Integer> mergedItems = mergeItems(checkoutDTO.getItems());
        if (mergedItems.isEmpty()) {
            return Result.error(ResultCode.PARAM_ERROR, "订单商品参数不合法");
        }

        // 地址快照统一做 JSON 合法性校验
        String addressSnapshot = normalizeAddressSnapshot(checkoutDTO.getAddressSnapshot());
        if (StringUtils.hasText(checkoutDTO.getAddressSnapshot()) && addressSnapshot == null) {
            return Result.error(ResultCode.PARAM_ERROR, "地址快照必须是合法的JSON");
        }

        // 查询商品并校验商品是否存在
        List<Long> productIds = new ArrayList<>(mergedItems.keySet());
        List<ProductPO> products = productMapper.listByIds(productIds);
        if (CollectionUtils.isEmpty(products) || products.size() != productIds.size()) {
            return Result.error(ResultCode.BUSINESS_ERROR, "存在不可下单的商品");
        }

        Map<Long, ProductPO> productMap = products.stream()
                .collect(Collectors.toMap(ProductPO::getId, product -> product));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<CreateNormalOrderItemDTO> orderItems = new ArrayList<>();

        // 校验商品状态、库存，并组装订单项
        for (Map.Entry<Long, Integer> entry : mergedItems.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            ProductPO product = productMap.get(productId);

            if (product == null) {
                return Result.error(ResultCode.BUSINESS_ERROR, "商品不存在");
            }
            if (product.getStatus() == null || product.getStatus() != PRODUCT_ENABLED) {
                return Result.error(ResultCode.BUSINESS_ERROR, "商品不可售：" + product.getName());
            }
            if (product.getStock() == null || product.getStock() < quantity) {
                return Result.error(ResultCode.STOCK_EMPTY, "库存不足：" + product.getName());
            }

            BigDecimal itemAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(itemAmount);

            CreateNormalOrderItemDTO itemDTO = new CreateNormalOrderItemDTO();
            itemDTO.setProductId(productId);
            itemDTO.setProductName(product.getName());
            itemDTO.setProductSubtitle(product.getSubtitle());
            itemDTO.setProductImage(product.getMainImage());
            itemDTO.setSalePrice(product.getPrice());
            itemDTO.setQuantity(quantity);
            itemDTO.setItemAmount(itemAmount);
            orderItems.add(itemDTO);
        }

        // 先扣减商品库存
        for (Map.Entry<Long, Integer> entry : mergedItems.entrySet()) {
            int updated = productMapper.decreaseStock(entry.getKey(), entry.getValue());
            if (updated <= 0) {
                throw new IllegalStateException("扣减普通商品库存失败，商品ID=" + entry.getKey());
            }
        }

        // 调用订单服务创建普通订单
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
                return Result.success(createResult.getData());
            }

            // 防止订单服务已成功落库但响应异常，尝试按订单号补查
            Result<NormalOrderVO> existingOrderResult = findByOrderNo(userId, orderNo);
            if (isSuccess(existingOrderResult)) {
                return Result.success(existingOrderResult.getData());
            }

            // 建单失败，恢复库存
            restoreStock(mergedItems);
            String message = createResult == null ? "普通订单创建失败" : createResult.getMessage();
            return Result.error(ResultCode.SERVER_ERROR, StringUtils.hasText(message) ? message : "普通订单创建失败");
        } catch (Exception ex) {
            Result<NormalOrderVO> existingOrderResult = findByOrderNo(userId, orderNo);
            if (isSuccess(existingOrderResult)) {
                return Result.success(existingOrderResult.getData());
            }

            restoreStock(mergedItems);
            log.error("调用订单服务创建普通订单失败，userId={}, orderNo={}", userId, orderNo, ex);
            return Result.error(ResultCode.SERVER_ERROR, "普通订单创建失败");
        }
    }

    // 建单后补偿查询，避免远程调用超时导致重复下单
    private Result<NormalOrderVO> findByOrderNo(Long userId, String orderNo) {
        try {
            return orderInternalClient.getNormalOrderByOrderNo(userId, orderNo);
        } catch (Exception ex) {
            log.warn("建单后按订单号查询普通订单失败，userId={}, orderNo={}", userId, orderNo, ex);
            return null;
        }
    }

    // 统一判断远程调用是否成功
    private boolean isSuccess(Result<NormalOrderVO> result) {
        return result != null
                && result.getCode() == ResultCode.SUCCESS.getCode()
                && result.getData() != null;
    }

    // 订单创建失败时回补库存
    private void restoreStock(Map<Long, Integer> mergedItems) {
        for (Map.Entry<Long, Integer> entry : mergedItems.entrySet()) {
            productMapper.increaseStock(entry.getKey(), entry.getValue());
        }
    }

    // 合并重复商品项，例如多个相同 productId 合并为一项
    private Map<Long, Integer> mergeItems(List<NormalOrderItemRequestDTO> items) {
        Map<Long, Integer> mergedItems = new LinkedHashMap<>();
        for (NormalOrderItemRequestDTO item : items) {
            if (item == null || item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            mergedItems.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return mergedItems;
    }

    // 生成普通订单号
    private String generateOrderNo() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // 字符串去空白，空字符串转 null
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    // 地址快照要求为合法 JSON，非法时返回 null
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
}
