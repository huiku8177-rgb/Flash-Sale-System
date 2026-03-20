package com.flashsale.orderservice.controller;

import com.flashsale.common.domain.Result;
import com.flashsale.orderservice.domain.vo.SeckillOrderPayStatusVO;
import com.flashsale.orderservice.domain.vo.SeckillOrderVO;
import com.flashsale.orderservice.service.SeckillOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * @author strive_qin
 * @version 1.0
 * @description OrderController
 * @date 2026/3/20 00:00
 */


@Tag(name = "秒杀订单", description = "秒杀订单查询与支付接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {

    private final SeckillOrderService seckillOrderService;

    /**
     * 查询秒杀订单列表
     *
     * @param userId 用户ID
     * @return 秒杀订单列表
     */
    @Operation(summary = "查询秒杀订单列表")
    @GetMapping("/seckill-orders")
    public Result<List<SeckillOrderVO>> listSeckillOrders(@Parameter(hidden = true)
                                                          @RequestHeader("X-User-Id") Long userId) {
        return seckillOrderService.listOrders(userId);
    }

    /**
     * 查询秒杀订单详情
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 秒杀订单详情
     */
    @Operation(summary = "查询秒杀订单详情")
    @GetMapping("/seckill-orders/{id}")
    public Result<SeckillOrderVO> getSeckillOrderDetail(@Parameter(hidden = true)
                                                        @RequestHeader("X-User-Id") Long userId,
                                                        @Parameter(description = "秒杀订单ID", example = "50001")
                                                        @PathVariable("id") @Min(1) Long id) {
        return seckillOrderService.getOrderDetail(userId, id);
    }

    /**
     * 模拟支付秒杀订单
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 支付后的订单详情
     */
    @Operation(summary = "模拟支付秒杀订单")
    @PostMapping("/seckill-orders/{id}/pay")
    public Result<SeckillOrderVO> mockPay(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                          @Parameter(description = "秒杀订单ID", example = "50001")
                                          @PathVariable("id") @Min(1) Long id) {
        return seckillOrderService.mockPay(userId, id);
    }

    /**
     * 查询秒杀订单支付状态
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 支付状态
     */
    @Operation(summary = "查询秒杀订单支付状态")
    @GetMapping("/seckill-orders/{id}/pay-status")
    public Result<SeckillOrderPayStatusVO> getSeckillPayStatus(@Parameter(hidden = true)
                                                               @RequestHeader("X-User-Id") Long userId,
                                                               @Parameter(description = "秒杀订单ID", example = "50001")
                                                               @PathVariable("id") @Min(1) Long id) {
        return seckillOrderService.getPayStatus(userId, id);
    }
}
