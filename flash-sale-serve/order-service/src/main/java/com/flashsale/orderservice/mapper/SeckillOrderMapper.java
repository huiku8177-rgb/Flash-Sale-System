package com.flashsale.orderservice.mapper;

import com.flashsale.orderservice.domain.po.SeckillOrderPO;
import com.flashsale.orderservice.domain.vo.SeckillOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillOrderMapper
 * @date 2026/3/13 17:00
 */
@Mapper
public interface SeckillOrderMapper {

    List<SeckillOrderVO> listOrders(@Param("userId") Long userId);

    SeckillOrderVO getOrderDetail(@Param("userId") Long userId, @Param("id") Long id);

    void insert(SeckillOrderPO order);

    SeckillOrderPO getByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    int updatePayStatus(@Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("expectedStatus") Integer expectedStatus,
                        @Param("targetStatus") Integer targetStatus,
                        @Param("payTime") LocalDateTime payTime);

    int updateOrderStatus(@Param("id") Long id,
                          @Param("userId") Long userId,
                          @Param("expectedStatus") Integer expectedStatus,
                          @Param("targetStatus") Integer targetStatus,
                          @Param("cancelReason") String cancelReason,
                          @Param("cancelTime") LocalDateTime cancelTime);

    List<SeckillOrderPO> listTimeoutOrders(@Param("deadline") LocalDateTime deadline,
                                           @Param("expectedStatus") Integer expectedStatus,
                                           @Param("limit") Integer limit);
}
