package com.flashsale.aiservice.controller;

import com.flashsale.aiservice.domain.dto.ChatRequestDTO;
import com.flashsale.aiservice.domain.dto.ProductResolveRequestDTO;
import com.flashsale.aiservice.domain.vo.ChatResponseVO;
import com.flashsale.aiservice.domain.vo.ChatSessionSummaryVO;
import com.flashsale.aiservice.domain.vo.ChatSessionVO;
import com.flashsale.aiservice.domain.vo.ProductResolutionVO;
import com.flashsale.aiservice.service.ProductResolutionService;
import com.flashsale.aiservice.service.RagChatService;
import com.flashsale.common.domain.Result;
import com.flashsale.common.web.RequestHeaderNames;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "AI 问答", description = "商品问答、会话管理与商品解析接口")
@RequestMapping("/ai/chat")
public class ChatController {

    private final RagChatService ragChatService;
    private final ProductResolutionService productResolutionService;

    @Operation(
            summary = "发起 AI 问答",
            description = "基于当前商品上下文、历史会话和知识库检索结果生成回答。",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ChatRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "商品详情页提问",
                                    value = """
                                            {
                                              "question": "这款商品的主要卖点是什么？",
                                              "productId": 4,
                                              "productName": "AirPods Pro",
                                              "productType": "seckill",
                                              "sessionId": "session-001",
                                              "contextType": "product-detail"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "问答成功")
    @PostMapping
    public Result<ChatResponseVO> chat(
            @Parameter(hidden = true)
            @RequestHeader(value = RequestHeaderNames.X_USER_ID, required = false) Long userId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ChatRequestDTO request) {
        log.info("AI chat request received, userId={}, sessionId={}, productId={}",
                userId, request.getSessionId(), request.getProductId());
        return Result.success(ragChatService.chat(userId, request));
    }

    @Operation(summary = "查询会话详情", description = "按会话 ID 返回当前会话的消息记录与上下文状态。")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/sessions/{sessionId}")
    public Result<ChatSessionVO> getSession(
            @Parameter(hidden = true)
            @RequestHeader(value = RequestHeaderNames.X_USER_ID, required = false) Long userId,
            @Parameter(description = "会话 ID", example = "session-001")
            @PathVariable String sessionId) {
        return Result.success(ragChatService.getSession(userId, sessionId));
    }

    @Operation(summary = "查询会话列表", description = "返回当前用户最近的会话摘要列表。")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/sessions")
    public Result<List<ChatSessionSummaryVO>> listSessions(
            @Parameter(hidden = true)
            @RequestHeader(value = RequestHeaderNames.X_USER_ID, required = false) Long userId,
            @Parameter(description = "返回条数上限", example = "20")
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        return Result.success(ragChatService.listSessions(userId, limit));
    }

    @Operation(summary = "删除会话", description = "删除当前用户指定的历史会话。")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(
            @Parameter(hidden = true)
            @RequestHeader(value = RequestHeaderNames.X_USER_ID, required = false) Long userId,
            @Parameter(description = "会话 ID", example = "session-001")
            @PathVariable String sessionId) {
        ragChatService.deleteSession(userId, sessionId);
        return Result.success();
    }

    @Operation(summary = "解析候选商品", description = "根据自然语言问题提取关键词并返回可能相关的商品候选项。")
    @ApiResponse(responseCode = "200", description = "解析成功")
    @PostMapping("/resolve-product")
    public Result<ProductResolutionVO> resolveProduct(
            @Valid @org.springframework.web.bind.annotation.RequestBody ProductResolveRequestDTO request) {
        return Result.success(productResolutionService.resolve(request));
    }
}
