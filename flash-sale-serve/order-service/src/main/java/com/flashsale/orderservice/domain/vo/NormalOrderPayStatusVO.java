package com.flashsale.orderservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * @author strive_qin
 * @version 1.0
 * @description NormalOrderPayStatusVO
 * @date 2026/3/20 00:00
 */


@Data
@Schema(description = "普通订单支付状态")
public class NormalOrderPayStatusVO {

    @Schema(description = "订单ID", example = "30001")
    private Long orderId;

    @Schema(description = "订单号", example = "202603200001")
    private String orderNo;

    @Schema(description = "订单状态", example = "1")
    private Integer orderStatus;

    @Schema(description = "是否已支付", example = "true")
    private Boolean paid;

    @Schema(description = "支付金额", example = "199.00")
    private BigDecimal payAmount;

    @Schema(description = "支付时间", example = "2026-03-20T18:00:00")
    private LocalDateTime payTime;

    @Schema(description = "取消原因", example = "超时未支付，系统自动取消")
    private String cancelReason;

    @Schema(description = "取消时间", example = "2026-03-20T17:45:00")
    private LocalDateTime cancelTime;

    @Schema(description = "状态说明", example = "订单已支付")
    private String message;
}
