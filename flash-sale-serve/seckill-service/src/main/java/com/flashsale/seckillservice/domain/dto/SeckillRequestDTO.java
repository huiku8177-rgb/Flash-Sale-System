package com.flashsale.seckillservice.domain.dto;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillRequestDTO
 * @date 2026/3/13 16:01
 */
import lombok.Data;

/**
 * 秒杀请求 DTO
 */
@Data
public class SeckillRequestDTO {

    /**
     * 商品ID
     */
    private Long productId;
}
