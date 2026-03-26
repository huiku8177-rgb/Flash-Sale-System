package com.flashsale.seckillservice.controller;

import com.flashsale.common.domain.Result;
import com.flashsale.seckillservice.domain.dto.SeckillProductQueryDTO;
import com.flashsale.seckillservice.domain.vo.SeckillProductVO;
import com.flashsale.seckillservice.service.SeckillProductService;
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

@Tag(name = "秒杀商品", description = "秒杀商品相关接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/seckill-product")
@Slf4j
public class SeckillProductController {

    private final SeckillProductService seckillProductService;

    @Operation(summary = "查询秒杀商品列表")
    @GetMapping("/products")
    public Result<List<SeckillProductVO>> listProducts(@ParameterObject SeckillProductQueryDTO queryDTO) {
        log.info("list seckill products request received, query={}", queryDTO);
        return seckillProductService.listProducts(queryDTO);
    }

    @Operation(summary = "查询秒杀商品详情")
    @GetMapping("/products/{id}")
    public Result<SeckillProductVO> getProductDetail(@Parameter(description = "秒杀商品ID", example = "2001")
                                                     @PathVariable("id") @Min(1) Long id) {
        log.info("get seckill product detail request received, productId={}", id);
        return seckillProductService.getProductDetail(id);
    }
}
