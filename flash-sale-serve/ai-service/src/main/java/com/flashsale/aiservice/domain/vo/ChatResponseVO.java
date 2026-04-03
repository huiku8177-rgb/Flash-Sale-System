package com.flashsale.aiservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI 问答响应结果")
public class ChatResponseVO {

    @Schema(description = "回答内容", example = "支持七天无理由退货，具体以商品详情页说明为准。")
    private String answer;

    @Schema(description = "回答参考来源", example = "[\"商品详情\", \"售后说明\"]")
    private List<String> sources;
}
