package com.flashsale.orderservice.mapper;

import com.flashsale.orderservice.domain.po.SeckillOrderPO;
import com.flashsale.orderservice.domain.vo.SeckillOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 秒杀订单数据访问 Mapper
 *
 * @author strive_qin
 * @version 1.0
 * @description SeckillOrderMapper
 * @date 2026/3/13 17:00
 */
@Mapper
public interface SeckillOrderMapper {

    /**
     * 查询用户的秒杀订单列表
     */
    List<SeckillOrderVO> listOrders(@Param("userId") Long userId);

    /**
     * 查询秒杀订单详情
     */
    SeckillOrderVO getOrderDetail(@Param("userId") Long userId, @Param("id") Long id);

    /**
     * 新增秒杀订单
     */
    void insert(SeckillOrderPO order);

    /**
     * 按用户和商品查询秒杀订单，用于幂等处理
     */
    SeckillOrderPO getByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * 按预期状态更新秒杀订单状态
     */
    int updateStatus(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("expectedStatus") Integer expectedStatus,
                     @Param("targetStatus") Integer targetStatus);
}
