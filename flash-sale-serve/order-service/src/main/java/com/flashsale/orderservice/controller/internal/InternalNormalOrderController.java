package com.flashsale.orderservice.controller.internal;

import com.flashsale.common.domain.Result;
import com.flashsale.orderservice.domain.dto.internal.InternalCreateNormalOrderRequestDTO;
import com.flashsale.orderservice.domain.vo.NormalOrderVO;
import com.flashsale.orderservice.service.NormalOrderService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/**
 * @author strive_qin
 * @version 1.0
 * @description InternalNormalOrderController
 * @date 2026/3/20 00:00
 */


@Hidden
@Validated
@RestController
@RequestMapping("/internal/orders/normal")
@RequiredArgsConstructor
public class InternalNormalOrderController {

    private final NormalOrderService normalOrderService;

    /**
     * 供商品服务调用的内部建单接口
     *
     * @param requestDTO 内部建单参数
     * @return 订单结果
     */
    @PostMapping
    public Result<NormalOrderVO> createOrder(@Valid @RequestBody InternalCreateNormalOrderRequestDTO requestDTO) {
        return normalOrderService.createOrder(requestDTO);
    }

    /**
     * 供商品服务按订单号补查订单
     *
     * @param userId 用户ID
     * @param orderNo 订单号
     * @return 订单结果
     */
    @GetMapping("/by-order-no")
    public Result<NormalOrderVO> getOrderByOrderNo(@RequestParam("userId") Long userId,
                                                   @RequestParam("orderNo") String orderNo) {
        return normalOrderService.getOrderByOrderNo(userId, orderNo);
    }
}
