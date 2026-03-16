package com.flashsale.seckillservice.controller;

import com.flashsale.common.domain.Result;
import com.flashsale.seckillservice.domain.dto.ProductQueryDTO;
import com.flashsale.seckillservice.domain.vo.ProductVO;
import com.flashsale.seckillservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description ProductController
 * @date 2026/3/13 15:00
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
@Slf4j
public class ProductController {

    private final ProductService productService;

    @GetMapping("/products")
    public Result<List<ProductVO>> listProducts(ProductQueryDTO queryDTO) {
        log.info("查询商品列表: {}", queryDTO);
        return productService.listProducts(queryDTO);
    }

    @GetMapping("/products/{id}")
    public Result<ProductVO> getProductDetail(@PathVariable("id") Long id) {
        return productService.getProductDetail(id);
    }
}
