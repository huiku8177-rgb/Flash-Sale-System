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

/**
 * 商品解析服务实现类
 *
 * 核心功能：
 * 根据用户问题中的关键词，在普通商品库和秒杀商品库中搜索匹配的商品，
 * 计算每个候选商品与关键词的匹配得分，并按得分排序返回。
 *
 * 自动解析判断：
 * 当最高得分的候选商品满足以下条件时，自动选定该商品：
 * - 得分 >= AUTO_RESOLVE_SCORE (0.95)
 * - 且与第二名候选商品的得分差距 >= CLEAR_LEAD_GAP (0.15)
 *
 * 主要应用于：
 * - 用户问题未明确指定商品ID时，尝试从问题文本中识别目标商品
 * - 为后续的 RAG 检索或对话上下文提供商品锚点
 */
@Service
@RequiredArgsConstructor
public class ProductResolutionServiceImpl implements ProductResolutionService {

  // 默认返回候选商品数量上限
  private static final int DEFAULT_LIMIT = 6;

  // 自动解析阈值：得分达到此值且领先第二名足够多时，自动选定商品
  private static final double AUTO_RESOLVE_SCORE = 0.95d;

  // 领先差距阈值：第一名得分需超过第二名此数值以上，方可自动选定
  private static final double CLEAR_LEAD_GAP = 0.15d;

  /**
   * 噪声短语列表
   *
   * 这些短语通常出现在问题中，但不包含商品名称信息，
   * 在提取关键词时需要被移除，以免干扰商品名称匹配。
   */
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

  /**
   * 噪声词汇列表
   *
   * 包括常见的指代词、修饰词、疑问词等，在更精细的文本清理中会被移除。
   */
  private static final List<String> NOISE_WORDS = List.of(
    "这款", "这个", "商品", "产品", "一下", "一下子", "请问",
    "适合", "什么人", "什么", "做什么", "用途", "卖点", "介绍", "详情",
    "退货", "退款", "售后", "发货", "配送", "库存", "价格", "多少", "多少钱",
    "现在", "还有", "支持", "吗", "呢", "呀", "啊", "手机", "耳机", "电脑", "手表"
  );

  // 依赖注入的商品知识库客户端
  private final ProductKnowledgeClient productKnowledgeClient;
  private final SeckillKnowledgeClient seckillKnowledgeClient;

  /**
   * 解析用户问题中的商品候选
   *
   * 处理流程：
   * 1. 从问题中提取关键词（最多4个）
   * 2. 对每个关键词分别在普通商品库和秒杀商品库中搜索
   * 3. 合并候选商品，计算得分并排序
   * 4. 判断是否满足自动解析条件
   *
   * @param request 包含问题文本和可选的最大候选数量限制
   * @return 解析结果，包含候选商品列表及自动选定的商品（若有）
   */
  @Override
  public ProductResolutionVO resolve(ProductResolveRequestDTO request) {
    // 确定返回的候选数量上限（最多10个）
    int limit = request.getMaxCandidates() == null || request.getMaxCandidates() <= 0
      ? DEFAULT_LIMIT
      : Math.min(request.getMaxCandidates(), 10);

    List<String> keywords = extractKeywords(request.getQuestion());
    Map<String, ProductCandidateVO> candidates = new LinkedHashMap<>();

    // 遍历关键词，搜索商品并合并候选
    for (String keyword : keywords) {
      for (ProductKnowledgeDTO product : productKnowledgeClient.searchProducts(keyword)) {
        mergeCandidate(candidates, toCandidate(product, keyword, "normal"), limit);
      }
      for (SeckillKnowledgeDTO product : seckillKnowledgeClient.searchProducts(keyword)) {
        mergeCandidate(candidates, toCandidate(product, keyword, "seckill"), limit);
      }
      // 候选数量达到上限后提前结束搜索
      if (candidates.size() >= limit) {
        break;
      }
    }

    // 按得分降序排序，并截取指定数量
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

    // 自动解析逻辑：第一名得分足够高，且领先第二名足够多
    ProductCandidateVO top = sorted.get(0);
    ProductCandidateVO second = sorted.size() > 1 ? sorted.get(1) : null;
    boolean hasClearLead = second == null || top.getScore() - second.getScore() >= CLEAR_LEAD_GAP;
    boolean autoResolved = top.getScore() >= AUTO_RESOLVE_SCORE && hasClearLead;

    resolution.setResolved(autoResolved);
    resolution.setSelectedCandidate(autoResolved ? top : null);
    return resolution;
  }

