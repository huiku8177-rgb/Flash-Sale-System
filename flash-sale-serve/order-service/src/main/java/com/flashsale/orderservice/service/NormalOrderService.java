package com.flashsale.orderservice.service;

import com.flashsale.common.domain.Result;
import com.flashsale.orderservice.domain.dto.internal.InternalCreateNormalOrderRequestDTO;
import com.flashsale.orderservice.domain.vo.NormalOrderPayStatusVO;
import com.flashsale.orderservice.domain.vo.NormalOrderVO;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description NormalOrderService
 * @date 2026/3/20 00:00
 */
public interface NormalOrderService {

    /**
     * 创建普通订单
     *
     * @param requestDTO 内部建单参数
     * @return 订单结果
     */
    Result<NormalOrderVO> createOrder(InternalCreateNormalOrderRequestDTO requestDTO);

    /**
     * 按订单号查询普通订单
     *
     * @param userId 用户ID
     * @param orderNo 订单号
     * @return 订单结果
     */
    Result<NormalOrderVO> getOrderByOrderNo(Long userId, String orderNo);

    /**
     * 查询普通订单列表
     *
     * @param userId 用户ID
     * @param orderStatus 订单状态
     * @return 订单列表
     */
    Result<List<NormalOrderVO>> listOrders(Long userId, Integer orderStatus);

    /**
     * 查询普通订单详情
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 订单详情
     */
    Result<NormalOrderVO> getOrderDetail(Long userId, Long id);

    /**
     * 模拟支付普通订单
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 支付后的订单详情
     */
    Result<NormalOrderVO> mockPay(Long userId, Long id);

    /**
     * 取消待支付普通订单
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 取消后的订单详情
     */
    Result<NormalOrderVO> cancelOrder(Long userId, Long id);

    /**
     * 查询普通订单支付状态
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 支付状态
     */
    Result<NormalOrderPayStatusVO> getPayStatus(Long userId, Long id);

    /**
     * 定时取消超时未支付订单
     *
     * @return 本次成功取消的订单数量
     */
    int cancelTimeoutOrders();
}
