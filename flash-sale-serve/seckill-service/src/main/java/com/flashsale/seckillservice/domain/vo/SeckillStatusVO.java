package com.flashsale.seckillservice.domain.vo;

import lombok.Data;

/**
 * 秒杀结果查询 VO
 */
@Data
public class SeckillStatusVO {

    /**
     * 1-秒杀成功
     * 0-排队中
     * -1-秒杀失败
     */
    private Integer status;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 订单ID
     */
    private Long orderId;
}