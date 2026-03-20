package com.flashsale.seckillservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillResultVO
 * @date 2026/3/20 00:00
 */


@Data
@Schema(description = "提交秒杀后的即时返回结果")
public class SeckillResultVO {

    @Schema(description = "请求是否已进入异步处理流程", example = "true")
    private Boolean success;

    @Schema(description = "结果说明", example = "秒杀请求已受理，订单处理中")
    private String message;

    @Schema(description = "秒杀商品ID", example = "2001")
    private Long productId;

    @Schema(description = "订单ID，异步建单完成前可能为空", example = "50001")
    private Long orderId;
}
