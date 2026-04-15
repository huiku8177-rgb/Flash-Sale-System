package com.flashsale.productservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "flash-sale.cache")
public class ProductCacheProperties {

    private final Product product = new Product();
    private final Cart cart = new Cart();

    @Getter
    @Setter
    public static class Product {
        private long listTtlSeconds = 60L;
        private long detailTtlSeconds = 60L;
    }

    @Getter
    @Setter
    public static class Cart {
        private long ttlSeconds = 120L;
    }
}
