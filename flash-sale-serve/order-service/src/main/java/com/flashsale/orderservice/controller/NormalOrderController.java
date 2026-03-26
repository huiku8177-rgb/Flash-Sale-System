package com.flashsale.orderservice.controller;

import com.flashsale.common.domain.Result;
import com.flashsale.orderservice.domain.vo.NormalOrderPayStatusVO;
import com.flashsale.orderservice.domain.vo.NormalOrderVO;
import com.flashsale.orderservice.service.NormalOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "普通订单", description = "普通订单查询、取消与支付接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Slf4j
public class NormalOrderController {

    private final NormalOrderService normalOrderService;

    @Operation(summary = "查询普通订单列表")
    @GetMapping("/normal-orders")
    public Result<List<NormalOrderVO>> listOrders(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                                  @Parameter(description = "可选的订单状态筛选", example = "0")
                                                  @RequestParam(value = "status", required = false) Integer status) {
        log.info("list normal orders request received, userId={}, status={}", userId, status);
        return normalOrderService.listOrders(userId, status);
    }

    @Operation(summary = "查询普通订单详情")
    @GetMapping("/normal-orders/{id}")
    public Result<NormalOrderVO> getOrderDetail(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                                @Parameter(description = "普通订单ID", example = "30001")
                                                @PathVariable("id") @Min(value = 1, message = "订单ID必须大于等于1") Long id) {
        log.info("get normal order detail request received, userId={}, orderId={}", userId, id);
        return normalOrderService.getOrderDetail(userId, id);
    }

    @Operation(summary = "模拟支付普通订单")
    @PostMapping("/normal-orders/{id}/pay")
    public Result<NormalOrderVO> mockPay(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                         @Parameter(description = "普通订单ID", example = "30001")
                                         @PathVariable("id") @Min(value = 1, message = "订单ID必须大于等于1") Long id) {
        log.info("mock pay normal order request received, userId={}, orderId={}", userId, id);
        return normalOrderService.mockPay(userId, id);
    }

    @Operation(summary = "取消普通订单")
    @PostMapping("/normal-orders/{id}/cancel")
    public Result<NormalOrderVO> cancelOrder(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                             @Parameter(description = "普通订单ID", example = "30001")
                                             @PathVariable("id") @Min(value = 1, message = "订单ID必须大于等于1") Long id) {
        log.info("cancel normal order request received, userId={}, orderId={}", userId, id);
        return normalOrderService.cancelOrder(userId, id);
    }

    @Operation(summary = "查询普通订单支付状态")
    @GetMapping("/normal-orders/{id}/pay-status")
    public Result<NormalOrderPayStatusVO> getPayStatus(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                                       @Parameter(description = "普通订单ID", example = "30001")
                                                       @PathVariable("id") @Min(value = 1, message = "订单ID必须大于等于1") Long id) {
        log.info("get normal order pay status request received, userId={}, orderId={}", userId, id);
        return normalOrderService.getPayStatus(userId, id);
    }
}
