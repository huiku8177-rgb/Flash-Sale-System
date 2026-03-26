package com.flashsale.productservice.controller;

import com.flashsale.common.domain.Result;
import com.flashsale.productservice.domain.dto.ProductQueryDTO;
import com.flashsale.productservice.domain.vo.ProductVO;
import com.flashsale.productservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "普通商品", description = "普通商品相关接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
@Slf4j
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "查询普通商品列表")
    @GetMapping("/products")
    public Result<List<ProductVO>> listProducts(@ParameterObject ProductQueryDTO queryDTO) {
        log.info("list products request received, query={}", queryDTO);
        return productService.listProducts(queryDTO);
    }

    @Operation(summary = "查询普通商品详情")
    @GetMapping("/products/{id}")
    public Result<ProductVO> getProductDetail(@Parameter(description = "商品ID", example = "1001")
                                              @PathVariable("id") @Min(value = 1, message = "商品ID必须大于等于1") Long id) {
        log.info("get product detail request received, productId={}", id);
        return productService.getProductDetail(id);
    }
}
