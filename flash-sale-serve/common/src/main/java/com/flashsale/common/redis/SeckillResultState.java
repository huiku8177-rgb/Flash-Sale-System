package com.flashsale.common.redis;

/**
 * 秒杀结果缓存状态，供秒杀与订单服务共享。
 */
public final class SeckillResultState {

    public static final String PROCESSING = "PROCESSING";
    public static final String PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAIL = "FAIL";
    public static final String ALREADY_PAID = "ALREADY_PAID";
    public static final String CANCELLED = "CANCELLED";

    private SeckillResultState() {
    }
}
