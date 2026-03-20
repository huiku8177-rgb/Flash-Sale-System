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
/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillProductController
 * @date 2026/3/20 00:00
 */


@Tag(name = "秒杀商品", description = "秒杀商品相关接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/seckill-product")
@Slf4j
public class SeckillProductController {

    private final SeckillProductService seckillProductService;

    /**
     * 查询秒杀商品列表
     *
     * @param queryDTO 查询参数
     * @return 商品列表
     */
    @Operation(summary = "查询秒杀商品列表")
    @GetMapping("/products")
    public Result<List<SeckillProductVO>> listProducts(@ParameterObject SeckillProductQueryDTO queryDTO) {
        log.info("查询秒杀商品列表：{}", queryDTO);
        return seckillProductService.listProducts(queryDTO);
    }

    /**
     * 查询秒杀商品详情
     *
     * @param id 商品ID
     * @return 商品详情
     */
    @Operation(summary = "查询秒杀商品详情")
    @GetMapping("/products/{id}")
    public Result<SeckillProductVO> getProductDetail(@Parameter(description = "秒杀商品ID", example = "2001")
                                                     @PathVariable("id") @Min(1) Long id) {
        return seckillProductService.getProductDetail(id);
    }
}
