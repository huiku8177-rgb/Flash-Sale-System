package com.flashsale.seckillservice.domain.vo;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillResultVO
 * @date 2026/3/13 16:03
 */

import lombok.Builder;
import lombok.Data;

/**
 * 秒杀结果 VO
 */
@Data
public class SeckillResultVO {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 订单ID（异步下单时可以先为空）
     */
    private Long orderId;
}
