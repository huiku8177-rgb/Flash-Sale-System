package com.flashsale.authservice.service;

import com.flashsale.authservice.domain.dto.LoginRequestDTO;
import com.flashsale.authservice.domain.dto.RegisterRequestDTO;
import com.flashsale.authservice.domain.dto.UpdatePasswordRequestDTO;
import com.flashsale.authservice.domain.vo.UserVO;
import com.flashsale.common.domain.Result;
/**
 * @author strive_qin
 * @version 1.0
 * @description UserService
 * @date 2026/3/20 00:00
 */


public interface UserService {

    /**
     * 用户登录
     *
     * @param requestDTO 登录参数
     * @return 登录结果
     */
    Result<UserVO> login(LoginRequestDTO requestDTO);

    /**
     * 用户注册
     *
     * @param requestDTO 注册参数
     * @return 注册结果
     */
    Result<Void> register(RegisterRequestDTO requestDTO);

    /**
     * 获取当前用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserVO getUserInfo(Long userId);

    /**
     * 修改密码
     *
     * @param userId 用户ID
     * @param requestDTO 修改密码参数
     * @return 修改结果
     */
    Result<Void> updatePassword(Long userId, UpdatePasswordRequestDTO requestDTO);
}
