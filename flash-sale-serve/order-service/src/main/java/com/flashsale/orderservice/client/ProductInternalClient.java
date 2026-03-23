package com.flashsale.orderservice.client;

import com.flashsale.common.domain.Result;
import com.flashsale.orderservice.domain.dto.internal.InternalRestoreNormalOrderStockRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author strive_qin
 * @version 1.0
 * @description ProductInternalClient
 * @date 2026/3/23 00:00
 */
@FeignClient(name = "product-service")
public interface ProductInternalClient {

    @PostMapping("/internal/products/normal-orders/restore-stock")
    Result<Void> restoreNormalOrderStock(@RequestBody InternalRestoreNormalOrderStockRequestDTO requestDTO);
}
