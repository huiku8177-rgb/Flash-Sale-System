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
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<UserVO> login(@RequestBody UserDTO userDTO){
        return userService.login(userDTO);
    }
    @PostMapping("/register")
    public Result register(@RequestBody UserDTO userDTO){
        log.info("用户注册: {}", userDTO.getUsername());
        userService.register(userDTO);
        return Result.success();
    }

}
