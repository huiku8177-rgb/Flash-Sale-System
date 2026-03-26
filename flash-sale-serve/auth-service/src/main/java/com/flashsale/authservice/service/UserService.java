package com.flashsale.authservice.service;

import com.flashsale.authservice.domain.dto.LoginRequestDTO;
import com.flashsale.authservice.domain.dto.RegisterRequestDTO;
import com.flashsale.authservice.domain.dto.UpdatePasswordRequestDTO;
import com.flashsale.authservice.domain.vo.UserVO;

/**
 * @author strive_qin
 * @version 1.0
 * @description UserService
 * @date 2026/3/20 00:00
 */
public interface UserService {

    UserVO login(LoginRequestDTO requestDTO);

    void register(RegisterRequestDTO requestDTO);

    UserVO getUserInfo(Long userId);

    void updatePassword(Long userId, UpdatePasswordRequestDTO requestDTO);
}
