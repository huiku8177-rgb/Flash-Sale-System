package com.flashsale.aiservice.service;

import com.flashsale.aiservice.domain.dto.ProductResolveRequestDTO;
import com.flashsale.aiservice.domain.vo.ProductResolutionVO;

public interface ProductResolutionService {

    ProductResolutionVO resolve(ProductResolveRequestDTO request);
}
