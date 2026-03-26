package com.flashsale.authservice.service.impl;

import com.flashsale.authservice.domain.dto.UserAddressCreateDTO;
import com.flashsale.authservice.domain.dto.UserAddressUpdateDTO;
import com.flashsale.authservice.domain.po.UserAddressPO;
import com.flashsale.authservice.domain.vo.UserAddressVO;
import com.flashsale.authservice.mapper.UserAddressMapper;
import com.flashsale.authservice.service.UserAddressService;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.common.exception.CommonException;
import com.flashsale.common.exception.UnauthorizedException;
import com.flashsale.common.util.AddressUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description UserAddressServiceImpl
 * @date 2026/3/23 00:00
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private static final int DEFAULT_TRUE = 1;
    private static final int DEFAULT_FALSE = 0;

    private final UserAddressMapper userAddressMapper;

    @Override
    public List<UserAddressVO> listAddresses(Long userId) {
        requireAuthenticated(userId);
        return userAddressMapper.listByUserId(userId);
    }

    @Override
    public UserAddressVO getAddressDetail(Long userId, Long id) {
        requireAuthenticated(userId);
        requireAddressId(id);

        UserAddressVO address = userAddressMapper.getDetailByIdAndUserId(id, userId);
        if (address == null) {
            throw businessError("收货地址不存在");
        }
        return address;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAddressVO createAddress(Long userId, UserAddressCreateDTO requestDTO) {
        requireAuthenticated(userId);
        if (requestDTO == null) {
            throw paramError("请求参数不能为空");
        }

        String receiver = AddressUtils.trimToNull(requestDTO.getReceiver());
        String mobile = AddressUtils.trimToNull(requestDTO.getMobile());
        String detail = AddressUtils.trimToNull(requestDTO.getDetail());
        validateAddressFields(receiver, mobile, detail);

        UserAddressPO address = new UserAddressPO();
        address.setUserId(userId);
        address.setReceiver(receiver);
        address.setMobile(mobile);
        address.setDetail(detail);

        boolean shouldSetDefault = shouldSetDefaultOnCreate(userId, requestDTO.getIsDefault());
        if (shouldSetDefault) {
            userAddressMapper.clearDefaultByUserId(userId);
        }
        address.setIsDefault(shouldSetDefault ? DEFAULT_TRUE : DEFAULT_FALSE);

        userAddressMapper.insert(address);
        log.info("新增收货地址成功: userId={}, addressId={}", userId, address.getId());
        return loadAddressVO(userId, address.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAddressVO updateAddress(Long userId, Long id, UserAddressUpdateDTO requestDTO) {
        requireAuthenticated(userId);
        requireAddressId(id);
        if (requestDTO == null) {
            throw paramError("请求参数不能为空");
        }

        UserAddressPO existing = userAddressMapper.findByIdAndUserId(id, userId);
        if (existing == null) {
            throw businessError("收货地址不存在");
        }

        String receiver = AddressUtils.trimToNull(requestDTO.getReceiver());
        String mobile = AddressUtils.trimToNull(requestDTO.getMobile());
        String detail = AddressUtils.trimToNull(requestDTO.getDetail());
        validateAddressFields(receiver, mobile, detail);

        existing.setReceiver(receiver);
        existing.setMobile(mobile);
        existing.setDetail(detail);

        if (Boolean.TRUE.equals(requestDTO.getIsDefault())) {
            userAddressMapper.clearDefaultByUserId(userId);
            existing.setIsDefault(DEFAULT_TRUE);
        } else if (existing.getIsDefault() == null || existing.getIsDefault() != DEFAULT_TRUE) {
            existing.setIsDefault(DEFAULT_FALSE);
        }

        userAddressMapper.update(existing);
        log.info("修改收货地址成功: userId={}, addressId={}", userId, id);
        return loadAddressVO(userId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Long userId, Long id) {
        requireAuthenticated(userId);
        requireAddressId(id);

        UserAddressPO existing = userAddressMapper.findByIdAndUserId(id, userId);
        if (existing == null) {
            throw businessError("收货地址不存在");
        }

        userAddressMapper.markDeleted(id, userId);

        if (existing.getIsDefault() != null && existing.getIsDefault() == DEFAULT_TRUE) {
            List<UserAddressVO> remainingAddresses = userAddressMapper.listByUserId(userId);
            if (!remainingAddresses.isEmpty()) {
                UserAddressVO nextAddress = remainingAddresses.get(0);
                userAddressMapper.clearDefaultByUserId(userId);

                UserAddressPO nextDefault = new UserAddressPO();
                nextDefault.setId(nextAddress.getId());
                nextDefault.setUserId(userId);
                nextDefault.setReceiver(nextAddress.getReceiver());
                nextDefault.setMobile(nextAddress.getMobile());
                nextDefault.setDetail(nextAddress.getDetail());
                nextDefault.setIsDefault(DEFAULT_TRUE);
                userAddressMapper.update(nextDefault);
            }
        }

        log.info("删除收货地址成功: userId={}, addressId={}", userId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAddressVO setDefaultAddress(Long userId, Long id) {
        requireAuthenticated(userId);
        requireAddressId(id);

        UserAddressPO existing = userAddressMapper.findByIdAndUserId(id, userId);
        if (existing == null) {
            throw businessError("收货地址不存在");
        }

        userAddressMapper.clearDefaultByUserId(userId);
        existing.setIsDefault(DEFAULT_TRUE);
        userAddressMapper.update(existing);
        log.info("设置默认收货地址成功: userId={}, addressId={}", userId, id);
        return loadAddressVO(userId, id);
    }

    private boolean shouldSetDefaultOnCreate(Long userId, Boolean isDefault) {
        if (Boolean.TRUE.equals(isDefault)) {
            return true;
        }
        return userAddressMapper.findDefaultByUserId(userId) == null;
    }

    private UserAddressVO loadAddressVO(Long userId, Long addressId) {
        UserAddressVO address = userAddressMapper.getDetailByIdAndUserId(addressId, userId);
        if (address == null) {
            throw new IllegalStateException("收货地址保存成功，但重新查询失败，addressId=" + addressId);
        }
        return address;
    }

    private void validateAddressFields(String receiver, String mobile, String detail) {
        if (!AddressUtils.hasRequiredFields(receiver, mobile, detail)) {
            throw paramError("收货人、手机号和收货地址不能为空");
        }
        if (!AddressUtils.isMobileValid(mobile)) {
            throw paramError("手机号格式不正确");
        }
    }

    private void requireAuthenticated(Long userId) {
        if (userId == null) {
            throw new UnauthorizedException("未登录或登录已失效");
        }
    }

    private void requireAddressId(Long id) {
        if (id == null) {
            throw paramError("地址ID不能为空");
        }
    }

    private CommonException paramError(String message) {
        return new CommonException(message, ResultCode.PARAM_ERROR.getCode());
    }

    private CommonException businessError(String message) {
        return new CommonException(message, ResultCode.BUSINESS_ERROR.getCode());
    }
}
