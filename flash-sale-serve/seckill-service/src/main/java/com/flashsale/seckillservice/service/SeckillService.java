package com.flashsale.seckillservice.service;

import com.flashsale.common.domain.Result;
import com.flashsale.seckillservice.domain.dto.SeckillRequestDTO;
import com.flashsale.seckillservice.domain.vo.SeckillResultVO;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillService
 * @date 2026/3/13 17:00
 */
public interface SeckillService {

    Result<SeckillResultVO> seckill(SeckillRequestDTO requestDTO);
}
