package com.flashsale.seckillservice.redis;

/**
 * @author strive_qin
 * @version 1.0
 * @description ReidsKeys
 * @date 2026/3/13 14:58
 */
public final class RedisKeys {

    private RedisKeys() {}

    public static final String PREFIX_SECKILL_STOCK = "seckill:stock:";
    public static final String PREFIX_SECKILL_USER = "seckill:user:";
    public static final String PREFIX_SECKILL_PRODUCT = "seckill:product:";
    public static final String KEY_SECKILL_PRODUCT_LIST = "seckill:product:list";
    public static final String PREFIX_RATE_LIMIT = "rate_limit:";

    public static String seckillStock(Long productId) {
        return PREFIX_SECKILL_STOCK + productId;
    }

    public static String seckillUser(Long productId, Long userId) {
        return PREFIX_SECKILL_USER + productId + ":" + userId;
    }

    public static String seckillProduct(Long productId) {
        return PREFIX_SECKILL_PRODUCT + productId;
    }

    public static String rateLimit(Long userId) {
        return PREFIX_RATE_LIMIT + userId;
    }
}