package com.flashsale.productservice.client;

import com.flashsale.common.domain.Result;
import com.flashsale.productservice.domain.vo.UserAddressVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * @author strive_qin
 * @version 1.0
 * @description AuthAddressClient
 * @date 2026/3/23 00:00
 */
@FeignClient(name = "auth-service")
public interface AuthAddressClient {

    @GetMapping("/auth/addresses/{id}")
    Result<UserAddressVO> getAddressDetail(@RequestHeader("X-User-Id") Long userId,
                                           @PathVariable("id") Long id);
}
