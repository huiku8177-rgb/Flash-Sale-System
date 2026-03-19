package com.flashsale.orderservice.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 普通订单明细返回对象。
 */
@Data
public class NormalOrderItemVO {

    private Long id;

    private Long orderId;

    private Long userId;

    private Long productId;

    private String productName;

    private String productSubtitle;

    private String productImage;

    private BigDecimal salePrice;

    private Integer quantity;

    private BigDecimal itemAmount;

    private LocalDateTime createTime;
}
