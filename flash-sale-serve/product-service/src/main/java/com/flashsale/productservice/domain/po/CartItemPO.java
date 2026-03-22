package com.flashsale.productservice.domain.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author strive_qin
 * @version 1.0
 * @description CartItemPO
 * @date 2026/3/22 00:00
 */
@Data
public class CartItemPO {

    private Long id;

    private Long userId;

    private Long productId;

    private Integer quantity;

    private Integer selected;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
