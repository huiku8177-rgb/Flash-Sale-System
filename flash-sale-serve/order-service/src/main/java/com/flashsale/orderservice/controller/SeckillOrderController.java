package com.flashsale.orderservice.controller;

import com.flashsale.common.domain.Result;
import com.flashsale.orderservice.domain.vo.SeckillOrderVO;
import com.flashsale.orderservice.service.SeckillOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillOrderController
 * @date 2026/3/13 17:00
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/seckill-order")
public class SeckillOrderController {

    private final SeckillOrderService seckillOrderService;

    @GetMapping("/list")
    public Result<List<SeckillOrderVO>> listOrders() {
        return seckillOrderService.listOrders();
    }

    @GetMapping("/{id}")
    public Result<SeckillOrderVO> getOrderDetail(@PathVariable Long id) {
        return seckillOrderService.getOrderDetail(id);
    }
}
