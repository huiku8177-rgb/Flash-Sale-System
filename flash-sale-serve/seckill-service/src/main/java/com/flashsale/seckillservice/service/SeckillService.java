package com.flashsale.seckillservice.service;

import com.flashsale.common.domain.Result;
import com.flashsale.seckillservice.domain.dto.SeckillRequestDTO;
import com.flashsale.seckillservice.domain.vo.SeckillResultVO;
import com.flashsale.seckillservice.domain.vo.SeckillStatusVO;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillService
 * @date 2026/3/13 17:00
 */
public interface SeckillService {

    /**
     * 发起秒杀
     *
     * @param requestDTO 秒杀请求参数
     * @return 秒杀结果
     */
    Result<SeckillResultVO> seckill(SeckillRequestDTO requestDTO);

    /**
     * 获取秒杀结果
     *
     * @param userId     用户ID
     * @param productId  商品ID
     * @return 秒杀结果
     */
    Result<SeckillStatusVO> getSeckillResult(Long userId, Long productId);
}
