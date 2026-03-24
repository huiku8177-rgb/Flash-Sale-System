package com.flashsale.orderservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillOrderVO
 * @date 2026/3/20 00:00
 */
@Data
@Schema(description = "秒杀订单详情")
public class SeckillOrderVO {

    @Schema(description = "订单ID", example = "50001")
    private Long id;

    @Schema(description = "秒杀订单号", example = "dd54b696cb5e4213839982f4222a2116")
    private String orderNo;

    @Schema(description = "用户ID", example = "10001")
    private Long userId;

    @Schema(description = "秒杀商品ID", example = "2001")
    private Long productId;

    @Schema(description = "秒杀价格", example = "699.00")
    private BigDecimal seckillPrice;

    @Schema(description = "订单状态：0-待支付，1-已支付，2-已取消", example = "0")
    private Integer status;

    @Schema(description = "支付时间", example = "2026-03-24T12:00:00")
    private LocalDateTime payTime;

    @Schema(description = "取消原因", example = "超时自动取消")
    private String cancelReason;

    @Schema(description = "取消时间", example = "2026-03-24T12:15:00")
    private LocalDateTime cancelTime;

    @Schema(description = "创建时间", example = "2026-03-20T20:00:05")
    private LocalDateTime createTime;
}
