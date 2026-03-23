package com.flashsale.productservice.domain.vo;

import lombok.Data;

/**
 * @author strive_qin
 * @version 1.0
 * @description UserAddressVO
 * @date 2026/3/23 00:00
 */
@Data
public class UserAddressVO {

    private Long id;

    private Long userId;

    private String receiver;

    private String mobile;

    private String detail;

    private Boolean isDefault;
}
