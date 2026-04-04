package com.flashsale.aiservice.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "商品解析请求")
public class ProductResolveRequestDTO {

    @NotBlank
    @Size(max = 500)
    @Schema(description = "用户原始问题", example = "iPhone 15 适合什么人")
    private String question;

    @Schema(description = "最多返回的候选商品数", example = "6")
    private Integer maxCandidates;
}
