package com.flashsale.orderservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.orderservice.domain.dto.NormalOrderCheckoutDTO;
import com.flashsale.orderservice.domain.dto.NormalOrderItemRequestDTO;
import com.flashsale.orderservice.domain.po.NormalOrderItemPO;
import com.flashsale.orderservice.domain.po.NormalOrderPO;
import com.flashsale.orderservice.domain.po.NormalProductPO;
import com.flashsale.orderservice.domain.vo.NormalOrderItemVO;
import com.flashsale.orderservice.domain.vo.NormalOrderPayStatusVO;
import com.flashsale.orderservice.domain.vo.NormalOrderVO;
import com.flashsale.orderservice.mapper.NormalOrderItemMapper;
import com.flashsale.orderservice.mapper.NormalOrderMapper;
import com.flashsale.orderservice.mapper.NormalProductMapper;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 普通订单服务实现类
 *
 * 职责：
 * 1. 普通商品下单结算
 * 2. 订单列表查询
 * 3. 订单详情查询
 * 4. 模拟支付
 * 5. 支付状态查询
 *
 * 下单核心流程：
 * 1. 校验用户与请求参数
 * 2. 合并重复商品项
 * 3. 校验商品状态与库存
 * 4. 计算订单金额并构建订单明细
 * 5. 扣减库存
 * 6. 创建订单主表和明细表
 *
 * 事务说明：
 * - checkout 和 mockPay 使用事务控制
 * - 保证库存扣减、订单创建、支付状态更新具备原子性
 *
 * 注意：
 * - 库存扣减依赖数据库层的原子更新逻辑，避免超卖
 * - 订单明细保存的是下单时的商品快照，避免后续商品信息变更影响历史订单
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NormalOrderServiceImpl implements NormalOrderService {

    /**
     * 订单状态：待支付
     */
    private static final int STATUS_CREATED = 0;

    /**
     * 订单状态：已支付
     */
    private static final int STATUS_PAID = 1;

    private final NormalOrderMapper normalOrderMapper;
    private final NormalOrderItemMapper normalOrderItemMapper;
    private final NormalProductMapper normalProductMapper;
    private final ObjectMapper objectMapper;

    /**
     * 提交订单（结算）
     *
     * 核心流程：
     * 1. 校验用户登录状态
     * 2. 校验下单商品参数
     * 3. 合并重复商品
     * 4. 校验商品是否存在、是否上架、库存是否充足
     * 5. 计算订单总金额
     * 6. 扣减库存
     * 7. 创建订单主表与订单明细表
     * 8. 返回完整订单详情
     *
     * 并发说明：
     * - 库存扣减必须依赖数据库条件更新
     * - 扣减失败时抛出异常，触发事务回滚
     *
     * @param userId 当前登录用户ID
     * @param checkoutDTO 结算请求参数
     * @return 创建后的订单详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<NormalOrderVO> checkout(Long userId, NormalOrderCheckoutDTO checkoutDTO) {
        // 1. 校验登录状态
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        // 2. 校验结算参数，至少要有一件商品
        if (checkoutDTO == null || CollectionUtils.isEmpty(checkoutDTO.getItems())) {
            return Result.error(ResultCode.PARAM_ERROR, "请至少选择一件商品");
        }

        // 3. 合并重复商品项，过滤非法商品项
        Map<Long, Integer> mergedItems = mergeItems(checkoutDTO.getItems());
        if (mergedItems.isEmpty()) {
            return Result.error(ResultCode.PARAM_ERROR, "商品参数不合法");
        }

        // 4. 规范化地址快照；如果传了值，则必须是合法 JSON
        String addressSnapshot = normalizeAddressSnapshot(checkoutDTO.getAddressSnapshot());
        if (StringUtils.hasText(checkoutDTO.getAddressSnapshot()) && addressSnapshot == null) {
            return Result.error(ResultCode.PARAM_ERROR, "addressSnapshot 必须是合法 JSON");
        }

        // 5. 批量查询商品信息
        List<Long> productIds = new ArrayList<>(mergedItems.keySet());
        List<NormalProductPO> products = normalProductMapper.listByIds(productIds);
        if (CollectionUtils.isEmpty(products) || products.size() != productIds.size()) {
            return Result.error(ResultCode.BUSINESS_ERROR, "存在不可下单的商品");
        }

        // 6. 转为 Map，便于按商品ID快速查找
        Map<Long, NormalProductPO> productMap = products.stream()
                .collect(Collectors.toMap(NormalProductPO::getId, product -> product));

        // 7. 构建订单明细，并计算总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<NormalOrderItemPO> itemPOList = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : mergedItems.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            NormalProductPO product = productMap.get(productId);

            // 商品不存在
            if (product == null) {
                return Result.error(ResultCode.BUSINESS_ERROR, "商品不存在");
            }

            // 商品必须为上架状态
            if (product.getStatus() == null || product.getStatus() != 1) {
                return Result.error(ResultCode.BUSINESS_ERROR, "商品已下架: " + product.getName());
            }

            // 库存必须充足
            if (product.getStock() == null || product.getStock() < quantity) {
                return Result.error(ResultCode.STOCK_EMPTY, "库存不足: " + product.getName());
            }

            // 计算当前商品项金额
            BigDecimal itemAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(itemAmount);

            // 保存商品快照，避免商品后续改名、改图、改价格影响历史订单展示
            NormalOrderItemPO itemPO = new NormalOrderItemPO();
            itemPO.setUserId(userId);
            itemPO.setProductId(productId);
            itemPO.setProductName(product.getName());
            itemPO.setProductSubtitle(product.getSubtitle());
            itemPO.setProductImage(product.getMainImage());
            itemPO.setSalePrice(product.getPrice());
            itemPO.setQuantity(quantity);
            itemPO.setItemAmount(itemAmount);
            itemPOList.add(itemPO);
        }

        // 8. 扣减库存
        // 这里要求 Mapper 层使用条件更新，避免并发下超卖
        for (Map.Entry<Long, Integer> entry : mergedItems.entrySet()) {
            int updated = normalProductMapper.decreaseStock(entry.getKey(), entry.getValue());
            if (updated <= 0) {
                throw new IllegalStateException("扣减普通商品库存失败，productId=" + entry.getKey());
            }
        }

        // 9. 创建订单主表
        NormalOrderPO orderPO = new NormalOrderPO();
        orderPO.setOrderNo(generateOrderNo());
        orderPO.setUserId(userId);
        orderPO.setOrderStatus(STATUS_CREATED);
        orderPO.setTotalAmount(totalAmount);
        orderPO.setPayAmount(totalAmount);
        orderPO.setRemark(trimToNull(checkoutDTO.getRemark()));
        orderPO.setAddressSnapshot(addressSnapshot);
        normalOrderMapper.insert(orderPO);

        // 10. 创建订单明细表
        for (NormalOrderItemPO itemPO : itemPOList) {
            itemPO.setOrderId(orderPO.getId());
        }
        normalOrderItemMapper.insertBatch(itemPOList);

        // 11. 回查订单详情并返回
        NormalOrderVO orderVO = normalOrderMapper.getOrderDetail(userId, orderPO.getId());
        if (orderVO == null) {
            return Result.error(ResultCode.SERVER_ERROR, "订单创建成功但查询详情失败");
        }
        orderVO.setItems(normalOrderItemMapper.listByOrderId(orderPO.getId()));

        log.info("普通订单创建成功，userId={}, orderId={}, orderNo={}",
                userId, orderPO.getId(), orderPO.getOrderNo());

        return Result.success(orderVO);
    }

    /**
     * 查询订单列表
     *
     * 处理逻辑：
     * 1. 查询订单主表数据
     * 2. 批量查询订单明细
     * 3. 按订单ID分组回填，避免 N+1 查询
     *
     * @param userId 当前登录用户ID
     * @param orderStatus 订单状态，可为空；为空时查询全部状态
     * @return 订单列表
     */
    @Override
    public Result<List<NormalOrderVO>> listOrders(Long userId, Integer orderStatus) {
        // 校验登录状态
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        // 查询订单主表
        List<NormalOrderVO> orders = normalOrderMapper.listOrders(userId, orderStatus);
        if (CollectionUtils.isEmpty(orders)) {
            return Result.success(Collections.emptyList());
        }

        // 批量查询订单明细
        List<Long> orderIds = orders.stream().map(NormalOrderVO::getId).toList();
        List<NormalOrderItemVO> items = normalOrderItemMapper.listByOrderIds(orderIds);

        // 按 orderId 分组，便于批量回填
        Map<Long, List<NormalOrderItemVO>> itemMap = items.stream()
                .collect(Collectors.groupingBy(NormalOrderItemVO::getOrderId));

        for (NormalOrderVO order : orders) {
            order.setItems(itemMap.getOrDefault(order.getId(), Collections.emptyList()));
        }

        return Result.success(orders);
    }

    /**
     * 查询订单详情
     *
     * @param userId 当前登录用户ID
     * @param id 订单ID
     * @return 订单详情（包含订单明细）
     */
    @Override
    public Result<NormalOrderVO> getOrderDetail(Long userId, Long id) {
        // 校验登录状态
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        // 校验订单ID
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR, "订单ID不能为空");
        }

        // 查询订单主信息
        NormalOrderVO order = normalOrderMapper.getOrderDetail(userId, id);
        if (order == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "普通订单不存在");
        }

        // 查询并填充订单明细
        order.setItems(normalOrderItemMapper.listByOrderId(id));
        return Result.success(order);
    }

    /**
     * 模拟支付
     *
     * 用于开发或测试环境，不接入真实支付系统。
     *
     * 状态流转：
     * - 待支付 -> 已支付
     *
     * 处理规则：
     * 1. 订单不存在，返回错误
     * 2. 已支付订单，直接幂等返回
     * 3. 非待支付状态，不允许支付
     * 4. 更新支付状态成功后，返回最新订单信息
     *
     * @param userId 当前登录用户ID
     * @param id 订单ID
     * @return 支付后的订单详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<NormalOrderVO> mockPay(Long userId, Long id) {
        // 校验登录状态
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        // 校验订单ID
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR, "订单ID不能为空");
        }

        // 查询订单
        NormalOrderVO order = normalOrderMapper.getOrderDetail(userId, id);
        if (order == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "普通订单不存在");
        }

        // 已支付则直接返回，保证接口幂等
        if (order.getOrderStatus() != null && order.getOrderStatus() == STATUS_PAID) {
            order.setItems(normalOrderItemMapper.listByOrderId(id));
            return Result.success(order);
        }

        // 只有待支付状态才允许支付
        if (order.getOrderStatus() != null && order.getOrderStatus() != STATUS_CREATED) {
            return Result.error(ResultCode.BUSINESS_ERROR, "当前订单状态不允许支付");
        }

        // 更新订单支付状态
        LocalDateTime now = LocalDateTime.now();
        int updated = normalOrderMapper.updatePayStatus(
                id,
                userId,
                STATUS_CREATED,
                STATUS_PAID,
                order.getPayAmount(),
                now
        );

        // 更新失败通常表示状态已变化，可能存在并发冲突
        if (updated <= 0) {
            return Result.error(ResultCode.BUSINESS_ERROR, "订单支付失败，请刷新后重试");
        }

        // 回查支付后的订单
        NormalOrderVO paidOrder = normalOrderMapper.getOrderDetail(userId, id);
        if (paidOrder == null) {
            return Result.error(ResultCode.SERVER_ERROR, "支付成功但查询订单失败");
        }
        paidOrder.setItems(normalOrderItemMapper.listByOrderId(id));

        return Result.success(paidOrder);
    }

    /**
     * 查询订单支付状态
     *
     * @param userId 当前登录用户ID
     * @param id 订单ID
     * @return 支付状态信息
     */
    @Override
    public Result<NormalOrderPayStatusVO> getPayStatus(Long userId, Long id) {
        // 校验登录状态
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        // 校验订单ID
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR, "订单ID不能为空");
        }

        // 查询订单
        NormalOrderVO order = normalOrderMapper.getOrderDetail(userId, id);
        if (order == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "普通订单不存在");
        }

        // 组装支付状态返回对象
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

    /**
     * 合并商品项
     *
     * 作用：
     * 1. 过滤非法商品项
     * 2. 合并相同商品的数量
     *
     * 示例：
     * - [productId=1, quantity=2]
     * - [productId=1, quantity=3]
     * 合并后：
     * - {1=5}
     *
     * @param items 前端提交的商品项列表
     * @return key=productId，value=合并后的数量
     */
    private Map<Long, Integer> mergeItems(List<NormalOrderItemRequestDTO> items) {
        Map<Long, Integer> mergedItems = new LinkedHashMap<>();

        for (NormalOrderItemRequestDTO item : items) {
            // 过滤非法参数：空对象、商品ID为空、数量为空、数量小于等于0
            if (item == null || item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }

            // 相同商品进行数量合并
            mergedItems.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        return mergedItems;
    }

    /**
     * 生成订单号
     *
     * 当前实现使用 UUID 去掉中划线生成订单号，
     * 适用于一般业务场景。
     *
     * @return 订单号
     */
    private String generateOrderNo() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 去除字符串首尾空白。
     *
     * 规则：
     * - 有效文本：返回 trim 后结果
     * - 空字符串或纯空白：返回 null
     *
     * @param value 原始字符串
     * @return 处理后的字符串或 null
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 规范化地址快照
     *
     * 逻辑：
     * 1. 去除首尾空白
     * 2. 如果为空，返回 null
     * 3. 如果不为空，尝试解析为 JSON
     * 4. 如果不是合法 JSON，记录告警日志并返回 null
     *
     * 设计目的：
     * - 防止非法地址快照入库
     * - 保证 addressSnapshot 字段格式统一
     *
     * @param addressSnapshot 地址快照 JSON 字符串
     * @return 合法 JSON 字符串；不合法则返回 null
     */
    private String normalizeAddressSnapshot(String addressSnapshot) {
        String value = trimToNull(addressSnapshot);
        if (value == null) {
            return null;
        }

        try {
            objectMapper.readTree(value);
            return value;
        } catch (JsonProcessingException e) {
            log.warn("地址快照不是合法 JSON: {}", value);
            return null;
        }
    }
}