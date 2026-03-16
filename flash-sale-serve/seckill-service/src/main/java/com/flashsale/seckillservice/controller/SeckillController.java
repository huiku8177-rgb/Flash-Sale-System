package com.flashsale.seckillservice.controller;

import com.flashsale.common.domain.Result;
import com.flashsale.seckillservice.domain.dto.SeckillRequestDTO;
import com.flashsale.seckillservice.domain.vo.SeckillResultVO;
import com.flashsale.seckillservice.domain.vo.SeckillStatusVO;
import com.flashsale.seckillservice.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillController
 * @date 2026/3/13 14:51
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/seckill")
public class SeckillController {

    private final SeckillService seckillService;


    /**
     * 发起秒杀
     *
     * @param productId 商品ID
     * @return 秒杀结果
     */
    @PostMapping("/{productId}")
    public Result<SeckillResultVO> seckill(@PathVariable("productId") Long productId,
                                           @RequestHeader("X-User-Id") Long userId) {
        log.info("用户 {} 秒杀商品 {}", userId, productId);
        SeckillRequestDTO requestDTO = new SeckillRequestDTO();
        requestDTO.setProductId(productId);
        requestDTO.setUserId(userId);
        return seckillService.seckill(requestDTO);
    }

    /**
     * 获取秒杀结果
     *
     * @param productId 商品ID
     * @return 秒杀结果
     */
    @GetMapping("/result/{productId}")
    public Result<SeckillStatusVO> getSeckillResult(
            @PathVariable("productId") Long productId,
            @RequestHeader("X-User-Id") Long userId) {

        return seckillService.getSeckillResult(userId, productId);
    }
}
