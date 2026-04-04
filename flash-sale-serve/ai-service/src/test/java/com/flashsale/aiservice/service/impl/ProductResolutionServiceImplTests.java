package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.client.ProductKnowledgeClient;
import com.flashsale.aiservice.client.SeckillKnowledgeClient;
import com.flashsale.aiservice.domain.dto.ProductKnowledgeDTO;
import com.flashsale.aiservice.domain.dto.ProductResolveRequestDTO;
import com.flashsale.aiservice.domain.dto.SeckillKnowledgeDTO;
import com.flashsale.aiservice.domain.vo.ProductResolutionVO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

class ProductResolutionServiceImplTests {

    @Test
    void resolveReturnsCandidatesForNaturalLanguageQuestion() {
        ProductKnowledgeClient productClient = Mockito.mock(ProductKnowledgeClient.class);
        SeckillKnowledgeClient seckillClient = Mockito.mock(SeckillKnowledgeClient.class);

        ProductKnowledgeDTO iphone15 = product(1001L, "iPhone 15");
        ProductKnowledgeDTO iphone15Pro = product(1002L, "iPhone 15 Pro");

        Mockito.when(productClient.searchProducts(anyString())).thenReturn(List.of(iphone15, iphone15Pro));
        Mockito.when(seckillClient.searchProducts(anyString())).thenReturn(List.of());

        ProductResolutionServiceImpl service = new ProductResolutionServiceImpl(productClient, seckillClient);
        ProductResolveRequestDTO request = new ProductResolveRequestDTO();
        request.setQuestion("iPhone手机适合什么人");

        ProductResolutionVO result = service.resolve(request);

        assertFalse(result.isResolved());
        assertEquals(2, result.getCandidates().size());
        assertTrue(result.getCandidates().stream().anyMatch(item -> "iPhone 15".equals(item.getName())));
    }

    @Test
    void resolveAutoSelectsSingleExactCandidate() {
        ProductKnowledgeClient productClient = Mockito.mock(ProductKnowledgeClient.class);
        SeckillKnowledgeClient seckillClient = Mockito.mock(SeckillKnowledgeClient.class);

        ProductKnowledgeDTO airpods = product(2001L, "AirPods Pro");

        Mockito.when(productClient.searchProducts(anyString())).thenReturn(List.of(airpods));
        Mockito.when(seckillClient.searchProducts(anyString())).thenReturn(List.of());

        ProductResolutionServiceImpl service = new ProductResolutionServiceImpl(productClient, seckillClient);
        ProductResolveRequestDTO request = new ProductResolveRequestDTO();
        request.setQuestion("AirPods Pro 支持退货吗");

        ProductResolutionVO result = service.resolve(request);

        assertTrue(result.isResolved());
        assertEquals("AirPods Pro", result.getSelectedCandidate().getName());
    }

    private ProductKnowledgeDTO product(Long id, String name) {
        ProductKnowledgeDTO product = new ProductKnowledgeDTO();
        product.setId(id);
        product.setName(name);
        product.setPrice(new BigDecimal("5999.00"));
        return product;
    }
}
