package com.flashsale.authservice.domain.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author strive_qin
 * @version 1.0
 * @description UserAddressPO
 * @date 2026/3/23 00:00
 */
@Data
public class UserAddressPO {

    private Long id;
    private Long userId;
    private String receiver;
    private String mobile;
    private String detail;
    private Integer isDefault;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
