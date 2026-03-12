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

    /** 用户数据访问层：负责用户查询/新增。 */
    private final UserMapper userMapper;
    /** JWT 工具：负责 token 签发。 */
    private final JwtTool jwtTool;
    /** JWT 配置：读取 tokenTTL 等配置项。 */
    private final JwtProperties jwtProperties;
    /** 密码编码器：BCrypt 加密与校验。 */
    private final PasswordEncoder passwordEncoder;


    @Override
    public Result<UserVO> login(UserDTO userDTO) {
        log.info("用户登录: {}", userDTO.getUsername());

        // 1) 按用户名查询账户
        User user = userMapper.findByUsername(userDTO.getUsername());
        if (user == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        // 2) 使用 BCrypt 校验密码
        if (!passwordEncoder.matches(userDTO.getPassword(), user.getPassword())) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        // 3) 签发 JWT token
        String token = jwtTool.createToken(user.getId(), jwtProperties.getTokenTTL());

        // 4) 组装返回对象
        UserVO userVO = new UserVO();
        userVO.setUserId(user.getId().intValue());
        userVO.setUsername(user.getUsername());
        userVO.setToken(token);

        return Result.success(userVO);
    }

    @Override
    public void register(UserDTO userDTO) {
        log.info("用户注册: {}", userDTO.getUsername());

        // 1) 用户名唯一性校验
        User oldUser = userMapper.findByUsername(userDTO.getUsername());
        if (oldUser != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 2) 密码加密后入库
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        userMapper.insert(user);
    }
}
