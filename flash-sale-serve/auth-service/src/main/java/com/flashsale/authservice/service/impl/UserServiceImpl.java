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

/**
 * @author strive_qin
 * @version 1.0
 * @description UserServiceImpl
 * @date 2026/3/12 10:25
 */
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
        log.info("用户登录: {}", userDTO.getUsername());

        User user = userMapper.findByUsername(userDTO.getUsername());
        if (user == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        if (!passwordEncoder.matches(userDTO.getPassword(), user.getPassword())) {
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
    public void register(UserDTO userDTO) {
        log.info("用户注册: {}", userDTO.getUsername());

        User oldUser = userMapper.findByUsername(userDTO.getUsername());
        if (oldUser != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        userMapper.insert(user);
    }
}