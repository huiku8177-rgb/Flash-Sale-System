package com.flashsale.aiservice.domain.po;

import com.flashsale.aiservice.domain.enums.KnowledgeSourceType;
import lombok.Data;

import java.util.List;

@Data
public class KnowledgeChunkPO {

    private String id;
    private String documentId;
    private KnowledgeSourceType sourceType;
    private String sourceId;
    private String title;
    private String content;
    private List<Double> embedding;
}
