package com.flashsale.aiservice.domain.dto;

import com.flashsale.aiservice.domain.enums.SyncType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "知识同步请求")
public class KnowledgeSyncRequestDTO {

    @Schema(description = "同步模式", example = "FULL")
    private SyncType syncType = SyncType.FULL;

    @Schema(description = "是否强制重建索引", example = "true")
    private boolean forceRebuild = true;

    @Schema(description = "增量同步时可指定商品 ID", example = "1001")
    private Long productId;
}
