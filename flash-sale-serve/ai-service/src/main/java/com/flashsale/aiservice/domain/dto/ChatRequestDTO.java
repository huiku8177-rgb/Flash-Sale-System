package com.flashsale.aiservice.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "AI 问答请求参数")
public class ChatRequestDTO {

    @NotBlank
    @Size(max = 500)
    @Schema(description = "用户问题", example = "这款商品支持七天无理由退货吗？")
    private String question;

    @Schema(description = "关联商品 ID，商品详情页接入时建议传入", example = "1001")
    private Long productId;

    // Dual-track context hydration: accept productName from the caller when already known.
    @Size(max = 128)
    @Schema(description = "关联商品名称，已知时建议同步传入", example = "iPhone 15")
    private String productName;

    // Dual-track context hydration: accept productType but keep server-side auto repair for old callers.
    @Size(max = 32)
    @Schema(description = "关联商品类型，例如 normal / seckill", example = "normal")
    private String productType;

    @Size(max = 64)
    @Schema(description = "会话 ID，用于串联多轮上下文", example = "session-001")
    private String sessionId;

    @Size(max = 32)
    @Schema(description = "上下文来源，例如 product-detail、global-assistant", example = "product-detail")
    private String contextType;
}
