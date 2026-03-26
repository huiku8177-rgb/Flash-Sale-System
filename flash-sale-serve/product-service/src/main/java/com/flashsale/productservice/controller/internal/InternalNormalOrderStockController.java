package com.flashsale.productservice.controller.internal;

import com.flashsale.common.domain.Result;
import com.flashsale.productservice.domain.dto.internal.InternalRestoreNormalOrderStockRequestDTO;
import com.flashsale.productservice.service.ProductOrderLocalTxService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Hidden
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/products/normal-orders")
public class InternalNormalOrderStockController {

    private final ProductOrderLocalTxService productOrderLocalTxService;

    @PostMapping("/restore-stock")
    public Result<Void> restoreStock(@Valid @RequestBody InternalRestoreNormalOrderStockRequestDTO requestDTO) {
        log.info("restore normal order stock request received, userId={}, orderNo={}",
                requestDTO.getUserId(), requestDTO.getOrderNo());
        Map<Long, Integer> mergedItems = new LinkedHashMap<>();
        requestDTO.getItems().forEach(item ->
                mergedItems.merge(item.getProductId(), item.getQuantity(), Integer::sum)
        );

        productOrderLocalTxService.restoreStock(mergedItems);
        log.info("restore normal order stock completed, userId={}, orderNo={}, productCount={}",
                requestDTO.getUserId(), requestDTO.getOrderNo(), mergedItems.size());
        return Result.success();
    }
}
