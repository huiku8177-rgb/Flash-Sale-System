package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.client.ProductKnowledgeClient;
import com.flashsale.aiservice.client.SeckillKnowledgeClient;
import com.flashsale.aiservice.domain.dto.ProductKnowledgeDTO;
import com.flashsale.aiservice.domain.dto.ProductResolveRequestDTO;
import com.flashsale.aiservice.domain.dto.SeckillKnowledgeDTO;
import com.flashsale.aiservice.domain.vo.ProductCandidateVO;
import com.flashsale.aiservice.domain.vo.ProductResolutionVO;
import com.flashsale.aiservice.service.ProductResolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductResolutionServiceImpl implements ProductResolutionService {

    private static final int DEFAULT_LIMIT = 6;
    private static final double AUTO_RESOLVE_SCORE = 0.95d;
    private static final double CLEAR_LEAD_GAP = 0.15d;

    private static final List<String> NOISE_PHRASES = List.of(
            "适合什么人",
            "适合谁",
            "是做什么的",
            "有什么用",
            "支持七天无理由退货吗",
            "支持退货吗",
            "支持退款吗",
            "现在还有库存吗",
            "现在多少钱",
            "价格是多少",
            "卖点是什么",
            "主要卖点",
            "怎么样",
            "好不好",
            "值得买吗"
    );

    private static final List<String> NOISE_WORDS = List.of(
            "这款", "这个", "商品", "产品", "一下", "一下子", "请问",
            "适合", "什么人", "什么", "做什么", "用途", "卖点", "介绍", "详情",
            "退货", "退款", "售后", "发货", "配送", "库存", "价格", "多少", "多少钱",
            "现在", "还有", "支持", "吗", "呢", "呀", "啊", "手机", "耳机", "电脑", "手表"
    );

    private final ProductKnowledgeClient productKnowledgeClient;
    private final SeckillKnowledgeClient seckillKnowledgeClient;

    @Override
    public ProductResolutionVO resolve(ProductResolveRequestDTO request) {
        int limit = request.getMaxCandidates() == null || request.getMaxCandidates() <= 0
                ? DEFAULT_LIMIT
                : Math.min(request.getMaxCandidates(), 10);

        List<String> keywords = extractKeywords(request.getQuestion());
        Map<String, ProductCandidateVO> candidates = new LinkedHashMap<>();

        for (String keyword : keywords) {
            for (ProductKnowledgeDTO product : productKnowledgeClient.searchProducts(keyword)) {
                mergeCandidate(candidates, toCandidate(product, keyword, "normal"), limit);
            }
            for (SeckillKnowledgeDTO product : seckillKnowledgeClient.searchProducts(keyword)) {
                mergeCandidate(candidates, toCandidate(product, keyword, "seckill"), limit);
            }
            if (candidates.size() >= limit) {
                break;
            }
        }

        List<ProductCandidateVO> sorted = candidates.values().stream()
                .sorted((left, right) -> Double.compare(right.getScore(), left.getScore()))
                .limit(limit)
                .toList();

        ProductResolutionVO resolution = new ProductResolutionVO();
        resolution.setKeyword(keywords.isEmpty() ? "" : keywords.get(0));
        resolution.setCandidates(sorted);

        if (sorted.isEmpty()) {
            resolution.setResolved(false);
            return resolution;
        }

        ProductCandidateVO top = sorted.get(0);
        ProductCandidateVO second = sorted.size() > 1 ? sorted.get(1) : null;
        boolean hasClearLead = second == null || top.getScore() - second.getScore() >= CLEAR_LEAD_GAP;
        boolean autoResolved = top.getScore() >= AUTO_RESOLVE_SCORE && hasClearLead;

        resolution.setResolved(autoResolved);
        resolution.setSelectedCandidate(autoResolved ? top : null);
        return resolution;
    }

    private void mergeCandidate(Map<String, ProductCandidateVO> candidates, ProductCandidateVO candidate, int limit) {
        if (candidate == null || candidates.size() > limit * 2) {
            return;
        }
        String key = candidate.getProductType() + ":" + candidate.getProductId();
        ProductCandidateVO existing = candidates.get(key);
        if (existing == null || candidate.getScore() > existing.getScore()) {
            candidates.put(key, candidate);
        }
    }

    private ProductCandidateVO toCandidate(ProductKnowledgeDTO product, String keyword, String type) {
        if (product == null || product.getId() == null || !StringUtils.hasText(product.getName())) {
            return null;
        }
        ProductCandidateVO candidate = new ProductCandidateVO();
        candidate.setProductId(product.getId());
        candidate.setProductType(type);
        candidate.setName(product.getName());
        candidate.setSubtitle(product.getSubtitle());
        candidate.setPrice(product.getPrice());
        candidate.setScore(scoreCandidate(keyword, product.getName()));
        return candidate;
    }

    private ProductCandidateVO toCandidate(SeckillKnowledgeDTO product, String keyword, String type) {
        if (product == null || product.getId() == null || !StringUtils.hasText(product.getName())) {
            return null;
        }
        ProductCandidateVO candidate = new ProductCandidateVO();
        candidate.setProductId(product.getId());
        candidate.setProductType(type);
        candidate.setName(product.getName());
        candidate.setPrice(product.getSeckillPrice() != null ? product.getSeckillPrice() : product.getPrice());
        candidate.setScore(scoreCandidate(keyword, product.getName()));
        return candidate;
    }

    private double scoreCandidate(String keyword, String name) {
        String normalizedKeyword = compact(keyword);
        String normalizedName = compact(name);
        if (!StringUtils.hasText(normalizedKeyword) || !StringUtils.hasText(normalizedName)) {
            return 0d;
        }
        if (normalizedKeyword.equals(normalizedName)) {
            return 1d;
        }
        if (normalizedName.contains(normalizedKeyword)) {
            return 0.96d;
        }
        if (normalizedKeyword.contains(normalizedName)) {
            return 0.90d;
        }

        int overlap = 0;
        for (String token : splitTokens(keyword)) {
            if (normalizedName.contains(compact(token))) {
                overlap++;
            }
        }
        return overlap == 0 ? 0.5d : Math.min(0.89d, 0.55d + overlap * 0.12d);
    }

    private List<String> extractKeywords(String question) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        String normalized = normalize(question);
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }

        String cleaned = normalized;
        for (String phrase : NOISE_PHRASES) {
            cleaned = cleaned.replace(phrase, " ");
        }
        cleaned = collapseSpaces(cleaned);
        if (StringUtils.hasText(cleaned)) {
            keywords.add(cleaned);
        }

        String stripped = cleaned;
        for (String word : NOISE_WORDS) {
            stripped = stripped.replace(word, " ");
        }
        stripped = collapseSpaces(stripped);
        if (StringUtils.hasText(stripped)) {
            keywords.add(stripped);
        }

        for (String token : splitTokens(stripped)) {
            if (token.length() >= 2) {
                keywords.add(token);
            }
        }
        for (String token : splitTokens(cleaned)) {
            if (token.length() >= 2) {
                keywords.add(token);
            }
        }

        return new ArrayList<>(keywords).stream()
                .filter(StringUtils::hasText)
                .limit(4)
                .toList();
    }

    private List<String> splitTokens(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        return List.of(text.split("\\s+"));
    }

    private String normalize(String text) {
        return collapseSpaces(text == null
                ? ""
                : text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+", " "));
    }

    private String compact(String text) {
        return text == null
                ? ""
                : text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+", "");
    }

    private String collapseSpaces(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }
}
