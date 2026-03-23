package com.flashsale.orderservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.common.util.AddressUtils;
import com.flashsale.orderservice.client.ProductInternalClient;
import com.flashsale.orderservice.domain.dto.internal.InternalCreateNormalOrderItemDTO;
import com.flashsale.orderservice.domain.dto.internal.InternalCreateNormalOrderRequestDTO;
import com.flashsale.orderservice.domain.dto.internal.InternalRestoreNormalOrderStockItemDTO;
import com.flashsale.orderservice.domain.dto.internal.InternalRestoreNormalOrderStockRequestDTO;
import com.flashsale.orderservice.domain.po.NormalOrderItemPO;
import com.flashsale.orderservice.domain.po.NormalOrderPO;
import com.flashsale.orderservice.domain.vo.NormalOrderItemVO;
import com.flashsale.orderservice.domain.vo.NormalOrderPayStatusVO;
import com.flashsale.orderservice.domain.vo.NormalOrderVO;
import com.flashsale.orderservice.mapper.NormalOrderItemMapper;
import com.flashsale.orderservice.mapper.NormalOrderMapper;
import com.flashsale.orderservice.service.NormalOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author strive_qin
 * @version 1.0
 * @description NormalOrderServiceImpl
 * @date 2026/3/20 00:00
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NormalOrderServiceImpl implements NormalOrderService {

    private static final int STATUS_CREATED = 0;
    private static final int STATUS_PAID = 1;
    private static final int STATUS_CANCELLED = 2;
    private static final int TIMEOUT_MINUTES = 15;
    private static final int TIMEOUT_SCAN_LIMIT = 100;

    private final NormalOrderMapper normalOrderMapper;
    private final NormalOrderItemMapper normalOrderItemMapper;
    private final ProductInternalClient productInternalClient;
    private final ObjectMapper objectMapper;

    /**
     * 根据商品服务传入的商品快照和地址信息创建普通订单。
     *
     * @param requestDTO 内部创建订单参数
     * @return 订单结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<NormalOrderVO> createOrder(InternalCreateNormalOrderRequestDTO requestDTO) {
        if (requestDTO == null) {
            return Result.error(ResultCode.PARAM_ERROR, "创建订单请求不能为空");
        }
        if (requestDTO.getUserId() == null) {
            return Result.error(ResultCode.PARAM_ERROR, "用户ID不能为空");
        }
        if (!StringUtils.hasText(requestDTO.getOrderNo())) {
            return Result.error(ResultCode.PARAM_ERROR, "订单号不能为空");
        }
        if (CollectionUtils.isEmpty(requestDTO.getItems())) {
            return Result.error(ResultCode.PARAM_ERROR, "订单商品不能为空");
        }

        Result<NormalOrderVO> existingOrderResult = getOrderByOrderNo(requestDTO.getUserId(), requestDTO.getOrderNo());
        if (existingOrderResult.getCode() == ResultCode.SUCCESS.getCode() && existingOrderResult.getData() != null) {
            return existingOrderResult;
        }

        String receiver = AddressUtils.trimToNull(requestDTO.getReceiver());
        String mobile = AddressUtils.trimToNull(requestDTO.getMobile());
        String detail = AddressUtils.trimToNull(requestDTO.getDetail());
        if (!AddressUtils.hasRequiredFields(receiver, mobile, detail)) {
            return Result.error(ResultCode.PARAM_ERROR, "收货人、手机号和收货地址不能为空");
        }
        if (!AddressUtils.isMobileValid(mobile)) {
            return Result.error(ResultCode.PARAM_ERROR, "手机号格式不正确");
        }

        BigDecimal computedTotalAmount = BigDecimal.ZERO;
        List<NormalOrderItemPO> itemPOList = new ArrayList<>();

        for (InternalCreateNormalOrderItemDTO itemDTO : requestDTO.getItems()) {
            if (itemDTO == null
                    || itemDTO.getProductId() == null
                    || itemDTO.getQuantity() == null
                    || itemDTO.getQuantity() <= 0
                    || itemDTO.getSalePrice() == null
                    || itemDTO.getItemAmount() == null
                    || !StringUtils.hasText(itemDTO.getProductName())) {
                return Result.error(ResultCode.PARAM_ERROR, "订单商品参数不合法");
            }

            BigDecimal expectedItemAmount = itemDTO.getSalePrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            if (expectedItemAmount.compareTo(itemDTO.getItemAmount()) != 0) {
                return Result.error(ResultCode.PARAM_ERROR, "订单商品金额不匹配");
            }

            computedTotalAmount = computedTotalAmount.add(itemDTO.getItemAmount());

            NormalOrderItemPO itemPO = new NormalOrderItemPO();
            itemPO.setUserId(requestDTO.getUserId());
            itemPO.setProductId(itemDTO.getProductId());
            itemPO.setProductName(itemDTO.getProductName());
            itemPO.setProductSubtitle(trimToNull(itemDTO.getProductSubtitle()));
            itemPO.setProductImage(trimToNull(itemDTO.getProductImage()));
            itemPO.setSalePrice(itemDTO.getSalePrice());
            itemPO.setQuantity(itemDTO.getQuantity());
            itemPO.setItemAmount(itemDTO.getItemAmount());
            itemPOList.add(itemPO);
        }

        if (requestDTO.getTotalAmount() == null || requestDTO.getPayAmount() == null) {
            return Result.error(ResultCode.PARAM_ERROR, "订单金额不能为空");
        }
        if (computedTotalAmount.compareTo(requestDTO.getTotalAmount()) != 0
                || requestDTO.getTotalAmount().compareTo(requestDTO.getPayAmount()) != 0) {
            return Result.error(ResultCode.PARAM_ERROR, "订单总金额不匹配");
        }

        String addressSnapshot;
        try {
            addressSnapshot = AddressUtils.buildSnapshot(receiver, mobile, detail, objectMapper);
        } catch (Exception ex) {
            log.warn("构建订单地址快照失败，userId={}, orderNo={}", requestDTO.getUserId(), requestDTO.getOrderNo(), ex);
            return Result.error(ResultCode.PARAM_ERROR, "收货信息格式不正确");
        }

        NormalOrderPO orderPO = new NormalOrderPO();
        orderPO.setOrderNo(requestDTO.getOrderNo().trim());
        orderPO.setUserId(requestDTO.getUserId());
        orderPO.setOrderStatus(STATUS_CREATED);
        orderPO.setTotalAmount(requestDTO.getTotalAmount());
        orderPO.setPayAmount(requestDTO.getPayAmount());
        orderPO.setRemark(trimToNull(requestDTO.getRemark()));
        orderPO.setAddressSnapshot(addressSnapshot);

        try {
            normalOrderMapper.insert(orderPO);
        } catch (DuplicateKeyException ex) {
            log.warn("普通订单幂等命中，orderNo={}, userId={}", requestDTO.getOrderNo(), requestDTO.getUserId());
            return getOrderByOrderNo(requestDTO.getUserId(), requestDTO.getOrderNo());
        }

        for (NormalOrderItemPO itemPO : itemPOList) {
            itemPO.setOrderId(orderPO.getId());
        }
        normalOrderItemMapper.insertBatch(itemPOList);

        NormalOrderVO orderVO = loadOrder(requestDTO.getUserId(), orderPO.getId());
        if (orderVO == null) {
            return Result.error(ResultCode.SERVER_ERROR, "普通订单创建成功，但查询详情失败");
        }

        log.info("普通订单创建成功，userId={}, orderId={}, orderNo={}",
                requestDTO.getUserId(), orderPO.getId(), orderPO.getOrderNo());
        return Result.success(orderVO);
    }

    @Override
    public Result<NormalOrderVO> getOrderByOrderNo(Long userId, String orderNo) {
        if (userId == null || !StringUtils.hasText(orderNo)) {
            return Result.error(ResultCode.PARAM_ERROR, "用户ID和订单号不能为空");
        }

        NormalOrderVO order = normalOrderMapper.getOrderDetailByOrderNo(userId, orderNo.trim());
        if (order == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "普通订单不存在");
        }
        return Result.success(fillOrderDetail(order));
    }

    @Override
    public Result<List<NormalOrderVO>> listOrders(Long userId, Integer orderStatus) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        List<NormalOrderVO> orders = normalOrderMapper.listOrders(userId, orderStatus);
        if (CollectionUtils.isEmpty(orders)) {
            return Result.success(Collections.emptyList());
        }

        List<Long> orderIds = orders.stream().map(NormalOrderVO::getId).toList();
        List<NormalOrderItemVO> items = normalOrderItemMapper.listByOrderIds(orderIds);
        Map<Long, List<NormalOrderItemVO>> itemMap = items.stream()
                .collect(Collectors.groupingBy(NormalOrderItemVO::getOrderId));

        for (NormalOrderVO order : orders) {
            fillAddressInfo(order);
            order.setItems(itemMap.getOrDefault(order.getId(), Collections.emptyList()));
        }

        return Result.success(orders);
    }

    @Override
    public Result<NormalOrderVO> getOrderDetail(Long userId, Long id) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR, "订单ID不能为空");
        }

        NormalOrderVO order = loadOrder(userId, id);
        if (order == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "普通订单不存在");
        }
        return Result.success(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<NormalOrderVO> mockPay(Long userId, Long id) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR, "订单ID不能为空");
        }

        NormalOrderVO order = loadOrder(userId, id);
        if (order == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "普通订单不存在");
        }
        if (isCancelled(order.getOrderStatus())) {
            return Result.error(ResultCode.BUSINESS_ERROR, "已取消订单不能支付");
        }
        if (isPaid(order.getOrderStatus())) {
            return Result.success(order);
        }
        if (!isCreated(order.getOrderStatus())) {
            return Result.error(ResultCode.BUSINESS_ERROR, "当前订单状态不允许支付");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = normalOrderMapper.updatePayStatus(
                id,
                userId,
                STATUS_CREATED,
                STATUS_PAID,
                order.getPayAmount(),
                now
        );
        if (updated <= 0) {
            NormalOrderVO latestOrder = loadOrder(userId, id);
            if (latestOrder != null) {
                if (isPaid(latestOrder.getOrderStatus())) {
                    return Result.success(latestOrder);
                }
                if (isCancelled(latestOrder.getOrderStatus())) {
                    return Result.error(ResultCode.BUSINESS_ERROR, "订单已取消，无法支付");
                }
            }
            return Result.error(ResultCode.BUSINESS_ERROR, "普通订单支付失败，请刷新后重试");
        }

        NormalOrderVO paidOrder = loadOrder(userId, id);
        if (paidOrder == null) {
            return Result.error(ResultCode.SERVER_ERROR, "支付成功，但查询订单失败");
        }
        return Result.success(paidOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<NormalOrderVO> cancelOrder(Long userId, Long id) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR, "订单ID不能为空");
        }
        return doCancelOrder(userId, id, "用户主动取消");
    }

    @Override
    public Result<NormalOrderPayStatusVO> getPayStatus(Long userId, Long id) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR, "订单ID不能为空");
        }

        NormalOrderVO order = normalOrderMapper.getOrderDetail(userId, id);
        if (order == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "普通订单不存在");
        }

        NormalOrderPayStatusVO payStatusVO = new NormalOrderPayStatusVO();
        payStatusVO.setOrderId(order.getId());
        payStatusVO.setOrderNo(order.getOrderNo());
        payStatusVO.setOrderStatus(order.getOrderStatus());
        payStatusVO.setPayAmount(order.getPayAmount());
        payStatusVO.setPayTime(order.getPayTime());

        boolean paid = isPaid(order.getOrderStatus());
        payStatusVO.setPaid(paid);
        payStatusVO.setCancelReason(order.getCancelReason());
        payStatusVO.setCancelTime(order.getCancelTime());
        if (isCancelled(order.getOrderStatus())) {
            payStatusVO.setMessage(buildCancelledMessage(order));
        } else {
            payStatusVO.setMessage(paid ? "订单已支付" : "订单待支付");
        }
        return Result.success(payStatusVO);
    }

    @Override
    public int cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        List<NormalOrderPO> timeoutOrders = normalOrderMapper.listTimeoutOrders(deadline, STATUS_CREATED, TIMEOUT_SCAN_LIMIT);
        if (CollectionUtils.isEmpty(timeoutOrders)) {
            return 0;
        }

        int cancelled = 0;
        for (NormalOrderPO timeoutOrder : timeoutOrders) {
            if (timeoutOrder == null || timeoutOrder.getUserId() == null || timeoutOrder.getId() == null) {
                continue;
            }
            Result<NormalOrderVO> cancelResult = doCancelOrder(timeoutOrder.getUserId(), timeoutOrder.getId(), "超时自动取消");
            if (cancelResult.getCode() == ResultCode.SUCCESS.getCode()
                    && cancelResult.getData() != null
                    && isCancelled(cancelResult.getData().getOrderStatus())) {
                cancelled++;
            }
        }
        return cancelled;
    }

    @Transactional(rollbackFor = Exception.class)
    protected Result<NormalOrderVO> doCancelOrder(Long userId, Long id, String source) {
        NormalOrderVO order = loadOrder(userId, id);
        if (order == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "普通订单不存在");
        }
        if (isCancelled(order.getOrderStatus())) {
            return Result.success(order);
        }
        if (isPaid(order.getOrderStatus())) {
            return Result.error(ResultCode.BUSINESS_ERROR, "已支付订单不能取消");
        }
        if (!isCreated(order.getOrderStatus())) {
            return Result.error(ResultCode.BUSINESS_ERROR, "当前订单状态不允许取消");
        }

        LocalDateTime cancelTime = LocalDateTime.now();
        int updated = normalOrderMapper.updateOrderStatus(
                id,
                userId,
                STATUS_CREATED,
                STATUS_CANCELLED,
                trimToNull(source),
                cancelTime
        );
        if (updated <= 0) {
            NormalOrderVO latestOrder = loadOrder(userId, id);
            if (latestOrder != null) {
                if (isCancelled(latestOrder.getOrderStatus())) {
                    return Result.success(latestOrder);
                }
                if (isPaid(latestOrder.getOrderStatus())) {
                    return Result.error(ResultCode.BUSINESS_ERROR, "订单已支付，无法取消");
                }
            }
            return Result.error(ResultCode.SERVER_ERROR, "取消订单失败，请稍后重试");
        }

        Result<Void> restoreResult = restoreNormalOrderStock(order);
        if (!isSuccess(restoreResult)) {
            normalOrderMapper.updateOrderStatus(id, userId, STATUS_CANCELLED, STATUS_CREATED, null, null);
            String message = restoreResult == null ? "恢复库存失败" : restoreResult.getMessage();
            log.error("{}后恢复普通商品库存失败，orderId={}, orderNo={}, message={}", source, id, order.getOrderNo(), message);
            return Result.error(ResultCode.SERVER_ERROR, "取消订单失败，请稍后重试");
        }

        NormalOrderVO cancelledOrder = loadOrder(userId, id);
        if (cancelledOrder == null) {
            return Result.error(ResultCode.SERVER_ERROR, "取消成功，但查询订单失败");
        }

        log.info("{}成功，userId={}, orderId={}, orderNo={}", source, userId, id, cancelledOrder.getOrderNo());
        return Result.success(cancelledOrder);
    }

    private Result<Void> restoreNormalOrderStock(NormalOrderVO order) {
        if (order == null || CollectionUtils.isEmpty(order.getItems())) {
            return Result.success();
        }

        InternalRestoreNormalOrderStockRequestDTO requestDTO = new InternalRestoreNormalOrderStockRequestDTO();
        requestDTO.setUserId(order.getUserId());
        requestDTO.setOrderNo(order.getOrderNo());
        requestDTO.setItems(order.getItems().stream().map(item -> {
            InternalRestoreNormalOrderStockItemDTO itemDTO = new InternalRestoreNormalOrderStockItemDTO();
            itemDTO.setProductId(item.getProductId());
            itemDTO.setQuantity(item.getQuantity());
            return itemDTO;
        }).toList());

        try {
            return productInternalClient.restoreNormalOrderStock(requestDTO);
        } catch (Exception ex) {
            log.error("调用商品服务恢复普通商品库存失败，orderId={}, orderNo={}", order.getId(), order.getOrderNo(), ex);
            return Result.error(ResultCode.SERVER_ERROR, "恢复库存失败");
        }
    }

    private NormalOrderVO loadOrder(Long userId, Long id) {
        NormalOrderVO order = normalOrderMapper.getOrderDetail(userId, id);
        if (order == null) {
            return null;
        }
        return fillOrderDetail(order);
    }

    private NormalOrderVO fillOrderDetail(NormalOrderVO order) {
        fillAddressInfo(order);
        order.setItems(normalOrderItemMapper.listByOrderId(order.getId()));
        return order;
    }

    private void fillAddressInfo(NormalOrderVO order) {
        if (order == null || !StringUtils.hasText(order.getAddressSnapshot())) {
            return;
        }
        try {
            var addressNode = AddressUtils.parseSnapshot(order.getAddressSnapshot(), objectMapper);
            order.setReceiver(AddressUtils.trimToNull(addressNode.path("receiver").asText(null)));
            order.setMobile(AddressUtils.trimToNull(addressNode.path("mobile").asText(null)));
            order.setDetail(AddressUtils.trimToNull(addressNode.path("detail").asText(null)));
        } catch (Exception ex) {
            log.warn("解析订单地址快照失败，orderId={}", order.getId(), ex);
        }
    }

    private boolean isCreated(Integer status) {
        return status != null && status == STATUS_CREATED;
    }

    private boolean isPaid(Integer status) {
        return status != null && status == STATUS_PAID;
    }

    private boolean isCancelled(Integer status) {
        return status != null && status == STATUS_CANCELLED;
    }

    private boolean isSuccess(Result<?> result) {
        return result != null && result.getCode() == ResultCode.SUCCESS.getCode();
    }

    private String buildCancelledMessage(NormalOrderVO order) {
        if (order == null || !StringUtils.hasText(order.getCancelReason())) {
            return "订单已取消";
        }
        if (order.getCancelReason().contains("超时")) {
            return "订单已超时关闭";
        }
        return "订单已取消";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
