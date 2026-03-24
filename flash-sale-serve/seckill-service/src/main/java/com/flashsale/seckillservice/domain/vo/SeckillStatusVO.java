package com.flashsale.seckillservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillStatusVO
 * @date 2026/3/13 17:00
 */
@Data
@Schema(description = "秒杀处理状态")
public class SeckillStatusVO {

    @Schema(description = "秒杀状态：1-成功，0-处理中，-1-失败", example = "0")
    private Integer status;

    @Schema(description = "状态说明", example = "处理中")
    private String message;

    @Schema(description = "成功时返回的订单ID", example = "50001")
    private Long orderId;

    @Schema(description = "成功时返回的秒杀订单号", example = "dd54b696cb5e4213839982f4222a2116")
    private String orderNo;
}
