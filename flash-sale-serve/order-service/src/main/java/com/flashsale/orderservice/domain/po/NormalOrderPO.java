package com.flashsale.orderservice.domain.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 普通订单主表实体。
 */
/**
 * @author strive_qin
 * @version 1.0
 * @description NormalOrderPO
 * @date 2026/3/20 00:00
 */

@Data
public class NormalOrderPO {

    private Long id;

    private String orderNo;

    private Long userId;

    private Integer orderStatus;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private LocalDateTime payTime;

    private String remark;

    private String addressSnapshot;

    private String cancelReason;

    private LocalDateTime cancelTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
