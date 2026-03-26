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

@Tag(name = "普通订单", description = "普通商品结算与下单接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
@Slf4j
public class NormalOrderController {

    private final ProductService productService;

    @Operation(summary = "创建普通订单", description = "基于当前用户已勾选的购物车商品和已保存地址创建普通订单。")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "创建成功",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "createNormalOrderSuccess",
                                    value = "{\"code\":200,\"message\":\"成功\",\"data\":{\"id\":30001,\"orderNo\":\"202603200001\"},\"timestamp\":\"2026-03-20T17:30:00\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "请求参数校验失败"),
            @ApiResponse(responseCode = "401", description = "未登录或令牌无效")
    })
    @PostMapping("/normal-orders")
    public Result<NormalOrderVO> createOrder(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                             @Valid @RequestBody NormalOrderCheckoutDTO checkoutDTO) {
        log.info("create normal order request received, userId={}", userId);
        Result<NormalOrderVO> result = productService.createNormalOrder(userId, checkoutDTO);
        if (result != null && result.getData() != null) {
            log.info("create normal order completed, userId={}, orderNo={}", userId, result.getData().getOrderNo());
        }
        return result;
    }
}
