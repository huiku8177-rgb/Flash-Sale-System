package com.flashsale.authservice.controller;

import com.flashsale.authservice.domain.dto.UserDTO;
import com.flashsale.authservice.domain.vo.UserVO;
import com.flashsale.authservice.service.UserService;
import com.flashsale.common.domain.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author strive_qin
 * @version 1.0
 * @description UserController
 * @date 2026/3/11 16:44
 */
@RestController
@RequestMapping("/auth")
@Slf4j
public class UserController {
    /**
     * 认证业务入口：聚合登录/注册接口。
     *
     * 说明：Controller 只做参数接收与返回封装，核心逻辑下沉至 UserService。
     */
    @Autowired
    private UserService userService;

    /**
     * 用户登录
     * @param userDTO
     * @return
     */
    @PostMapping("/login")
    public Result<UserVO> login(@RequestBody UserDTO userDTO){
        // 登录成功后返回 userId、username 与 JWT token
        return userService.login(userDTO);
    }
    /**
     * 用户注册
     * @param userDTO
     * @return
     */
    @PostMapping("/register")
    public Result register(@RequestBody UserDTO userDTO){
        log.info("用户注册: {}", userDTO.getUsername());
        // 注册流程：用户名唯一性校验 + 密码加密后入库（在 service 完成）
        userService.register(userDTO);
        return Result.success();
    }
    /**
     * 用户登出
     * @return
     */
    //TODO 用户登出
    @PostMapping("/logout")
    public Result logout(){
        return Result.success();
    }
    /**
     * 获取当前用户信息
     * @return
     */
    //TODO 获取当前用户信息
    @PostMapping("/me")
    public Result<UserVO> me(){
        return Result.success();
    }

}
