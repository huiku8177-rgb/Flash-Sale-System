package com.flashsale.orderservice.service.impl;

import com.flashsale.common.domain.Result;
import com.flashsale.orderservice.domain.vo.SeckillOrderVO;
import com.flashsale.orderservice.mapper.SeckillOrderMapper;
import com.flashsale.orderservice.service.SeckillOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillOrderServiceImpl
 * @date 2026/3/13 17:00
 */
@Service
@RequiredArgsConstructor
public class SeckillOrderServiceImpl implements SeckillOrderService {

    private final SeckillOrderMapper seckillOrderMapper;

    @Override
    public Result<List<SeckillOrderVO>> listOrders() {
        return Result.success(seckillOrderMapper.listOrders());
    }

    @Override
    public Result<SeckillOrderVO> getOrderDetail(Long id) {
        return Result.success(seckillOrderMapper.getOrderDetail(id));
    }
}
