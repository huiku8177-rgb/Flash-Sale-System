package com.flashsale.seckillservice.mapper;

import com.flashsale.seckillservice.domain.po.SeckillOrderPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 秒杀结果查询 Mapper
 *
 * @author strive_qin
 * @version 1.0
 * @description SeckillMapper
 * @date 2026/3/13 17:00
 */
@Mapper
public interface SeckillMapper {

    /**
     * 秒杀结果查询兜底：
     * 当 Redis 结果丢失或过期时，回查数据库订单判断用户是否真实抢购成功。
     */
    SeckillOrderPO getOrderByUserIdAndProductId(@Param("userId") Long userId,
                                                @Param("productId") Long productId);
}
