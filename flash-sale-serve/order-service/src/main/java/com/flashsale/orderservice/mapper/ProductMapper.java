package com.flashsale.orderservice.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * @author strive_qin
 * @version 1.0
 * @description ProductMapper
 * @date 2026/3/16 13:00
 */
public interface ProductMapper {

    int decreaseStock(@Param("productId") Long productId);
}
