package com.flashsale.seckillservice.service;

import com.flashsale.common.domain.Result;
import com.flashsale.seckillservice.domain.dto.SeckillProductQueryDTO;
import com.flashsale.seckillservice.domain.vo.SeckillProductVO;

import java.util.List;

public interface SeckillProductService {

    Result<List<SeckillProductVO>> listProducts(SeckillProductQueryDTO queryDTO);

    Result<SeckillProductVO> getProductDetail(Long id);

    void loadStockToRedis();
}
