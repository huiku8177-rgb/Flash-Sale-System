package com.flashsale.orderservice.mapper;

import com.flashsale.orderservice.domain.po.NormalOrderPO;
import com.flashsale.orderservice.domain.vo.NormalOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description NormalOrderMapper
 * @date 2026/3/20 00:00
 */
@Mapper
public interface NormalOrderMapper {

    void insert(NormalOrderPO order);

    List<NormalOrderVO> listOrders(@Param("userId") Long userId, @Param("orderStatus") Integer orderStatus);

    NormalOrderVO getOrderDetail(@Param("userId") Long userId, @Param("id") Long id);

    NormalOrderVO getOrderDetailByOrderNo(@Param("userId") Long userId, @Param("orderNo") String orderNo);

    int updatePayStatus(@Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("expectedStatus") Integer expectedStatus,
                        @Param("targetStatus") Integer targetStatus,
                        @Param("payAmount") BigDecimal payAmount,
                        @Param("payTime") LocalDateTime payTime);

    int updateOrderStatus(@Param("id") Long id,
                          @Param("userId") Long userId,
                          @Param("expectedStatus") Integer expectedStatus,
                          @Param("targetStatus") Integer targetStatus,
                          @Param("cancelReason") String cancelReason,
                          @Param("cancelTime") LocalDateTime cancelTime);

    List<NormalOrderPO> listTimeoutOrders(@Param("deadline") LocalDateTime deadline,
                                          @Param("expectedStatus") Integer expectedStatus,
                                          @Param("limit") Integer limit);
}
