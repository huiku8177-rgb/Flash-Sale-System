package com.flashsale.aiservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "商品解析结果")
public class ProductResolutionVO {

    @Schema(description = "解析出的搜索关键词", example = "iphone")
    private String keyword;

    @Schema(description = "是否已自动定位到唯一商品", example = "false")
    private boolean resolved;

    @Schema(description = "自动定位到的商品")
    private ProductCandidateVO selectedCandidate;

    @Schema(description = "候选商品列表")
    private List<ProductCandidateVO> candidates;
}
