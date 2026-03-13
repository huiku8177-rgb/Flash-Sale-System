package com.flashsale.seckillservice.domain.dto;

/**
 * @author strive_qin
 * @version 1.0
 * @description ProductQueryDTO
 * @date 2026/3/13 16:02
 */

import lombok.Data;

/**
 * 商品查询 DTO
 */
@Data
public class ProductQueryDTO {

    /**
     * 商品名称（模糊查询）
     */
    private String name;

    /**
     * 状态：0-下架，1-上架
     */
    private Integer status;
}
