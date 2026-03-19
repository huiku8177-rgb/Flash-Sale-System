package com.flashsale.orderservice.mapper;

import com.flashsale.orderservice.domain.po.NormalOrderItemPO;
import com.flashsale.orderservice.domain.vo.NormalOrderItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 普通订单明细 Mapper。
 */
@Mapper
public interface NormalOrderItemMapper {

    void insertBatch(@Param("items") List<NormalOrderItemPO> items);

    List<NormalOrderItemVO> listByOrderIds(@Param("orderIds") List<Long> orderIds);

    List<NormalOrderItemVO> listByOrderId(@Param("orderId") Long orderId);
}
