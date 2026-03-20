package com.flashsale.authservice.domain.po;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 用户实体
 *
 * @author strive_qin
 * @version 1.0
 * @description User
 * @date 2026/3/11 16:15
 */
@Data
public class User {
    private Long id;
    private String username;
    private String password;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String createTime;
}
