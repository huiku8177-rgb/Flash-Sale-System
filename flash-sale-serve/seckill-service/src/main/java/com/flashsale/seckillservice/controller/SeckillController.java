package com.flashsale.seckillservice.controller;

import com.flashsale.common.domain.Result;
import com.flashsale.seckillservice.domain.dto.SeckillRequestDTO;
import com.flashsale.seckillservice.domain.vo.SeckillResultVO;
import com.flashsale.seckillservice.domain.vo.SeckillStatusVO;
import com.flashsale.seckillservice.service.SeckillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillController
 * @date 2026/3/20 00:00
 */


@Slf4j
@Tag(name = "秒杀流程", description = "秒杀提交与结果查询接口")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * 提交秒杀请求
     *
     * @param productId 商品ID
     * @param userId 用户ID
     * @return 秒杀结果
     */
    @Operation(summary = "提交秒杀请求")
    @PostMapping("/{productId}")
    public Result<SeckillResultVO> seckill(@Parameter(description = "秒杀商品ID", example = "2001")
                                           @PathVariable("productId") @Min(1) Long productId,
                                           @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        log.info("用户 {} 发起秒杀，商品ID={}", userId, productId);
        SeckillRequestDTO requestDTO = new SeckillRequestDTO();
        requestDTO.setProductId(productId);
        requestDTO.setUserId(userId);
        return seckillService.seckill(requestDTO);
    }

    /**
     * 查询秒杀处理结果
     *
     * @param productId 商品ID
     * @param userId 用户ID
     * @return 秒杀状态
     */
    @Operation(summary = "查询秒杀结果")
    @GetMapping("/result/{productId}")
    public Result<SeckillStatusVO> getSeckillResult(@Parameter(description = "秒杀商品ID", example = "2001")
                                                    @PathVariable("productId") @Min(1) Long productId,
                                                    @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return seckillService.getSeckillResult(userId, productId);
    }
}
