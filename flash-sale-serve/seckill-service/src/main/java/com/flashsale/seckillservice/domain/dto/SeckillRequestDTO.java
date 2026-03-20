package com.flashsale.seckillservice.domain.dto;

import lombok.Data;

/**
 * 秒杀请求 DTO
 *
 * @author strive_qin
 * @version 1.0
 * @description SeckillRequestDTO
 * @date 2026/3/13 16:01
 */
@Data
public class SeckillRequestDTO {

    /**
     * 秒杀商品ID
     */
    private Long productId;

    /**
     * 用户ID
     */
    private Long userId;
}
