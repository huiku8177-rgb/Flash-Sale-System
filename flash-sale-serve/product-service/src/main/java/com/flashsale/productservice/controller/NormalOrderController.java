package com.flashsale.productservice.controller;

import com.flashsale.common.domain.Result;
import com.flashsale.productservice.domain.dto.NormalOrderCheckoutDTO;
import com.flashsale.productservice.domain.vo.NormalOrderVO;
import com.flashsale.productservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author strive_qin
 * @version 1.0
 * @description NormalOrderController
 * @date 2026/3/20 00:00
 */
@Tag(name = "普通订单", description = "普通订单创建接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
@Slf4j
public class NormalOrderController {

    private final ProductService productService;

    /**
     * 基于当前用户已勾选的购物车商品创建普通订单。
     *
     * @param userId 用户ID
     * @param checkoutDTO 订单信息
     * @return 订单信息
     */
    @Operation(summary = "创建普通订单", description = "基于当前用户已勾选的购物车商品创建普通订单。")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "创建成功",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "createNormalOrderSuccess",
                                    value = "{\"code\":200,\"message\":\"成功\",\"data\":{\"id\":30001,\"orderNo\":\"202603200001\",\"userId\":10001,\"orderStatus\":0,\"totalAmount\":199.00,\"payAmount\":199.00,\"payTime\":null,\"remark\":\"请尽快发货\",\"addressSnapshot\":\"{\\\"receiver\\\":\\\"小曾\\\"}\",\"createTime\":\"2026-03-20T17:30:00\",\"items\":[{\"id\":31001,\"orderId\":30001,\"userId\":10001,\"productId\":1001,\"productName\":\"旗舰手机\",\"productSubtitle\":\"12GB+256GB\",\"productImage\":\"https://cdn.example.com/product/1001.png\",\"salePrice\":99.50,\"quantity\":2,\"itemAmount\":199.00,\"createTime\":\"2026-03-20T17:30:00\"}]},\"timestamp\":\"2026-03-20T17:30:00\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "请求参数校验失败"),
            @ApiResponse(responseCode = "401", description = "未登录或令牌无效")
    })
    @PostMapping("/normal-orders")
    public Result<NormalOrderVO> createOrder(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                             @Valid @RequestBody NormalOrderCheckoutDTO checkoutDTO) {
        log.info("用户 {} 通过商品服务基于已选购物车商品创建普通订单", userId);
        return productService.createNormalOrder(userId, checkoutDTO);
    }
}
