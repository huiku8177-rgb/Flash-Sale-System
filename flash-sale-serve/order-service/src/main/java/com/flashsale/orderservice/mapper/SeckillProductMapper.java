package com.flashsale.orderservice.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillProductMapper
 * @date 2026/3/20 00:00
 */
public interface SeckillProductMapper {

    int decreaseStock(@Param("productId") Long productId);

    int increaseStock(@Param("productId") Long productId);
}
