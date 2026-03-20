package com.flashsale.seckillservice.mapper;

import com.flashsale.seckillservice.domain.dto.SeckillProductQueryDTO;
import com.flashsale.seckillservice.domain.po.SeckillProductPO;
import com.flashsale.seckillservice.domain.vo.SeckillProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillProductMapper
 * @date 2026/3/20 00:00
 */


@Mapper
public interface SeckillProductMapper {

    List<SeckillProductVO> listProducts(@Param("queryDTO") SeckillProductQueryDTO queryDTO);

    SeckillProductVO getProductDetail(@Param("id") Long id);

    List<SeckillProductPO> listAll();

    SeckillProductPO getById(@Param("id") Long productId);
}
