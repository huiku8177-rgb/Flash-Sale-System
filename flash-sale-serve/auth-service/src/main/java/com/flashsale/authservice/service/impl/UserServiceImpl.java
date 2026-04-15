package com.flashsale.authservice.service.impl;

import com.flashsale.authservice.config.JwtProperties;
import com.flashsale.authservice.domain.dto.LoginRequestDTO;
import com.flashsale.authservice.domain.dto.RegisterRequestDTO;
import com.flashsale.authservice.domain.dto.UpdatePasswordRequestDTO;
import com.flashsale.authservice.domain.po.User;
import com.flashsale.authservice.domain.vo.UserVO;
import com.flashsale.authservice.mapper.UserMapper;
import com.flashsale.authservice.service.AuthSessionService;
import com.flashsale.authservice.service.UserService;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.common.exception.CommonException;
import com.flashsale.common.exception.UnauthorizedException;
import com.flashsale.common.util.JwtTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtTool jwtTool;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;

    @Override
    public UserVO login(LoginRequestDTO requestDTO) {
        if (requestDTO == null || !hasCredential(requestDTO.getUsername(), requestDTO.getPassword())) {
            throw paramError("用户名或密码不能为空");
        }

        String username = normalizeUsername(requestDTO.getUsername());
        log.info("用户登录: {}", username);

        User user = userMapper.findByUsername(username);
        if (user == null || !passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
            throw unauthorized("用户名或密码错误");
        }

        long tokenVersion = authSessionService.getCurrentTokenVersion(user.getId());
        UserVO userVO = buildUserVO(user);
        userVO.setToken(jwtTool.createToken(user.getId(), jwtProperties.getTokenTTL(), tokenVersion));
        return userVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequestDTO requestDTO) {
        if (requestDTO == null || !hasCredential(requestDTO.getUsername(), requestDTO.getPassword())) {
            throw paramError("用户名或密码不能为空");
        }

        String username = normalizeUsername(requestDTO.getUsername());
        log.info("用户注册: {}", username);

        User oldUser = userMapper.findByUsername(username);
        if (oldUser != null) {
            throw businessError("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(requestDTO.getPassword()));

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            log.warn("注册并发冲突，用户名已存在 {}", username);
            throw businessError("用户名已存在");
        }
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        requireAuthenticated(userId);

        User user = userMapper.findById(userId);
        if (user == null) {
            throw businessError("用户不存在");
        }

        return buildUserVO(user);
    }

    @Override
    public void logout(Long userId, String authorization) {
        requireAuthenticated(userId);
        authSessionService.blacklistToken(userId, authorization, jwtProperties.getTokenTTL());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, UpdatePasswordRequestDTO requestDTO) {
        requireAuthenticated(userId);
        if (requestDTO == null) {
            throw paramError("请求参数不能为空");
        }
        if (!StringUtils.hasText(requestDTO.getOldPassword())) {
            throw paramError("旧密码不能为空");
        }
        if (!StringUtils.hasText(requestDTO.getNewPassword())) {
            throw paramError("新密码不能为空");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            throw businessError("用户不存在");
        }
        if (!passwordEncoder.matches(requestDTO.getOldPassword(), user.getPassword())) {
            throw businessError("旧密码错误");
        }
        if (passwordEncoder.matches(requestDTO.getNewPassword(), user.getPassword())) {
            throw businessError("新密码不能与旧密码相同");
        }

        user.setPassword(passwordEncoder.encode(requestDTO.getNewPassword()));
        userMapper.updateUser(user);
        authSessionService.incrementTokenVersion(userId);
        log.info("用户修改密码: userId={}", userId);
    }

    private UserVO buildUserVO(User user) {
        UserVO userVO = new UserVO();
        userVO.setUserId(user.getId());
        userVO.setUsername(user.getUsername());
        return userVO;
    }

    private void requireAuthenticated(Long userId) {
        if (userId == null) {
            throw unauthorized("未登录或登录已失效");
        }
    }

    private boolean hasCredential(String username, String password) {
        return StringUtils.hasText(normalizeUsername(username)) && StringUtils.hasText(password);
    }

    private String normalizeUsername(String username) {
        return StringUtils.hasText(username) ? username.trim() : null;
    }

    private CommonException paramError(String message) {
        return new CommonException(message, ResultCode.PARAM_ERROR.getCode());
    }

    private CommonException businessError(String message) {
        return new CommonException(message, ResultCode.BUSINESS_ERROR.getCode());
    }

    private UnauthorizedException unauthorized(String message) {
        return new UnauthorizedException(message);
    }
}
