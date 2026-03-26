package com.flashsale.orderservice.controller.internal;

import com.flashsale.common.domain.Result;
import com.flashsale.orderservice.domain.dto.internal.InternalCreateNormalOrderRequestDTO;
import com.flashsale.orderservice.domain.vo.NormalOrderVO;
import com.flashsale.orderservice.service.NormalOrderService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@Validated
@RestController
@RequestMapping("/internal/orders/normal")
@RequiredArgsConstructor
@Slf4j
public class InternalNormalOrderController {

    private final NormalOrderService normalOrderService;

    @PostMapping
    public Result<NormalOrderVO> createOrder(@Valid @RequestBody InternalCreateNormalOrderRequestDTO requestDTO) {
        log.info("internal create normal order request received, userId={}, orderNo={}",
                requestDTO.getUserId(), requestDTO.getOrderNo());
        return normalOrderService.createOrder(requestDTO);
    }

    @GetMapping("/by-order-no")
    public Result<NormalOrderVO> getOrderByOrderNo(@RequestParam("userId") Long userId,
                                                   @RequestParam("orderNo") String orderNo) {
        log.info("internal query normal order by orderNo, userId={}, orderNo={}", userId, orderNo);
        return normalOrderService.getOrderByOrderNo(userId, orderNo);
    }
}
