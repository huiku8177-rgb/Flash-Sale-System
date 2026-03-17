package com.flashsale.seckillservice.service;

import com.flashsale.common.domain.Result;
import com.flashsale.seckillservice.domain.dto.ProductQueryDTO;
import com.flashsale.seckillservice.domain.vo.ProductVO;

import java.util.List;

public interface ProductService {

    Result<List<ProductVO>> listProducts(ProductQueryDTO queryDTO);

    Result<ProductVO> getProductDetail(Long id);
}
