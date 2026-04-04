package com.flashsale.aiservice.domain.po;

import com.flashsale.aiservice.domain.enums.KnowledgeSourceType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDocumentPO {

    private String id;
    private KnowledgeSourceType sourceType;
    private String sourceId;
    private String title;
    private String content;
    private LocalDateTime updatedAt;
}
