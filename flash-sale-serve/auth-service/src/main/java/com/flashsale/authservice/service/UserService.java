package com.flashsale.authservice.service;

import com.flashsale.authservice.domain.dto.LoginRequestDTO;
import com.flashsale.authservice.domain.dto.RegisterRequestDTO;
import com.flashsale.authservice.domain.dto.UpdatePasswordRequestDTO;
import com.flashsale.authservice.domain.vo.UserVO;

public interface UserService {

    UserVO login(LoginRequestDTO requestDTO);

    void register(RegisterRequestDTO requestDTO);

    UserVO getUserInfo(Long userId);

    void logout(Long userId, String authorization);

    void updatePassword(Long userId, UpdatePasswordRequestDTO requestDTO);
}
