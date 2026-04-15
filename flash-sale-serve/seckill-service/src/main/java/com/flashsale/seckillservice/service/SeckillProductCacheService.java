package com.flashsale.seckillservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.seckillservice.config.SeckillBusinessProperties;
import com.flashsale.seckillservice.domain.dto.SeckillProductQueryDTO;
import com.flashsale.seckillservice.domain.vo.SeckillProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillProductCacheService {

    private static final TypeReference<List<SeckillProductVO>> PRODUCT_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SeckillBusinessProperties seckillBusinessProperties;

    public SeckillProductVO getProductDetail(Long productId) {
        if (productId == null) {
            return null;
        }
        SeckillProductVO cached = readValue(RedisKeys.seckillProductDetailCache(productId), SeckillProductVO.class);
        return applyRealtimeStock(cached);
    }

    public void cacheProductDetail(SeckillProductVO product) {
        if (product == null) {
            return;
        }
        cacheProductDetail(product.getId(), product);
    }

    public void cacheProductDetail(Long productId, SeckillProductVO product) {
        if (productId == null || product == null) {
            return;
        }
        writeValue(
                RedisKeys.seckillProductDetailCache(productId),
                product,
                Duration.ofSeconds(seckillBusinessProperties.getCache().getDetailTtlSeconds())
        );
    }

    public List<SeckillProductVO> getProductList(SeckillProductQueryDTO queryDTO) {
        List<SeckillProductVO> cached = readValue(
                RedisKeys.seckillProductListCache(fingerprint(queryDTO), getListVersion()),
                PRODUCT_LIST_TYPE
        );
        return applyRealtimeStock(cached);
    }

    public void cacheProductList(SeckillProductQueryDTO queryDTO, List<SeckillProductVO> products) {
        if (products == null) {
            return;
        }
        writeValue(
                RedisKeys.seckillProductListCache(fingerprint(queryDTO), getListVersion()),
                products,
                Duration.ofSeconds(seckillBusinessProperties.getCache().getListTtlSeconds())
        );
    }

    private long getListVersion() {
        try {
            String version = stringRedisTemplate.opsForValue().get(RedisKeys.seckillProductListCacheVersion());
            return StringUtils.hasText(version) ? Long.parseLong(version) : 0L;
        } catch (Exception ex) {
            log.warn("Failed to load seckill product list cache version", ex);
            return 0L;
        }
    }

    public SeckillProductVO applyRealtimeStock(SeckillProductVO product) {
        if (product == null || product.getId() == null) {
            return product;
        }

        try {
            String stock = stringRedisTemplate.opsForValue().get(RedisKeys.seckillStock(product.getId()));
            if (StringUtils.hasText(stock)) {
                product.setStock(Integer.parseInt(stock));
            }
        } catch (Exception ex) {
            log.warn("Failed to load realtime seckill stock, productId={}", product.getId(), ex);
        }
        return product;
    }

    public List<SeckillProductVO> applyRealtimeStock(List<SeckillProductVO> products) {
        if (products == null || products.isEmpty()) {
            return products;
        }

        List<SeckillProductVO> hydrated = new ArrayList<>(products.size());
        for (SeckillProductVO product : products) {
            hydrated.add(applyRealtimeStock(product));
        }
        return hydrated;
    }

    private <T> T readValue(String key, Class<T> type) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(cached)) {
                return null;
            }
            return objectMapper.readValue(cached, type);
        } catch (Exception ex) {
            log.warn("Failed to read seckill cache, key={}", key, ex);
            tryDelete(key);
            return null;
        }
    }

    private <T> T readValue(String key, TypeReference<T> typeReference) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(cached)) {
                return null;
            }
            return objectMapper.readValue(cached, typeReference);
        } catch (Exception ex) {
            log.warn("Failed to read seckill list cache, key={}", key, ex);
            tryDelete(key);
            return null;
        }
    }

    private void writeValue(String key, Object value, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception ex) {
            log.warn("Failed to write seckill cache, key={}", key, ex);
        }
    }

    private void tryDelete(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception ex) {
            log.warn("Failed to delete seckill cache, key={}", key, ex);
        }
    }

    private String fingerprint(SeckillProductQueryDTO queryDTO) {
        SeckillProductQueryDTO query = queryDTO == null ? new SeckillProductQueryDTO() : queryDTO;
        String name = StringUtils.hasText(query.getName()) ? query.getName().trim() : "";
        return "name=" + name + "|status=" + query.getStatus();
    }
}
