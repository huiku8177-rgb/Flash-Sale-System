package com.flashsale.authservice.service;

import com.flashsale.authservice.domain.dto.UserAddressCreateDTO;
import com.flashsale.authservice.domain.dto.UserAddressUpdateDTO;
import com.flashsale.authservice.domain.vo.UserAddressVO;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description UserAddressService
 * @date 2026/3/23 00:00
 */
public interface UserAddressService {

    List<UserAddressVO> listAddresses(Long userId);

    UserAddressVO getAddressDetail(Long userId, Long id);

    UserAddressVO createAddress(Long userId, UserAddressCreateDTO requestDTO);

    UserAddressVO updateAddress(Long userId, Long id, UserAddressUpdateDTO requestDTO);

    void deleteAddress(Long userId, Long id);

    UserAddressVO setDefaultAddress(Long userId, Long id);
}
