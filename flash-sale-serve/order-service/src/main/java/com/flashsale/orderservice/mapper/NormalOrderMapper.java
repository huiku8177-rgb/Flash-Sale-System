package com.flashsale.orderservice.mapper;

import com.flashsale.orderservice.domain.po.NormalOrderPO;
import com.flashsale.orderservice.domain.vo.NormalOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 普通订单主表 Mapper。
 */
@Mapper
public interface NormalOrderMapper {

    void insert(NormalOrderPO order);

    List<NormalOrderVO> listOrders(@Param("userId") Long userId, @Param("orderStatus") Integer orderStatus);

    NormalOrderVO getOrderDetail(@Param("userId") Long userId, @Param("id") Long id);

    int updatePayStatus(@Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("expectedStatus") Integer expectedStatus,
                        @Param("targetStatus") Integer targetStatus,
                        @Param("payAmount") BigDecimal payAmount,
                        @Param("payTime") LocalDateTime payTime);
}
