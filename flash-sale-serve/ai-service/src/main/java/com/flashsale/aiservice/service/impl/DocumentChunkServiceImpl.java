package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.client.EmbeddingClient;
import com.flashsale.aiservice.domain.po.KnowledgeChunkPO;
import com.flashsale.aiservice.domain.po.KnowledgeDocumentPO;
import com.flashsale.aiservice.service.DocumentChunkService;
import com.flashsale.aiservice.util.TextChunkUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档分块服务实现类
 *
 * 负责将完整的知识文档切分为多个短文本片段（Chunk），
 * 并为每个片段生成对应的向量嵌入（Embedding），以便存入向量数据库进行相似度检索。
 *
 * 主要流程：
 * 1. 调用文本分块工具按语义边界切分文档内容
 * 2. 遍历每个文本片段，调用向量化客户端生成 Embedding 向量
 * 3. 组装 KnowledgeChunkPO 对象，包含分块内容、向量、及源文档元数据
 */
@Service
@RequiredArgsConstructor
public class DocumentChunkServiceImpl implements DocumentChunkService {

  // 向量化客户端，用于将文本转换为向量表示
  private final EmbeddingClient embeddingClient;

  /**
   * 对单个文档进行分块处理
   *
   * @param document 待分块的知识文档（包含标题、内容、来源类型等）
   * @return 该文档切分出的所有知识分块列表（每个分块已包含向量数据）
   */
  @Override
  public List<KnowledgeChunkPO> chunk(KnowledgeDocumentPO document) {
    // 使用工具类将文档内容切分为多个短文本片段
    List<String> texts = TextChunkUtils.chunk(document.getContent());

    // 预分配列表容量，避免扩容开销
    List<KnowledgeChunkPO> chunks = new ArrayList<>(texts.size());

    // 遍历每个文本片段，构建知识分块对象
    for (int index = 0; index < texts.size(); index++) {
      KnowledgeChunkPO chunk = new KnowledgeChunkPO();

      // 生成唯一的分块ID：文档ID + 序号后缀
      chunk.setId(document.getId() + "-chunk-" + index);

      // 继承源文档的元数据
      chunk.setDocumentId(document.getId());
      chunk.setSourceType(document.getSourceType());
      chunk.setSourceId(document.getSourceId());
      chunk.setTitle(document.getTitle());

      // 设置当前分块的文本内容
      chunk.setContent(texts.get(index));

      // 调用向量化服务生成该文本的 Embedding 向量
      chunk.setEmbedding(embeddingClient.embed(texts.get(index)));

      chunks.add(chunk);
    }
    return chunks;
  }
}
