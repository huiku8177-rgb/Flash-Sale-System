# AI-Service 模块优化方向

> 基于 2026-04-08 代码审查结果，梳理以下优化建议。按优先级从高到低排列。

---

## 🔴 P0 - 高优先级（影响性能与稳定性）

### 1. 远程服务重复调用问题

**现状**：`RagRouteExecutionService` 中 `buildRealtimeFacts()` 对同一 `productId` 最多调用 4 次远程服务（产品信息、秒杀信息、库存信息等），而 fallback 回退链 `buildCurrentProductDirectFallback()` 又会再次调用，一次请求可能产生 3-5 次相同的 HTTP 调用。

**优化方案**：
- 在 `buildRouteKnowledgeContext()` 阶段将 `ProductKnowledgeDTO` 缓存到 `RagChatContext` 中
- 后续 `buildRealtimeFacts()` 和 `buildCurrentProductDirectFallback()` 直接从 context 读取
- 引入请求级缓存（如 `RequestScoped` Bean 或 `ThreadLocal`），避免同一请求内重复查询

**预期收益**：减少 60-80% 的重复 HTTP 调用，显著降低响应延迟和服务负载

```java
// 示例：在 RagChatContext 中添加缓存字段
public class RagChatContext {
    // ... 已有字段
    private Map<Long, ProductKnowledgeDTO> productCache = new HashMap<>();
    
    public ProductKnowledgeDTO getCachedProduct(Long productId) {
        return productCache.get(productId);
    }
    
    public void cacheProduct(Long productId, ProductKnowledgeDTO dto) {
        productCache.put(productId, dto);
    }
}
```

### 2. Embedding 降级策略过于简陋

**现状**：`EmbeddingClient.embedSafely()` 的本地降级使用 16 维哈希向量，语义表达能力极差，几乎退化为精确匹配。

**优化方案**：
- 方案A：接入离线 Embedding 模型（如 ONNX Runtime 加载小型中文 Embedding 模型），保证降级时仍有语义能力
- 方案B：降级时切换为关键词匹配 + BM25 检索，而非向量检索
- 方案C：增加缓存层，缓存历史 Embedding 结果，降级时优先使用缓存

**预期收益**：API 不可用时知识检索质量大幅提升

---

## 🟠 P1 - 中优先级（影响准确性与可维护性）

### 3. 意图分类机制优化

**现状**：`RagChatServiceImpl.classifyIntent()` 仅通过关键词 `containsAny()` 匹配，"推荐"等词同时出现在多个意图判断中，边界误判风险高。

**优化方案**：
- 方案A：引入优先级机制，按意图置信度排序，高优先级优先匹配
- 方案B：改用 LLM 做意图分类（增加一次轻量调用，但准确率显著提升）
- 方案C：使用正则表达式 + 权重打分，而非简单 containsAny
- 方案D：维护互斥关键词表，避免歧义词跨意图

```java
// 示例：优先级 + 互斥关键词方案
private static final Map<ChatIntent, List<String>> INTENT_KEYWORDS = new LinkedHashMap<>();
static {
    // 按优先级顺序，先匹配高优先级
    INTENT_KEYWORDS.put(COMPARE_RECOMMENDATION, List.of("对比", "比较", "哪个好", "区别"));
    INTENT_KEYWORDS.put(PRODUCT_DISCOVERY, List.of("推荐", "有没有", "想找", "帮我选"));
    INTENT_KEYWORDS.put(REALTIME_STATUS, List.of("库存", "还有吗", "秒杀", "什么时候开始"));
    INTENT_KEYWORDS.put(POLICY_QA, List.of("退货", "包邮", "运费", "售后"));
    INTENT_KEYWORDS.put(PRODUCT_FACT, List.of("参数", "规格", "介绍", "详情"));
}
```

**预期收益**：意图识别准确率提升，减少错误路由

### 4. RagRouteExecutionService 职责拆分

**现状**：`RagRouteExecutionService` 有 1249 行代码，承担了所有路由执行逻辑，职责过重，可读性和可维护性差。

**优化方案**：
- 将每个路由的执行逻辑拆分为独立的 Service（如 `ProductFactExecutionService`、`RealtimeStatusExecutionService`）
- 将公共的 fallback 逻辑抽取到 `FallbackChainService`
- 将 `buildRealtimeFacts()` 和 `buildCurrentProductDirectFallback()` 抽取为 `RealtimeDataService`

