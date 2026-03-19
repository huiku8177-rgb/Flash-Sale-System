package com.flashsale.seckillservice.controller;

import com.flashsale.common.domain.Result;
import com.flashsale.seckillservice.domain.dto.ProductQueryDTO;
import com.flashsale.seckillservice.domain.vo.ProductVO;
import com.flashsale.seckillservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    /**
     * 获取商品列表
     *
     * @param queryDTO
     * @return
     */
    @GetMapping("/products")
    public Result<List<ProductVO>> listProducts(ProductQueryDTO queryDTO) {
        return productService.listProducts(queryDTO);
    }

    /**
     * 获取商品详情
     *
     * @param id
     * @return
     */
    @GetMapping("/products/{id}")
    public Result<ProductVO> getProductDetail(@PathVariable("id") Long id) {
        return productService.getProductDetail(id);
    }
}
