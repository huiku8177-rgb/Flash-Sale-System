package com.flashsale.authservice.service.impl;

import com.flashsale.authservice.config.JwtProperties;
import com.flashsale.authservice.domain.dto.LoginRequestDTO;
import com.flashsale.authservice.domain.dto.RegisterRequestDTO;
import com.flashsale.authservice.domain.dto.UpdatePasswordRequestDTO;
import com.flashsale.authservice.domain.po.User;
import com.flashsale.authservice.domain.vo.UserVO;
import com.flashsale.authservice.mapper.UserMapper;
import com.flashsale.authservice.service.UserService;
import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.common.util.JwtTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
/**
 * @author strive_qin
 * @version 1.0
 * @description UserServiceImpl
 * @date 2026/3/20 00:00
 */


@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtTool jwtTool;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户登录并签发 JWT 令牌
     *
     * @param requestDTO 登录参数
     * @return 登录结果
     */
    @Override
    public Result<UserVO> login(LoginRequestDTO requestDTO) {
        // 先校验用户名和密码是否为空
        if (!hasCredential(requestDTO.getUsername(), requestDTO.getPassword())) {
            return Result.error(ResultCode.PARAM_ERROR, "用户名或密码不能为空");
        }

        log.info("用户登录：{}", requestDTO.getUsername());

        // 查询用户并校验密码
        User user = userMapper.findByUsername(requestDTO.getUsername());
        if (user == null || !passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        // 登录成功后生成令牌并返回用户信息
        String token = jwtTool.createToken(user.getId(), jwtProperties.getTokenTTL());
        UserVO userVO = new UserVO();
        userVO.setUserId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setToken(token);
        return Result.success(userVO);
    }

    /**
     * 注册新用户
     *
     * @param requestDTO 注册参数
     * @return 注册结果
     */
    @Override
    public Result<Void> register(RegisterRequestDTO requestDTO) {
        // 用户名和密码是注册最基础的必填项
        if (!hasCredential(requestDTO.getUsername(), requestDTO.getPassword())) {
            return Result.error(ResultCode.PARAM_ERROR, "用户名或密码不能为空");
        }

        log.info("用户注册：{}", requestDTO.getUsername());

        // 用户名已存在时直接返回业务错误
        User oldUser = userMapper.findByUsername(requestDTO.getUsername());
        if (oldUser != null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "用户名已存在");
        }

        // 密码统一加密后再入库
        User user = new User();
        user.setUsername(requestDTO.getUsername());
        user.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        userMapper.insert(user);
        return Result.success();
    }

    /**
     * 根据用户ID查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @Override
    public UserVO getUserInfo(Long userId) {
        if (userId == null) {
            return null;
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            return null;
        }

        UserVO userVO = new UserVO();
        userVO.setUserId(user.getId());
        userVO.setUsername(user.getUsername());
        return userVO;
    }

    /**
     * 修改当前登录用户密码
     *
     * @param userId 用户ID
     * @param requestDTO 修改密码参数
     * @return 修改结果
     */
    @Override
    public Result<Void> updatePassword(Long userId, UpdatePasswordRequestDTO requestDTO) {
        // 先校验登录态和请求体
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        if (requestDTO == null) {
            return Result.error(ResultCode.PARAM_ERROR, "请求参数不能为空");
        }
        if (!StringUtils.hasText(requestDTO.getOldPassword())) {
            return Result.error(ResultCode.PARAM_ERROR, "旧密码不能为空");
        }
        if (!StringUtils.hasText(requestDTO.getNewPassword())) {
            return Result.error(ResultCode.PARAM_ERROR, "新密码不能为空");
        }

        // 查询当前用户并校验旧密码
        User user = userMapper.findById(userId);
        if (user == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "用户不存在");
        }
        if (!passwordEncoder.matches(requestDTO.getOldPassword(), user.getPassword())) {
            return Result.error(ResultCode.BUSINESS_ERROR, "旧密码错误");
        }
        if (passwordEncoder.matches(requestDTO.getNewPassword(), user.getPassword())) {
            return Result.error(ResultCode.BUSINESS_ERROR, "新密码不能与旧密码相同");
        }

        // 新密码加密后覆盖旧密码
        user.setPassword(passwordEncoder.encode(requestDTO.getNewPassword()));
        userMapper.updateUser(user);
        log.info("用户修改密码：userId={}", userId);
        return Result.success();
    }

    // 用户名和密码同时有值时才视为有效凭证
    private boolean hasCredential(String username, String password) {
        return StringUtils.hasText(username) && StringUtils.hasText(password);
    }
}
