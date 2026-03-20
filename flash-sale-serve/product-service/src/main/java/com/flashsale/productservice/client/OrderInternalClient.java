package com.flashsale.productservice.client;

import com.flashsale.common.domain.Result;
import com.flashsale.productservice.domain.dto.CreateNormalOrderRequestDTO;
import com.flashsale.productservice.domain.vo.NormalOrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
/**
 * @author strive_qin
 * @version 1.0
 * @description OrderInternalClient
 * @date 2026/3/20 00:00
 */


@FeignClient(name = "order-service")
public interface OrderInternalClient {

    @PostMapping("/internal/orders/normal")
    Result<NormalOrderVO> createNormalOrder(@RequestBody CreateNormalOrderRequestDTO requestDTO);

    @GetMapping("/internal/orders/normal/by-order-no")
    Result<NormalOrderVO> getNormalOrderByOrderNo(@RequestParam("userId") Long userId,
                                                  @RequestParam("orderNo") String orderNo);
}
