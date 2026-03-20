package com.flashsale.orderservice.service;

import com.flashsale.common.domain.Result;
import com.flashsale.orderservice.domain.vo.SeckillOrderVO;
import com.flashsale.orderservice.domain.vo.SeckillOrderPayStatusVO;
import com.flashsale.orderservice.mq.message.SeckillMessage;

import java.util.List;

/**
 * 秒杀订单服务接口
 *
 * @author strive_qin
 * @version 1.0
 * @description SeckillOrderService
 * @date 2026/3/13 17:00
 */
public interface SeckillOrderService {

    /**
     * 查询秒杀订单列表
     *
     * @param userId 用户ID
     * @return 秒杀订单列表
     */
    Result<List<SeckillOrderVO>> listOrders(Long userId);

    /**
     * 查询秒杀订单详情
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 秒杀订单详情
     */
    Result<SeckillOrderVO> getOrderDetail(Long userId, Long id);

    /**
     * 模拟支付秒杀订单
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 支付后的订单详情
     */
    Result<SeckillOrderVO> mockPay(Long userId, Long id);

    /**
     * 查询秒杀订单支付状态
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 支付状态
     */
    Result<SeckillOrderPayStatusVO> getPayStatus(Long userId, Long id);

    /**
     * 消费消息并创建秒杀订单
     *
     * @param message 秒杀消息
     */
    void createSeckillOrder(SeckillMessage message);

    /**
     * 处理死信消息并执行失败补偿
     *
     * @param message 秒杀消息
     */
    void handleSeckillFailure(SeckillMessage message);
}
