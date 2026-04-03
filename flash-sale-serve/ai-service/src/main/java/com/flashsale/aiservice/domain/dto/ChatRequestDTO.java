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
}
