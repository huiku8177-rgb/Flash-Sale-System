package com.flashsale.productservice.mapper;

import com.flashsale.productservice.domain.dto.ProductQueryDTO;
import com.flashsale.productservice.domain.po.ProductPO;
import com.flashsale.productservice.domain.vo.ProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
/**
 * @author strive_qin
 * @version 1.0
 * @description ProductMapper
 * @date 2026/3/20 00:00
 */


@Mapper
public interface ProductMapper {

    List<ProductVO> listProducts(@Param("queryDTO") ProductQueryDTO queryDTO);

    ProductVO getProductDetail(@Param("id") Long id);

    List<ProductPO> listByIds(@Param("ids") List<Long> ids);

    int decreaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    int increaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
