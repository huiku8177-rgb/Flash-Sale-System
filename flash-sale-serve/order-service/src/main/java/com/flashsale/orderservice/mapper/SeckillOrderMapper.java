package com.flashsale.orderservice.mapper;

import com.flashsale.orderservice.domain.po.SeckillOrderPO;
import com.flashsale.orderservice.domain.vo.SeckillOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillOrderMapper
 * @date 2026/3/13 17:00
 */
@Mapper
public interface SeckillOrderMapper {

    List<SeckillOrderVO> listOrders();

    SeckillOrderVO getOrderDetail(@Param("id") Long id);

    void insert(SeckillOrderPO order);

    SeckillOrderPO getByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
}
