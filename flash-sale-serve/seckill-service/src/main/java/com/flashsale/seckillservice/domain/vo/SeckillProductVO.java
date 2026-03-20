package com.flashsale.seckillservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillProductVO
 * @date 2026/3/20 00:00
 */


@Data
@Schema(description = "秒杀商品信息")
public class SeckillProductVO {

    @Schema(description = "秒杀商品ID", example = "2001")
    private Long id;

    @Schema(description = "商品名称", example = "无线蓝牙耳机")
    private String name;

    @Schema(description = "原价", example = "999.00")
    private BigDecimal price;

    @Schema(description = "秒杀价", example = "699.00")
    private BigDecimal seckillPrice;

    @Schema(description = "库存", example = "50")
    private Integer stock;

    @Schema(description = "状态，1-启用，0-停用", example = "1")
    private Integer status;

    @Schema(description = "秒杀开始时间", example = "2026-03-20T20:00:00")
    private LocalDateTime startTime;

    @Schema(description = "秒杀结束时间", example = "2026-03-20T22:00:00")
    private LocalDateTime endTime;
}
