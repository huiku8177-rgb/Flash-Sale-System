package com.flashsale.orderservice.service;

import com.flashsale.common.domain.Result;
import com.flashsale.orderservice.domain.vo.SeckillOrderVO;
import com.flashsale.orderservice.mq.message.SeckillMessage;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillOrderService
 * @date 2026/3/13 17:00
 */
public interface SeckillOrderService {

    Result<List<SeckillOrderVO>> listOrders();

    Result<SeckillOrderVO> getOrderDetail(Long id);

    void createSeckillOrder(SeckillMessage message);
}
