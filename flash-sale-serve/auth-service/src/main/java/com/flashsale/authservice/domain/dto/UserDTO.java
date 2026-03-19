package com.flashsale.authservice.domain.dto;

import lombok.Data;

/**
 * @author strive_qin
 * @version 1.0
 * @description User
 * @date 2026/3/11 16:16
 */
@Data
public class UserDTO {

    /**
     * 用户名。
     * 登录、注册时使用。
     */
    private String username;

    /**
     * 密码。
     * 登录、注册时表示当前密码。
     */
    private String password;

    /**
     * 旧密码。
     * 修改密码时使用。
     */
    private String oldPassword;

    /**
     * 新密码。
     * 修改密码时使用。
     */
    private String newPassword;
}
