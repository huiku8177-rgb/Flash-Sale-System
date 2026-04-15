package com.flashsale.common.redis;

import cn.hutool.crypto.digest.DigestUtil;

/**
 * Redis key conventions shared across services.
 */
public final class RedisKeys {

    public static final String PREFIX_GATEWAY = "gateway:";
    public static final String PREFIX_AUTH = "auth:";
    public static final String PREFIX_PRODUCT = "product:";
    public static final String PREFIX_SECKILL = "seckill:";

    public static final String PREFIX_GATEWAY_RATE_LIMIT = PREFIX_GATEWAY + "rate_limit:";

    public static final String PREFIX_AUTH_TOKEN_BLACKLIST = PREFIX_AUTH + "token:blacklist:";
    public static final String PREFIX_AUTH_TOKEN_VERSION = PREFIX_AUTH + "token:version:";

    public static final String PREFIX_PRODUCT_DETAIL_CACHE = PREFIX_PRODUCT + "cache:detail:";
    public static final String PREFIX_PRODUCT_LIST_CACHE = PREFIX_PRODUCT + "cache:list:";
    public static final String KEY_PRODUCT_LIST_CACHE_VERSION = PREFIX_PRODUCT + "cache:list:version";
    public static final String PREFIX_PRODUCT_CART_CACHE = PREFIX_PRODUCT + "cart:";

    public static final String PREFIX_SECKILL_STOCK = PREFIX_SECKILL + "stock:";
    public static final String PREFIX_SECKILL_USER = PREFIX_SECKILL + "user:";
    public static final String PREFIX_SECKILL_PRODUCT = PREFIX_SECKILL + "product:";
    public static final String KEY_SECKILL_PRODUCT_LIST = PREFIX_SECKILL + "product:list";
    public static final String PREFIX_SECKILL_PRODUCT_DETAIL_CACHE = PREFIX_SECKILL + "cache:product:detail:";
    public static final String PREFIX_SECKILL_PRODUCT_LIST_CACHE = PREFIX_SECKILL + "cache:product:list:";
    public static final String KEY_SECKILL_PRODUCT_LIST_CACHE_VERSION = PREFIX_SECKILL + "cache:product:list:version";
    public static final String PREFIX_RATE_LIMIT = PREFIX_SECKILL + "rate_limit:user:";
    public static final String PREFIX_SECKILL_ORDER = PREFIX_SECKILL + "order:";
    public static final String PREFIX_SECKILL_RESULT = PREFIX_SECKILL + "result:";
    public static final String PREFIX_MQ_CONSUME = PREFIX_SECKILL + "mq:consume:";
    public static final String PREFIX_ACTIVITY_STOCK = PREFIX_SECKILL + "activity:stock:";
    public static final String PREFIX_ACTIVITY_USER = PREFIX_SECKILL + "activity:user:";

    private RedisKeys() {
    }

    public static String gatewayRateLimit(String ruleId, String clientKey, long windowStartEpochSeconds) {
        return PREFIX_GATEWAY_RATE_LIMIT
                + sha256(ruleId)
                + ":"
                + sha256(clientKey)
                + ":"
                + windowStartEpochSeconds;
    }

    public static String authTokenBlacklist(String token) {
        return PREFIX_AUTH_TOKEN_BLACKLIST + sha256(token);
    }

    public static String authTokenVersion(Long userId) {
        return PREFIX_AUTH_TOKEN_VERSION + userId;
    }

    public static String productDetailCache(Long productId) {
        return PREFIX_PRODUCT_DETAIL_CACHE + productId;
    }

    public static String productListCache(String queryFingerprint) {
        return PREFIX_PRODUCT_LIST_CACHE + sha256(queryFingerprint);
    }

    public static String productListCache(String queryFingerprint, long version) {
        return PREFIX_PRODUCT_LIST_CACHE + version + ":" + sha256(queryFingerprint);
    }

    public static String productListCacheVersion() {
        return KEY_PRODUCT_LIST_CACHE_VERSION;
    }

    public static String cartItems(Long userId) {
        return PREFIX_PRODUCT_CART_CACHE + userId;
    }

    public static String seckillStock(Long productId) {
        return PREFIX_SECKILL_STOCK + productId;
    }

    public static String seckillUser(Long productId) {
        return PREFIX_SECKILL_USER + productId;
    }

    public static String seckillProduct(Long productId) {
        return PREFIX_SECKILL_PRODUCT + productId;
    }

    public static String seckillProductList() {
        return KEY_SECKILL_PRODUCT_LIST;
    }

    public static String seckillProductDetailCache(Long productId) {
        return PREFIX_SECKILL_PRODUCT_DETAIL_CACHE + productId;
    }

    public static String seckillProductListCache(String queryFingerprint) {
        return PREFIX_SECKILL_PRODUCT_LIST_CACHE + sha256(queryFingerprint);
    }

    public static String seckillProductListCache(String queryFingerprint, long version) {
        return PREFIX_SECKILL_PRODUCT_LIST_CACHE + version + ":" + sha256(queryFingerprint);
    }

    public static String seckillProductListCacheVersion() {
        return KEY_SECKILL_PRODUCT_LIST_CACHE_VERSION;
    }

    public static String rateLimit(Long userId) {
        return PREFIX_RATE_LIMIT + userId;
    }

    public static String seckillOrder(Long userId, Long productId) {
        return PREFIX_SECKILL_ORDER + userId + ":" + productId;
    }

    public static String seckillResult(Long userId, Long productId) {
        return PREFIX_SECKILL_RESULT + userId + ":" + productId;
    }

    public static String mqConsume(String messageId) {
        return PREFIX_MQ_CONSUME + messageId;
    }

    public static String activityStock(Long activityId, Long productId) {
        return PREFIX_ACTIVITY_STOCK + activityId + ":" + productId;
    }

    public static String activityUsers(Long activityId, Long productId) {
        return PREFIX_ACTIVITY_USER + activityId + ":" + productId;
    }

    private static String sha256(String raw) {
        return DigestUtil.sha256Hex(raw == null ? "null" : raw);
    }
}
