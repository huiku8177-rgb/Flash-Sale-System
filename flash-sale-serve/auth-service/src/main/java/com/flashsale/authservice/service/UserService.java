package com.flashsale.authservice.service;

import com.flashsale.authservice.domain.dto.UserDTO;
import com.flashsale.authservice.domain.vo.UserVO;
import com.flashsale.common.domain.Result;

public interface UserService {

    Result<UserVO> login(UserDTO userDTO);

    Result<Void> register(UserDTO userDTO);

    UserVO getUserInfo(Long userId);

    Result<Void> updatePassword(Long userId, UserDTO userDTO);
}
