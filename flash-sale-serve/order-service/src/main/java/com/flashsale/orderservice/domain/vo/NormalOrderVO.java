package com.flashsale.orderservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
/**
 * @author strive_qin
 * @version 1.0
 * @description NormalOrderVO
 * @date 2026/3/20 00:00
 */


@Data
@Schema(description = "普通订单详情")
public class NormalOrderVO {

    @Schema(description = "订单ID", example = "30001")
    private Long id;

    @Schema(description = "订单号", example = "202603200001")
    private String orderNo;

    @Schema(description = "用户ID", example = "10001")
    private Long userId;

    @Schema(description = "订单状态：0-待支付，1-已支付，2-已取消，3-已发货，4-已完成", example = "0")
    private Integer orderStatus;

    @Schema(description = "订单总金额", example = "199.00")
    private BigDecimal totalAmount;

    @Schema(description = "实付金额", example = "199.00")
    private BigDecimal payAmount;

    @Schema(description = "支付时间", example = "2026-03-20T18:00:00")
    private LocalDateTime payTime;

    @Schema(description = "订单备注", example = "工作日送达")
    private String remark;

    @Schema(description = "地址快照JSON字符串")
    private String addressSnapshot;

    @Schema(description = "创建时间", example = "2026-03-20T17:30:00")
    private LocalDateTime createTime;

    @Schema(description = "订单商品项")
    private List<NormalOrderItemVO> items;
}
