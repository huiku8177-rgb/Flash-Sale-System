package com.flashsale.productservice.service;

import com.flashsale.productservice.mapper.CartMapper;
import com.flashsale.productservice.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductOrderLocalTxService {

    private static final int UNSELECTED = 0;

    private final ProductMapper productMapper;
    private final CartMapper cartMapper;
    private final ProductReadCacheService productReadCacheService;

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void reserveStock(Map<Long, Integer> mergedItems) {
        for (Map.Entry<Long, Integer> entry : mergedItems.entrySet()) {
            int updated = productMapper.decreaseStock(entry.getKey(), entry.getValue());
            if (updated <= 0) {
                throw new IllegalStateException("扣减普通商品库存失败，商品ID=" + entry.getKey());
            }
        }
        productReadCacheService.evictProductCaches(mergedItems.keySet());
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void restoreStock(Map<Long, Integer> mergedItems) {
        for (Map.Entry<Long, Integer> entry : mergedItems.entrySet()) {
            productMapper.increaseStock(entry.getKey(), entry.getValue());
        }
        productReadCacheService.evictProductCaches(mergedItems.keySet());
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void removeCheckedOutCartItems(Long userId, List<Long> cartItemIds) {
        if (CollectionUtils.isEmpty(cartItemIds)) {
            return;
        }
        cartMapper.deleteCartItemsByIds(userId, cartItemIds);
        productReadCacheService.evictCartItems(userId);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void unselectCartItems(Long userId, List<Long> cartItemIds) {
        if (CollectionUtils.isEmpty(cartItemIds)) {
            return;
        }
        cartMapper.updateSelectedByIds(userId, cartItemIds, UNSELECTED);
        productReadCacheService.evictCartItems(userId);
    }
}
