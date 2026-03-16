package com.flashsale.seckillservice.mapper;

import com.flashsale.seckillservice.domain.dto.ProductQueryDTO;
import com.flashsale.seckillservice.domain.po.ProductPO;
import com.flashsale.seckillservice.domain.vo.ProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description ProductMapper
 * @date 2026/3/13 17:00
 */
@Mapper
public interface ProductMapper {

    List<ProductVO> listProducts(@Param("queryDTO") ProductQueryDTO queryDTO);

    ProductVO getProductDetail(@Param("id") Long id);


    List<ProductPO> listAll();


    ProductPO getById(Long productId);
}