  /**
   * 合并候选商品到结果Map中
   *
   * 策略：
   * - 使用 "商品类型:商品ID" 作为唯一键
   * - 若已存在相同商品，保留得分更高的记录
   * - 若候选数量超过 limit * 2，停止合并以控制性能
   *
   * @param candidates 现有候选商品Map
   * @param candidate  待合并的候选商品
   * @param limit      最终返回的数量上限
   */
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

  /**
   * 将普通商品DTO转换为候选商品VO
   *
   * @param product 普通商品数据
   * @param keyword 用于匹配的关键词
   * @param type    商品类型标识（"normal"）
   * @return 候选商品对象
   */
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

  /**
   * 将秒杀商品DTO转换为候选商品VO
   *
   * @param product 秒杀商品数据
   * @param keyword 用于匹配的关键词
   * @param type    商品类型标识（"seckill"）
   * @return 候选商品对象
   */
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

  /**
   * 计算关键词与商品名称的匹配得分
   *
   * 评分规则（按匹配程度递减）：
   * - 完全相等：1.0
   * - 商品名包含关键词：0.96
   * - 关键词包含商品名：0.90
   * - 分词重叠（基础0.55，每个重叠分词+0.12，上限0.89）
   * - 无重叠：0.5（作为兜底，避免完全不相关的商品进入）
   *
   * @param keyword 搜索关键词
   * @param name    商品名称
   * @return 匹配得分（0.0 ~ 1.0）
   */
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

    // 基于分词重叠计算得分
    int overlap = 0;
    for (String token : splitTokens(keyword)) {
      if (normalizedName.contains(compact(token))) {
        overlap++;
      }
    }
    return overlap == 0 ? 0.5d : Math.min(0.89d, 0.55d + overlap * 0.12d);
  }

  /**
   * 从问题文本中提取商品相关的关键词
   *
   * 处理流程：
   * 1. 规范化文本（转小写、去除非字母数字中文的字符）
   * 2. 移除噪声短语和噪声词汇
   * 3. 将清理后的短语作为关键词候选
   * 4. 进一步分词，提取长度≥2的token
   * 5. 返回前4个有效关键词
   *
   * @param question 用户问题原文
   * @return 关键词列表
   */
  private List<String> extractKeywords(String question) {
    LinkedHashSet<String> keywords = new LinkedHashSet<>();
    String normalized = normalize(question);
    if (!StringUtils.hasText(normalized)) {
      return List.of();
    }

    // 移除噪声短语
    String cleaned = normalized;
    for (String phrase : NOISE_PHRASES) {
      cleaned = cleaned.replace(phrase, " ");
    }
    cleaned = collapseSpaces(cleaned);
    if (StringUtils.hasText(cleaned)) {
      keywords.add(cleaned);
    }

    // 移除噪声词汇
    String stripped = cleaned;
    for (String word : NOISE_WORDS) {
      stripped = stripped.replace(word, " ");
    }
    stripped = collapseSpaces(stripped);
    if (StringUtils.hasText(stripped)) {
      keywords.add(stripped);
    }

    // 提取分词token
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

    // 过滤空字符串，返回最多4个
    return new ArrayList<>(keywords).stream()
      .filter(StringUtils::hasText)
      .limit(4)
      .toList();
  }

  /**
   * 按空白字符分割文本，返回token列表
   */
  private List<String> splitTokens(String text) {
    if (!StringUtils.hasText(text)) {
      return List.of();
    }
    return List.of(text.split("\\s+"));
  }

  /**
   * 文本规范化
   *
   * 操作：
   * - 转小写
   * - 将非字母、非数字、非中文字符替换为空格
   * - 合并多余空格
   *
   * @param text 原始文本
   * @return 规范化后的文本
   */
  private String normalize(String text) {
    return collapseSpaces(text == null
      ? ""
      : text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+", " "));
  }

  /**
   * 文本紧凑化（用于精确匹配）
   *
   * 与 normalize 的区别：
   * - 直接删除所有特殊字符，不保留空格
   * - 适用于字符串包含判断
   */
  private String compact(String text) {
    return text == null
      ? ""
      : text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+", "");
  }

  /**
   * 合并多个连续空格，去除首尾空格
   */
  private String collapseSpaces(String text) {
    return text == null ? "" : text.trim().replaceAll("\\s+", " ");
  }
}
