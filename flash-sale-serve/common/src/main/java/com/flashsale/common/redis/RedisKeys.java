package com.flashsale.common.redis;

/**
 * Redis Key 规范
 *
 * 当前版本以 productId 作为秒杀商品唯一标识。
 * 后续若支持多活动场景，可扩展为 activityId + productId 或 seckillProductId 维度。
 *
 * Key 设计格式：
 * seckill:业务对象:维度[:扩展维度]
 *
 * 例如：
 * seckill:stock:1
 * seckill:user:1
 * seckill:product:1
 * seckill:product:list
 * seckill:rate_limit:user:1001
 * seckill:order:1001:1
 * seckill:mq:consume:msgId123
 *
 * @author strive_qin
 * @version 1.0
 * @description RedisKeys
 * @date 2026/3/13 14:58
 *
 */
public final class RedisKeys {

    private RedisKeys() {}

    /**
     * 通用前缀
     */
    public static final String PREFIX_SECKILL = "seckill:";

    /**
     * 秒杀库存前缀
     * 示例：seckill:stock:1
     */
    public static final String PREFIX_SECKILL_STOCK = PREFIX_SECKILL + "stock:";

    /**
     * 秒杀用户记录前缀
     * 当前设计：一个商品对应一个 Set，里面存 userId
     * 示例：seckill:user:1
     */
    public static final String PREFIX_SECKILL_USER = PREFIX_SECKILL + "user:";

    /**
     * 商品详情缓存前缀
     * 示例：seckill:product:1
     */
    public static final String PREFIX_SECKILL_PRODUCT = PREFIX_SECKILL + "product:";

    /**
     * 商品列表缓存
     * 示例：seckill:product:list
     */
    public static final String KEY_SECKILL_PRODUCT_LIST = PREFIX_SECKILL + "product:list";

    /**
     * 用户限流前缀
     * 示例：seckill:rate_limit:user:1001
     */
    public static final String PREFIX_RATE_LIMIT = PREFIX_SECKILL + "rate_limit:user:";

    /**
     * 秒杀结果前缀（可选扩展）
     * 示例：seckill:order:1001:1
     * 含义：用户1001抢到商品1后，对应订单号缓存
     */
    public static final String PREFIX_SECKILL_ORDER = PREFIX_SECKILL + "order:";

    /**
     * 秒杀处理状态前缀
     * 示例：seckill:result:1001:1
     */
    public static final String PREFIX_SECKILL_RESULT = PREFIX_SECKILL + "result:";

    /**
     * MQ 消费幂等前缀（可选扩展）
     * 示例：seckill:mq:consume:msgId123
     */
    public static final String PREFIX_MQ_CONSUME = PREFIX_SECKILL + "mq:consume:";

    /**
     * 活动库存前缀（后续扩展：多场秒杀活动）
     * 示例：seckill:activity:stock:10:1
     * 含义：活动10下商品1的库存
     */
    public static final String PREFIX_ACTIVITY_STOCK = PREFIX_SECKILL + "activity:stock:";

    /**
     * 活动用户记录前缀（后续扩展：多场秒杀活动）
     * 示例：seckill:activity:user:10:1
     * 含义：活动10下商品1的已抢用户集合
     */
    public static final String PREFIX_ACTIVITY_USER = PREFIX_SECKILL + "activity:user:";

    /**
     * 秒杀库存 key
     */
    public static String seckillStock(Long productId) {
        return PREFIX_SECKILL_STOCK + productId;
    }

    /**
     * 秒杀用户记录 key
     * 当前用于 Redis Set，存储已抢购该商品的用户ID
     */
    public static String seckillUser(Long productId) {
        return PREFIX_SECKILL_USER + productId;
    }

    /**
     * 商品详情缓存 key
     */
    public static String seckillProduct(Long productId) {
        return PREFIX_SECKILL_PRODUCT + productId;
    }

    /**
     * 商品列表缓存 key
     */
    public static String seckillProductList() {
        return KEY_SECKILL_PRODUCT_LIST;
    }

    /**
     * 用户限流 key
     */
    public static String rateLimit(Long userId) {
        return PREFIX_RATE_LIMIT + userId;
    }

    /**
     * 秒杀结果 key（可选扩展）
     * 值通常可存 orderId
     */
    public static String seckillOrder(Long userId, Long productId) {
        return PREFIX_SECKILL_ORDER + userId + ":" + productId;
    }

    /**
     * 秒杀状态 key
     */
    public static String seckillResult(Long userId, Long productId) {
        return PREFIX_SECKILL_RESULT + userId + ":" + productId;
    }

    /**
     * MQ 消费幂等 key（可选扩展）
     */
    public static String mqConsume(String messageId) {
        return PREFIX_MQ_CONSUME + messageId;
    }

    /**
     * 活动库存 key（后续扩展）
     */
    public static String activityStock(Long activityId, Long productId) {
        return PREFIX_ACTIVITY_STOCK + activityId + ":" + productId;
    }

    /**
     * 活动用户记录 key（后续扩展）
     */
    public static String activityUsers(Long activityId, Long productId) {
        return PREFIX_ACTIVITY_USER + activityId + ":" + productId;
    }


}
