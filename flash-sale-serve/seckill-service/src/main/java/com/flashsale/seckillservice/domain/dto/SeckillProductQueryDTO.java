package com.flashsale.seckillservice.domain.dto;

import lombok.Data;

/**
 * 秒杀商品查询 DTO
 */
@Data
public class SeckillProductQueryDTO {

    /**
     * 商品名称（模糊查询）
     */
    private String name;

    /**
     * 状态：0-下架，1-上架
     */
    private Integer status;
}
