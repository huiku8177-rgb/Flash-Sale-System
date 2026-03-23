package com.flashsale.authservice.service;

import com.flashsale.authservice.domain.dto.UserAddressCreateDTO;
import com.flashsale.authservice.domain.dto.UserAddressUpdateDTO;
import com.flashsale.authservice.domain.vo.UserAddressVO;
import com.flashsale.common.domain.Result;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description UserAddressService
 * @date 2026/3/23 00:00
 */
public interface UserAddressService {

    Result<List<UserAddressVO>> listAddresses(Long userId);

    Result<UserAddressVO> getAddressDetail(Long userId, Long id);

    Result<UserAddressVO> createAddress(Long userId, UserAddressCreateDTO requestDTO);

    Result<UserAddressVO> updateAddress(Long userId, Long id, UserAddressUpdateDTO requestDTO);

    Result<Void> deleteAddress(Long userId, Long id);

    Result<UserAddressVO> setDefaultAddress(Long userId, Long id);
}
