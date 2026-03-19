package com.flashsale.orderservice.mapper;

import com.flashsale.orderservice.domain.po.NormalProductPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 普通商品 Mapper，订单服务只读商品快照并做库存扣减。
 */
@Mapper
public interface NormalProductMapper {

    List<NormalProductPO> listByIds(@Param("ids") List<Long> ids);

    int decreaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
