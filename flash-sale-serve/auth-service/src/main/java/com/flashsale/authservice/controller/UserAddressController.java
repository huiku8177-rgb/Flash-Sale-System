package com.flashsale.authservice.controller;

import com.flashsale.authservice.domain.dto.UserAddressCreateDTO;
import com.flashsale.authservice.domain.dto.UserAddressUpdateDTO;
import com.flashsale.authservice.domain.vo.UserAddressVO;
import com.flashsale.authservice.service.UserAddressService;
import com.flashsale.common.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "收货地址管理", description = "用户收货地址增删改查接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/auth/addresses")
@RequiredArgsConstructor
@Slf4j
public class UserAddressController {

    private final UserAddressService userAddressService;

    @Operation(summary = "查询当前用户地址列表")
    @GetMapping
    public Result<List<UserAddressVO>> listAddresses(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        log.info("list user addresses, userId={}", userId);
        return Result.success(userAddressService.listAddresses(userId));
    }

    @Operation(summary = "查询收货地址详情")
    @GetMapping("/{id}")
    public Result<UserAddressVO> getAddressDetail(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                                  @PathVariable("id") @Min(value = 1, message = "地址ID必须大于等于1") Long id) {
        log.info("get address detail, userId={}, addressId={}", userId, id);
        return Result.success(userAddressService.getAddressDetail(userId, id));
    }

    @Operation(summary = "新增收货地址")
    @PostMapping
    public Result<UserAddressVO> createAddress(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                               @Valid @RequestBody UserAddressCreateDTO requestDTO) {
        log.info("create address request received, userId={}", userId);
        return Result.success(userAddressService.createAddress(userId, requestDTO));
    }

    @Operation(summary = "修改收货地址")
    @PutMapping("/{id}")
    public Result<UserAddressVO> updateAddress(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                               @PathVariable("id") @Min(value = 1, message = "地址ID必须大于等于1") Long id,
                                               @Valid @RequestBody UserAddressUpdateDTO requestDTO) {
        log.info("update address request received, userId={}, addressId={}", userId, id);
        return Result.success(userAddressService.updateAddress(userId, id, requestDTO));
    }

    @Operation(summary = "删除收货地址")
    @DeleteMapping("/{id}")
    public Result<Void> deleteAddress(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                      @PathVariable("id") @Min(value = 1, message = "地址ID必须大于等于1") Long id) {
        log.info("delete address request received, userId={}, addressId={}", userId, id);
        userAddressService.deleteAddress(userId, id);
        return Result.success();
    }

    @Operation(summary = "设置默认收货地址")
    @PutMapping("/{id}/default")
    public Result<UserAddressVO> setDefaultAddress(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                                   @PathVariable("id") @Min(value = 1, message = "地址ID必须大于等于1") Long id) {
        log.info("set default address request received, userId={}, addressId={}", userId, id);
        return Result.success(userAddressService.setDefaultAddress(userId, id));
    }
}
