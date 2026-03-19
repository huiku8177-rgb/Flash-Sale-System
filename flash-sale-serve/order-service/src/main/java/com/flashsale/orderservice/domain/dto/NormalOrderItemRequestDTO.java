package com.flashsale.orderservice.domain.dto;

import lombok.Data;

/**
 * 普通订单商品项请求参数。
 */
@Data
public class NormalOrderItemRequestDTO {

    /**
     * 普通商品 ID。
     */
    private Long productId;

    /**
     * 购买数量。
     */
    private Integer quantity;
}
