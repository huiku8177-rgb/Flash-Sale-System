package com.flashsale.seckillservice.controller;

import com.flashsale.common.domain.Result;
import com.flashsale.seckillservice.domain.dto.SeckillRequestDTO;
import com.flashsale.seckillservice.domain.vo.SeckillResultVO;
import com.flashsale.seckillservice.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillController
 * @date 2026/3/13 14:51
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    @PostMapping("/{productId}")
    public Result<SeckillResultVO> seckill(@PathVariable Long productId,
                                           @RequestBody(required = false) SeckillRequestDTO requestDTO) {
        if (requestDTO == null) {
            requestDTO = new SeckillRequestDTO();
        }
        requestDTO.setProductId(productId);
        return seckillService.seckill(requestDTO);
    }
}
