package com.flashsale.authservice.controller;

import com.flashsale.authservice.domain.dto.LoginRequestDTO;
import com.flashsale.authservice.domain.dto.RegisterRequestDTO;
import com.flashsale.authservice.domain.dto.UpdatePasswordRequestDTO;
import com.flashsale.authservice.domain.vo.UserVO;
import com.flashsale.authservice.service.UserService;
import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * @author strive_qin
 * @version 1.0
 * @description UserController
 * @date 2026/3/20 00:00
 */


@Tag(name = "认证管理", description = "登录、注册与账户相关接口")
@Validated
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 用户登录
     *
     * @param requestDTO 登录参数
     * @return 登录结果
     */
    @Operation(summary = "登录", description = "使用用户名和密码登录，成功后返回 JWT 令牌。")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "登录处理完成",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "登录成功",
                                            value = "{\"code\":200,\"message\":\"成功\",\"data\":{\"userId\":10001,\"username\":\"neo\",\"token\":\"eyJhbGciOiJIUzI1NiJ9...\"},\"timestamp\":\"2026-03-20T16:30:00\"}"
                                    ),
                                    @ExampleObject(
                                            name = "登录失败",
                                            value = "{\"code\":401,\"message\":\"未登录或登录已失效\",\"data\":null,\"timestamp\":\"2026-03-20T16:31:00\"}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "请求参数校验失败",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "请求体不合法",
                                    value = "{\"code\":400,\"message\":\"参数校验失败\",\"data\":null,\"timestamp\":\"2026-03-20T16:29:50\"}"
                            )
                    )
            )
    })
    @PostMapping("/login")
    public Result<UserVO> login(@Valid @RequestBody LoginRequestDTO requestDTO) {
        return userService.login(requestDTO);
    }

    /**
     * 用户注册
     *
     * @param requestDTO 注册参数
     * @return 注册结果
     */
    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequestDTO requestDTO) {
        log.info("注册用户：{}", requestDTO.getUsername());
        return userService.register(requestDTO);
    }

    /**
     * 退出登录
     *
     * @return 退出结果
     */
    @Operation(summary = "退出登录")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success("退出登录成功");
    }

    /**
     * 获取当前登录用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @Operation(summary = "获取当前用户信息")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public Result<UserVO> meByGet(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return getCurrentUser(userId);
    }

    /**
     * 修改当前用户密码
     *
     * @param userId 用户ID
     * @param requestDTO 修改密码参数
     * @return 修改结果
     */
    @Operation(summary = "修改密码")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/updatePassword")
    public Result<Void> updatePassword(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                       @Valid @RequestBody UpdatePasswordRequestDTO requestDTO) {
        return userService.updatePassword(userId, requestDTO);
    }

    // 统一封装获取当前用户的返回逻辑
    private Result<UserVO> getCurrentUser(Long userId) {
        log.info("获取当前用户信息：{}", userId);
        UserVO userVO = userService.getUserInfo(userId);
        if (userVO == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "用户不存在");
        }
        return Result.success(userVO);
    }
}
