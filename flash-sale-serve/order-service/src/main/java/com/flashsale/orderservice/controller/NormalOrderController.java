package com.flashsale.orderservice.controller;

import com.flashsale.common.domain.Result;
import com.flashsale.orderservice.domain.vo.NormalOrderPayStatusVO;
import com.flashsale.orderservice.domain.vo.NormalOrderVO;
import com.flashsale.orderservice.service.NormalOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * @author strive_qin
 * @version 1.0
 * @description NormalOrderController
 * @date 2026/3/20 00:00
 */


@Tag(name = "普通订单", description = "普通订单查询与支付接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class NormalOrderController {

    private final NormalOrderService normalOrderService;

    /**
     * 查询普通订单列表
     *
     * @param userId 用户ID
     * @param status 订单状态筛选
     * @return 订单列表
     */
    @Operation(summary = "查询普通订单列表")
    @GetMapping("/normal-orders")
    public Result<List<NormalOrderVO>> listOrders(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                                  @Parameter(description = "可选的订单状态筛选", example = "0")
                                                  @RequestParam(value = "status", required = false) Integer status) {
        return normalOrderService.listOrders(userId, status);
    }

    /**
     * 查询普通订单详情
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 订单详情
     */
    @Operation(summary = "查询普通订单详情")
    @GetMapping("/normal-orders/{id}")
    public Result<NormalOrderVO> getOrderDetail(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                                @Parameter(description = "普通订单ID", example = "30001")
                                                @PathVariable("id") @Min(value = 1, message = "订单ID必须大于等于1") Long id) {
        return normalOrderService.getOrderDetail(userId, id);
    }

    /**
     * 模拟支付普通订单
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 支付后的订单详情
     */
    @Operation(summary = "模拟支付普通订单")
    @PostMapping("/normal-orders/{id}/pay")
    public Result<NormalOrderVO> mockPay(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                         @Parameter(description = "普通订单ID", example = "30001")
                                         @PathVariable("id") @Min(value = 1, message = "订单ID必须大于等于1") Long id) {
        return normalOrderService.mockPay(userId, id);
    }

    /**
     * 查询普通订单支付状态
     *
     * @param userId 用户ID
     * @param id 订单ID
     * @return 支付状态
     */
    @Operation(summary = "查询普通订单支付状态")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "查询成功",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "normalPayStatusSuccess",
                                            value = "{\"code\":200,\"message\":\"成功\",\"data\":{\"orderId\":30001,\"orderNo\":\"202603200001\",\"orderStatus\":1,\"paid\":true,\"payAmount\":199.00,\"payTime\":\"2026-03-20T18:00:00\",\"message\":\"订单已支付\"},\"timestamp\":\"2026-03-20T18:00:00\"}"
                                    ),
                                    @ExampleObject(
                                            name = "normalPayStatusBusinessError",
                                            value = "{\"code\":2003,\"message\":\"普通订单不存在\",\"data\":null,\"timestamp\":\"2026-03-20T18:00:01\"}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "路径参数校验失败",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "normalPayStatusInvalidId",
                                    value = "{\"code\":400,\"message\":\"getPayStatus.id必须大于等于1\",\"data\":null,\"timestamp\":\"2026-03-20T17:59:59\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "未登录或令牌无效")
    })
    @GetMapping("/normal-orders/{id}/pay-status")
    public Result<NormalOrderPayStatusVO> getPayStatus(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                                       @Parameter(description = "普通订单ID", example = "30001")
                                                       @PathVariable("id") @Min(value = 1, message = "订单ID必须大于等于1") Long id) {
        return normalOrderService.getPayStatus(userId, id);
    }
}
