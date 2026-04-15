package com.flashsale.productservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.productservice.config.ProductCacheProperties;
import com.flashsale.productservice.domain.dto.ProductQueryDTO;
import com.flashsale.productservice.domain.vo.CartItemVO;
import com.flashsale.productservice.domain.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductReadCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ProductCacheProperties cacheProperties;

    public List<ProductVO> getProductList(ProductQueryDTO queryDTO) {
        String key = RedisKeys.productListCache(buildListFingerprint(queryDTO), getProductListCacheVersion());
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return objectMapper.readValue(value, productListType());
        } catch (Exception ex) {
            log.warn("Failed to load product list cache", ex);
            return null;
        }
    }

    public void cacheProductList(ProductQueryDTO queryDTO, List<ProductVO> products) {
        String key = RedisKeys.productListCache(buildListFingerprint(queryDTO), getProductListCacheVersion());
        try {
            stringRedisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(products),
                    Duration.ofSeconds(cacheProperties.getProduct().getListTtlSeconds())
            );
        } catch (Exception ex) {
            log.warn("Failed to cache product list", ex);
        }
    }

    public ProductVO getProductDetail(Long productId) {
        try {
            String value = stringRedisTemplate.opsForValue().get(RedisKeys.productDetailCache(productId));
            if (value == null) {
                return null;
            }
            return objectMapper.readValue(value, ProductVO.class);
        } catch (Exception ex) {
            log.warn("Failed to load product detail cache, productId={}", productId, ex);
            return null;
        }
    }

    public void cacheProductDetail(ProductVO product) {
        if (product == null || product.getId() == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisKeys.productDetailCache(product.getId()),
                    objectMapper.writeValueAsString(product),
                    Duration.ofSeconds(cacheProperties.getProduct().getDetailTtlSeconds())
            );
        } catch (Exception ex) {
            log.warn("Failed to cache product detail, productId={}", product.getId(), ex);
        }
    }

    public void evictProductCaches(Collection<Long> productIds) {
        if (productIds != null) {
            for (Long productId : productIds) {
                evictProductCache(productId);
            }
        }
        evictProductListNamespace();
    }

    public void evictProductCache(Long productId) {
        if (productId == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(RedisKeys.productDetailCache(productId));
        } catch (Exception ex) {
            log.warn("Failed to evict product detail cache, productId={}", productId, ex);
        }
    }

    public void evictProductListNamespace() {
        try {
            stringRedisTemplate.opsForValue().increment(RedisKeys.productListCacheVersion());
        } catch (Exception ex) {
            log.warn("Failed to advance product list cache version", ex);
        }
    }

    public List<CartItemVO> getCartItems(Long userId) {
        try {
            String value = stringRedisTemplate.opsForValue().get(RedisKeys.cartItems(userId));
            if (value == null) {
                return null;
            }
            return objectMapper.readValue(value, cartItemListType());
        } catch (Exception ex) {
            log.warn("Failed to load cart cache, userId={}", userId, ex);
            return null;
        }
    }

    public void cacheCartItems(Long userId, List<CartItemVO> cartItems) {
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisKeys.cartItems(userId),
                    objectMapper.writeValueAsString(cartItems),
                    Duration.ofSeconds(cacheProperties.getCart().getTtlSeconds())
            );
        } catch (Exception ex) {
            log.warn("Failed to cache cart items, userId={}", userId, ex);
        }
    }

    public void evictCartItems(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(RedisKeys.cartItems(userId));
        } catch (Exception ex) {
            log.warn("Failed to evict cart cache, userId={}", userId, ex);
        }
    }

    private long getProductListCacheVersion() {
        try {
            String value = stringRedisTemplate.opsForValue().get(RedisKeys.productListCacheVersion());
            if (value == null) {
                return 0L;
            }
            return Long.parseLong(value);
        } catch (Exception ex) {
            log.warn("Failed to load product list cache version", ex);
            return 0L;
        }
    }

    private String buildListFingerprint(ProductQueryDTO queryDTO) {
        try {
            return objectMapper.writeValueAsString(queryDTO == null ? new ProductQueryDTO() : queryDTO);
        } catch (JsonProcessingException ex) {
            return String.valueOf(queryDTO);
        }
    }

    private JavaType productListType() {
        return objectMapper.getTypeFactory().constructCollectionType(List.class, ProductVO.class);
    }

    private JavaType cartItemListType() {
        return objectMapper.getTypeFactory().constructCollectionType(List.class, CartItemVO.class);
    }
}
