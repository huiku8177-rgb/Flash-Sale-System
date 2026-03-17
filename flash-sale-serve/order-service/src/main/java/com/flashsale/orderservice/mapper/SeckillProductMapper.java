package com.flashsale.orderservice.mapper;

import org.apache.ibatis.annotations.Param;

public interface SeckillProductMapper {

    int decreaseStock(@Param("productId") Long productId);
}
