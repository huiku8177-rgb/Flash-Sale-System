package com.flashsale.seckillservice.mapper;

import com.flashsale.seckillservice.domain.dto.ProductQueryDTO;
import com.flashsale.seckillservice.domain.vo.ProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    List<ProductVO> listProducts(@Param("queryDTO") ProductQueryDTO queryDTO);

    ProductVO getProductDetail(@Param("id") Long id);
}
