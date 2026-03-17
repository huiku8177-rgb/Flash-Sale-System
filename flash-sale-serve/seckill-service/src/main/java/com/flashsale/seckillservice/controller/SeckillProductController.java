package com.flashsale.seckillservice.controller;

import com.flashsale.common.domain.Result;
import com.flashsale.seckillservice.domain.dto.SeckillProductQueryDTO;
import com.flashsale.seckillservice.domain.vo.SeckillProductVO;
import com.flashsale.seckillservice.service.SeckillProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seckill-product")
@Slf4j
public class SeckillProductController {

    private final SeckillProductService seckillProductService;

    @GetMapping("/products")
    public Result<List<SeckillProductVO>> listProducts(SeckillProductQueryDTO queryDTO) {
        log.info("查询秒杀商品列表: {}", queryDTO);
        return seckillProductService.listProducts(queryDTO);
    }

    @GetMapping("/products/{id}")
    public Result<SeckillProductVO> getProductDetail(@PathVariable("id") Long id) {
        return seckillProductService.getProductDetail(id);
    }
}