**预期收益**：单文件行数降至 200-300 行，职责清晰，便于单独测试

### 5. 会话上下文管理增强

**现状**：问题改写（指代消解）依赖上下文中的产品名替换代词，但上下文窗口有限，长对话场景可能丢失关键信息。

**优化方案**：
- 引入滑动窗口，保留最近 N 轮对话
- 对重要实体（产品名、品牌等）做持久化标记，不因窗口滚动丢失
- 考虑使用 LLM 做摘要压缩，保留语义精华

---

## 🟡 P2 - 低优先级（优化体验与细节）

### 6. resolveStrategy() 缓存优化

**现状**：每次 chat 请求都重建 `EnumMap<ChatIntent, ChatRouteStrategy>`，虽然开销不大，但完全没必要。

**优化方案**：
- 将 strategy map 作为类成员变量，在 `@PostConstruct` 中初始化一次

```java
private final Map<ChatIntent, ChatRouteStrategy> strategyMap;

@PostConstruct
public void init() {
    strategyMap = new EnumMap<>(ChatIntent.class);
    strategyMap.put(ChatIntent.GREETING, greetingStrategy);
    strategyMap.put(ChatIntent.OUT_OF_SCOPE, outOfScopeStrategy);
    // ...
}
```

### 7. 日志与可观测性增强

**现状**：缺乏结构化日志，难以追踪一次完整请求的执行路径和耗时。

**优化方案**：
- 为每个请求生成 traceId，贯穿整个调用链
- 在关键节点（意图分类、策略路由、远程调用、LLM 调用）增加耗时日志
- 接入 Micrometer 指标，监控 LLM 调用成功率、平均耗时、fallback 触发率

### 8. 知识库同步机制优化

**现状**：`KnowledgeSyncServiceImpl` 全量同步知识，数据量大时效率低。

**优化方案**：
- 支持增量同步，基于更新时间戳只拉取变更数据
- 引入消息队列（如 RocketMQ），由产品服务在数据变更时主动推送
- 增加同步状态监控和失败重试机制

### 9. Prompt 模板外部化

**现状**：Prompt 模板硬编码在 `PromptBuilderServiceImpl` 中，调整需要改代码重新部署。

**优化方案**：
- 将 Prompt 模板抽取到配置文件或数据库
- 支持 Nacos 动态配置，热更新 Prompt
- 引入 A/B 测试框架，对比不同 Prompt 效果

---

## 📊 优化优先级总览

| 优先级 | 编号 | 优化项 | 预估工作量 | 预期收益 |
|--------|------|--------|-----------|---------|
| P0 | 1 | 远程服务重复调用 | 2-3天 | 减少60-80%重复HTTP |
| P0 | 2 | Embedding降级策略 | 3-5天 | 降级时检索质量提升 |
| P1 | 3 | 意图分类机制 | 2-3天 | 意图准确率提升 |
| P1 | 4 | 职责拆分 | 3-4天 | 可维护性提升 |
| P1 | 5 | 会话上下文管理 | 2-3天 | 长对话体验提升 |
| P2 | 6 | Strategy缓存 | 0.5天 | 微小性能提升 |
| P2 | 7 | 日志与可观测性 | 2-3天 | 问题排查效率提升 |
| P2 | 8 | 知识库增量同步 | 3-5天 | 同步效率提升 |
| P2 | 9 | Prompt模板外部化 | 2-3天 | 运维灵活性提升 |

---

## 🏗️ 架构演进建议（长期）

1. **引入向量数据库**：将 `InMemoryVectorStore` 替换为 Milvus/Qdrant，支持更大规模知识库和更高检索性能
2. **多轮对话状态机**：将当前线性流程改为状态机模式，支持更复杂的对话场景（如对比流程中的追问）
3. **流式响应**：LLM 调用改为 SSE 流式输出，提升用户体感响应速度
4. **Agent 架构**：从 RAG 管道演进为 Agent 架构，支持工具调用（查库存、下单等操作类意图）
5. **评测体系**：建立离线评测集，定期回归测试意图分类准确率和回答质量

---

*文档生成时间：2026-04-08*
*基于 ai-service 模块代码审查结果*
