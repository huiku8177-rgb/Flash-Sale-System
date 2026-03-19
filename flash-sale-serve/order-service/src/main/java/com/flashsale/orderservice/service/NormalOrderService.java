package com.flashsale.orderservice.service;

import com.flashsale.common.domain.Result;
import com.flashsale.orderservice.domain.dto.NormalOrderCheckoutDTO;
import com.flashsale.orderservice.domain.vo.NormalOrderPayStatusVO;
import com.flashsale.orderservice.domain.vo.NormalOrderVO;

import java.util.List;

/**
 * 普通商品订单服务。
 */
public interface NormalOrderService {

    Result<NormalOrderVO> checkout(Long userId, NormalOrderCheckoutDTO checkoutDTO);

    Result<List<NormalOrderVO>> listOrders(Long userId, Integer orderStatus);

    Result<NormalOrderVO> getOrderDetail(Long userId, Long id);

    Result<NormalOrderVO> mockPay(Long userId, Long id);

    Result<NormalOrderPayStatusVO> getPayStatus(Long userId, Long id);
}
