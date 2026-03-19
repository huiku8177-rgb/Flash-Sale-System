package com.flashsale.orderservice.controller;

import com.flashsale.common.domain.Result;
import com.flashsale.orderservice.domain.dto.NormalOrderCheckoutDTO;
import com.flashsale.orderservice.domain.vo.NormalOrderPayStatusVO;
import com.flashsale.orderservice.domain.vo.NormalOrderVO;
import com.flashsale.orderservice.service.NormalOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 普通商品订单接口。
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Slf4j
public class NormalOrderController {

    private final NormalOrderService normalOrderService;

    /**
     * 普通商品结算下单。
     */
    @PostMapping("/checkout")
    public Result<NormalOrderVO> checkout(@RequestHeader("X-User-Id") Long userId,
                                          @RequestBody NormalOrderCheckoutDTO checkoutDTO) {
        log.info("用户 {} 发起普通商品结算，下单项数量={}",
                userId,
                checkoutDTO == null || checkoutDTO.getItems() == null ? 0 : checkoutDTO.getItems().size());
        return normalOrderService.checkout(userId, checkoutDTO);
    }

    /**
     * 查询当前用户普通订单列表。
     */
    @GetMapping("/normal-orders")
    public Result<List<NormalOrderVO>> listOrders(@RequestHeader("X-User-Id") Long userId,
                                                  @RequestParam(value = "status", required = false) Integer status) {
        return normalOrderService.listOrders(userId, status);
    }

    /**
     * 查询当前用户普通订单详情。
     */
    @GetMapping("/normal-orders/{id}")
    public Result<NormalOrderVO> getOrderDetail(@RequestHeader("X-User-Id") Long userId,
                                                @PathVariable("id") Long id) {
        return normalOrderService.getOrderDetail(userId, id);
    }

    /**
     * 模拟普通订单支付，便于后端接口先闭环。
     */
    @PostMapping("/normal-orders/{id}/pay")
    public Result<NormalOrderVO> mockPay(@RequestHeader("X-User-Id") Long userId,
                                         @PathVariable("id") Long id) {
        return normalOrderService.mockPay(userId, id);
    }

    /**
     * 查询普通订单支付状态。
     */
    @GetMapping("/pay-status/{id}")
    public Result<NormalOrderPayStatusVO> getPayStatus(@RequestHeader("X-User-Id") Long userId,
                                                       @PathVariable("id") Long id) {
        return normalOrderService.getPayStatus(userId, id);
    }
}
