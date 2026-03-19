package com.flashsale.orderservice.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 普通订单返回对象。
 */
@Data
public class NormalOrderVO {

    private Long id;

    private String orderNo;

    private Long userId;

    /**
     * 0-待支付，1-已支付，2-已取消，3-已发货，4-已完成
     */
    private Integer orderStatus;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private LocalDateTime payTime;

    private String remark;

    private String addressSnapshot;

    private LocalDateTime createTime;

    private List<NormalOrderItemVO> items;
}
