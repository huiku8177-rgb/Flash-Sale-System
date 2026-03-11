package com.flashsale.authservice.domain.vo;

import lombok.Data;

/**
 * @author strive_qin
 * @version 1.0
 * @description User
 * @date 2026/3/11 16:17
 */
@Data
public class UserVO {
    private Integer id;
    private String username;
    private String token;

}
