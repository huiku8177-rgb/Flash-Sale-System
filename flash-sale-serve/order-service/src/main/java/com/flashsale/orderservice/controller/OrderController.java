package com.flashsale.orderservice.controller;

import com.flashsale.common.domain.Result;
import com.flashsale.orderservice.domain.vo.SeckillOrderPayStatusVO;
import com.flashsale.orderservice.domain.vo.SeckillOrderVO;
import com.flashsale.orderservice.service.SeckillOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description OrderController
 * @date 2026/3/13 15:20
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {

    private final SeckillOrderService seckillOrderService;

    @GetMapping("/orders")
    public Result<List<SeckillOrderVO>> listOrders(@RequestHeader("X-User-Id") Long userId) {
        return seckillOrderService.listOrders(userId);
    }

    @GetMapping("/orderDetail/{id}")
    public Result<SeckillOrderVO> getOrderDetail(@RequestHeader("X-User-Id") Long userId,
                                                 @PathVariable Long id) {
        return seckillOrderService.getOrderDetail(userId, id);
    }

    @PostMapping("/seckill-orders/{id}/pay")
    public Result<SeckillOrderVO> mockPay(@RequestHeader("X-User-Id") Long userId,
                                          @PathVariable("id") Long id) {
        return seckillOrderService.mockPay(userId, id);
    }

    @GetMapping("/seckill-pay-status/{id}")
    public Result<SeckillOrderPayStatusVO> getPayStatus(@RequestHeader("X-User-Id") Long userId,
                                                        @PathVariable("id") Long id) {
        return seckillOrderService.getPayStatus(userId, id);
    }
}
