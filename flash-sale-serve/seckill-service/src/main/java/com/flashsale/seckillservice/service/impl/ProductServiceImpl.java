package com.flashsale.seckillservice.service.impl;

import com.flashsale.common.domain.Result;
import com.flashsale.seckillservice.domain.dto.ProductQueryDTO;
import com.flashsale.seckillservice.domain.vo.ProductVO;
import com.flashsale.seckillservice.mapper.ProductMapper;
import com.flashsale.seckillservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    @Override
    public Result<List<ProductVO>> listProducts(ProductQueryDTO queryDTO) {
        return Result.success(productMapper.listProducts(queryDTO));
    }

    @Override
    public Result<ProductVO> getProductDetail(Long id) {
        return Result.success(productMapper.getProductDetail(id));
    }
}
