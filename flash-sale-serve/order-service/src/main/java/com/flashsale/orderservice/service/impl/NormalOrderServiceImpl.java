package com.flashsale.orderservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.orderservice.domain.dto.internal.InternalCreateNormalOrderItemDTO;
import com.flashsale.orderservice.domain.dto.internal.InternalCreateNormalOrderRequestDTO;
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

    private final NormalOrderMapper normalOrderMapper;
    private final NormalOrderItemMapper normalOrderItemMapper;
    private final ObjectMapper objectMapper;

    /**
     * 根据商品服务传入的快照数据创建普通订单
     *
     * @param requestDTO 内部建单参数
     * @return 订单结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<NormalOrderVO> createOrder(InternalCreateNormalOrderRequestDTO requestDTO) {
        // 基础参数校验
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

        // 按订单号做幂等校验，避免重复建单
        Result<NormalOrderVO> existingOrderResult = getOrderByOrderNo(requestDTO.getUserId(), requestDTO.getOrderNo());
        if (existingOrderResult.getCode() == ResultCode.SUCCESS.getCode() && existingOrderResult.getData() != null) {
            return existingOrderResult;
        }

        // 地址快照统一校验为合法 JSON
        String addressSnapshot = normalizeAddressSnapshot(requestDTO.getAddressSnapshot());
        if (StringUtils.hasText(requestDTO.getAddressSnapshot()) && addressSnapshot == null) {
            return Result.error(ResultCode.PARAM_ERROR, "地址快照必须是合法的JSON");
        }

        BigDecimal computedTotalAmount = BigDecimal.ZERO;
        List<NormalOrderItemPO> itemPOList = new ArrayList<>();

        // 校验订单商品并组装明细数据
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

        // 校验订单总金额和实付金额
        if (requestDTO.getTotalAmount() == null || requestDTO.getPayAmount() == null) {
            return Result.error(ResultCode.PARAM_ERROR, "订单金额不能为空");
        }
        if (computedTotalAmount.compareTo(requestDTO.getTotalAmount()) != 0
                || requestDTO.getTotalAmount().compareTo(requestDTO.getPayAmount()) != 0) {
            return Result.error(ResultCode.PARAM_ERROR, "订单总金额不匹配");
        }

        // 先创建订单主表，再批量插入订单明细
        NormalOrderPO orderPO = new NormalOrderPO();
        orderPO.setOrderNo(requestDTO.getOrderNo().trim());
        orderPO.setUserId(requestDTO.getUserId());
        orderPO.setOrderStatus(STATUS_CREATED);
        orderPO.setTotalAmount(requestDTO.getTotalAmount());
        orderPO.setPayAmount(requestDTO.getPayAmount());
        orderPO.setRemark(trimToNull(requestDTO.getRemark()));
        orderPO.setAddressSnapshot(addressSnapshot);
        normalOrderMapper.insert(orderPO);

        for (NormalOrderItemPO itemPO : itemPOList) {
            itemPO.setOrderId(orderPO.getId());
        }
        normalOrderItemMapper.insertBatch(itemPOList);

        NormalOrderVO orderVO = normalOrderMapper.getOrderDetail(requestDTO.getUserId(), orderPO.getId());
        if (orderVO == null) {
            return Result.error(ResultCode.SERVER_ERROR, "普通订单创建成功，但查询详情失败");
        }
        orderVO.setItems(normalOrderItemMapper.listByOrderId(orderPO.getId()));

        log.info("普通订单创建成功，userId={}, orderId={}, orderNo={}",
                requestDTO.getUserId(), orderPO.getId(), orderPO.getOrderNo());
        return Result.success(orderVO);
    }

    /**
     * 按订单号查询普通订单
     *
     * @param userId 用户ID
     * @param orderNo 订单号
     * @return 订单结果
     */
    @Override
    public Result<NormalOrderVO> getOrderByOrderNo(Long userId, String orderNo) {
        if (userId == null || !StringUtils.hasText(orderNo)) {
            return Result.error(ResultCode.PARAM_ERROR, "用户ID和订单号不能为空");
        }

        NormalOrderVO order = normalOrderMapper.getOrderDetailByOrderNo(userId, orderNo.trim());
        if (order == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "普通订单不存在");
        }
        order.setItems(normalOrderItemMapper.listByOrderId(order.getId()));
        return Result.success(order);
    }

    /**
     * 查询普通订单列表并补齐订单项
     *
     * @param userId 用户ID
     * @param orderStatus 订单状态
     * @return 订单列表
     */
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

        // 订单项按订单ID分组，便于回填到每个订单对象
        Map<Long, List<NormalOrderItemVO>> itemMap = items.stream()
                .collect(Collectors.groupingBy(NormalOrderItemVO::getOrderId));

        for (NormalOrderVO order : orders) {
            order.setItems(itemMap.getOrDefault(order.getId(), Collections.emptyList()));
        }

        return Result.success(orders);
    }

    /**
     * 查询普通订单详情
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 订单详情
     */
    @Override
    public Result<NormalOrderVO> getOrderDetail(Long userId, Long id) {
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

        order.setItems(normalOrderItemMapper.listByOrderId(id));
        return Result.success(order);
    }

    /**
     * 模拟支付普通订单
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 支付后的订单详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<NormalOrderVO> mockPay(Long userId, Long id) {
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

        if (order.getOrderStatus() != null && order.getOrderStatus() == STATUS_PAID) {
            order.setItems(normalOrderItemMapper.listByOrderId(id));
            return Result.success(order);
        }
        if (order.getOrderStatus() != null && order.getOrderStatus() != STATUS_CREATED) {
            return Result.error(ResultCode.BUSINESS_ERROR, "当前订单状态不允许支付");
        }

        // 仅允许待支付订单更新为已支付
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
            return Result.error(ResultCode.BUSINESS_ERROR, "普通订单支付失败，请刷新后重试");
        }

        NormalOrderVO paidOrder = normalOrderMapper.getOrderDetail(userId, id);
        if (paidOrder == null) {
            return Result.error(ResultCode.SERVER_ERROR, "支付成功，但查询订单失败");
        }
        paidOrder.setItems(normalOrderItemMapper.listByOrderId(id));

        return Result.success(paidOrder);
    }

    /**
     * 查询普通订单支付状态
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 支付状态
     */
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

        boolean paid = order.getOrderStatus() != null && order.getOrderStatus() == STATUS_PAID;
        payStatusVO.setPaid(paid);
        payStatusVO.setMessage(paid ? "订单已支付" : "订单待支付");

        return Result.success(payStatusVO);
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
