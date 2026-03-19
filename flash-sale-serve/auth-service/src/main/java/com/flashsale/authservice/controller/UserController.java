package com.flashsale.authservice.controller;

import com.flashsale.authservice.domain.dto.UserDTO;
import com.flashsale.authservice.domain.vo.UserVO;
import com.flashsale.authservice.service.UserService;
import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<UserVO> login(@RequestBody UserDTO userDTO) {
        return userService.login(userDTO);
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody UserDTO userDTO) {
        log.info("用户注册: {}", userDTO.getUsername());
        return userService.register(userDTO);
    }

    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success("logout success");
    }

    @GetMapping("/me")
    public Result<UserVO> meByGet(@RequestHeader("X-User-Id") Long userId) {
        return getCurrentUser(userId);
    }

    @PostMapping("/updatePassword")
    public Result<Void> updatePassword(@RequestHeader("X-User-Id") Long userId,
                                       @RequestBody UserDTO userDTO) {
        return userService.updatePassword(userId, userDTO);
    }

    private Result<UserVO> getCurrentUser(Long userId) {
        log.info("获取当前用户信息: {}", userId);
        UserVO userVO = userService.getUserInfo(userId);
        if (userVO == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "用户不存在");
        }
        return Result.success(userVO);
    }
}
