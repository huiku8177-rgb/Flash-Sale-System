package com.flashsale.seckillservice.service.impl;

import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
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
        if (queryDTO == null) {
            queryDTO = new ProductQueryDTO();
        }
        if (queryDTO.getStatus() == null) {
            queryDTO.setStatus(1);
        }
        return Result.success(productMapper.listProducts(queryDTO));
    }

    @Override
    public Result<ProductVO> getProductDetail(Long id) {
        if (id == null) {
            return Result.error(ResultCode.PARAM_ERROR, "商品ID不能为空");
        }
        ProductVO product = productMapper.getProductDetail(id);
        if (product == null) {
            return Result.error(ResultCode.BUSINESS_ERROR, "商品不存在");
        }
        return Result.success(product);
    }
}
