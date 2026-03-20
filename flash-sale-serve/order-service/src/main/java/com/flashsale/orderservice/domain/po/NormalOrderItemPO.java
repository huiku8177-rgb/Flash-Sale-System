package com.flashsale.orderservice.domain.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 普通订单明细实体。
 */
/**
 * @author strive_qin
 * @version 1.0
 * @description NormalOrderItemPO
 * @date 2026/3/20 00:00
 */

@Data
public class NormalOrderItemPO {

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

    private LocalDateTime updateTime;
}
