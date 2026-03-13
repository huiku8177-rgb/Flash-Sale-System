package com.flashsale.seckillservice.service.impl;

import com.flashsale.common.domain.Result;
import com.flashsale.seckillservice.domain.dto.SeckillRequestDTO;
import com.flashsale.seckillservice.domain.vo.SeckillResultVO;
import com.flashsale.seckillservice.mapper.SeckillMapper;
import com.flashsale.seckillservice.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillServiceImpl
 * @date 2026/3/13 17:00
 */
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final SeckillMapper seckillMapper;

    @Override
    public Result<SeckillResultVO> seckill(SeckillRequestDTO requestDTO) {
        return Result.success(seckillMapper.buildSeckillResult(requestDTO));
    }
}
