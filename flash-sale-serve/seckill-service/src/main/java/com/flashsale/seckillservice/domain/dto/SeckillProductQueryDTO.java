package com.flashsale.seckillservice.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillProductQueryDTO
 * @date 2026/3/20 00:00
 */


@Data
@Schema(description = "秒杀商品列表查询参数")
public class SeckillProductQueryDTO {

    @Schema(description = "商品名称关键字", example = "耳机")
    private String name;

    @Schema(description = "商品状态，1-启用，0-停用", example = "1")
    private Integer status;
}
