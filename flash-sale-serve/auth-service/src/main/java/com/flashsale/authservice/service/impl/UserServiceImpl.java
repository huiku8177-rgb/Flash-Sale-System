package com.flashsale.authservice.service.impl;

import com.flashsale.authservice.config.JwtProperties;
import com.flashsale.authservice.domain.dto.UserDTO;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtTool jwtTool;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Result<UserVO> login(UserDTO userDTO) {
        if (!isValidCredential(userDTO)) {
            return Result.error(ResultCode.PARAM_ERROR, "用户名或密码不能为空");
        }

        log.info("用户登录: {}", userDTO.getUsername());
        User user = userMapper.findByUsername(userDTO.getUsername());
        if (user == null || !passwordEncoder.matches(userDTO.getPassword(), user.getPassword())) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        String token = jwtTool.createToken(user.getId(), jwtProperties.getTokenTTL());
        UserVO userVO = new UserVO();
        userVO.setUserId(user.getId().intValue());
        userVO.setUsername(user.getUsername());
        userVO.setToken(token);
        return Result.success(userVO);
    }

    @Override
    public Result<Void> register(UserDTO userDTO) {
        if (!isValidCredential(userDTO)) {
            return Result.error(ResultCode.PARAM_ERROR, "用户名或密码不能为空");
        }

        log.info("用户注册: {}", userDTO.getUsername());
        User oldUser = userMapper.findByUsername(userDTO.getUsername());
        if (oldUser != null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "用户名已存在");
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        userMapper.insert(user);
        return Result.success();
    }

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
        userVO.setUserId(user.getId().intValue());
        userVO.setUsername(user.getUsername());
        return userVO;
    }

    @Override
    public Result<Void> updatePassword(Long userId, UserDTO userDTO) {
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        if (userDTO == null) {
            return Result.error(ResultCode.PARAM_ERROR, "请求参数不能为空");
        }
        if (!StringUtils.hasText(userDTO.getOldPassword())) {
            return Result.error(ResultCode.PARAM_ERROR, "旧密码不能为空");
        }
        if (!StringUtils.hasText(userDTO.getNewPassword())) {
            return Result.error(ResultCode.PARAM_ERROR, "新密码不能为空");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "用户不存在");
        }
        if (!passwordEncoder.matches(userDTO.getOldPassword(), user.getPassword())) {
            return Result.error(ResultCode.BUSINESS_ERROR, "旧密码错误");
        }
        if (passwordEncoder.matches(userDTO.getNewPassword(), user.getPassword())) {
            return Result.error(ResultCode.BUSINESS_ERROR, "新密码不能与旧密码相同");
        }

        // 只允许基于当前登录用户和正确旧密码更新密码，避免越权或误改。
        user.setPassword(passwordEncoder.encode(userDTO.getNewPassword()));
        userMapper.updateUser(user);
        log.info("用户修改密码: userId={}", userId);
        return Result.success();
    }

    private boolean isValidCredential(UserDTO userDTO) {
        return userDTO != null
                && StringUtils.hasText(userDTO.getUsername())
                && StringUtils.hasText(userDTO.getPassword());
    }
}
