package com.flashsale.aiservice.controller;

import com.flashsale.aiservice.domain.dto.ChatRequestDTO;
import com.flashsale.aiservice.domain.dto.ProductResolveRequestDTO;
import com.flashsale.aiservice.domain.vo.ChatResponseVO;
import com.flashsale.aiservice.domain.vo.ChatSessionVO;
import com.flashsale.aiservice.domain.vo.ProductResolutionVO;
import com.flashsale.aiservice.service.ProductResolutionService;
import com.flashsale.aiservice.service.RagChatService;
import com.flashsale.common.domain.Result;
import com.flashsale.common.web.RequestHeaderNames;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "AI问答", description = "商品知识智能客服接口")
@RequestMapping("/ai/chat")
public class ChatController {

    private final RagChatService ragChatService;
    private final ProductResolutionService productResolutionService;

    @Operation(summary = "商品知识问答")
    @PostMapping
    public Result<ChatResponseVO> chat(
            @Parameter(hidden = true)
            @RequestHeader(value = RequestHeaderNames.X_USER_ID, required = false) Long userId,
            @Valid @RequestBody ChatRequestDTO request) {
        log.info("AI chat request received, userId={}, sessionId={}, productId={}", userId, request.getSessionId(), request.getProductId());
        return Result.success(ragChatService.chat(userId, request));
    }

    @Operation(summary = "查询会话记录")
    @GetMapping("/sessions/{sessionId}")
    public Result<ChatSessionVO> getSession(
            @Parameter(hidden = true)
            @RequestHeader(value = RequestHeaderNames.X_USER_ID, required = false) Long userId,
            @PathVariable String sessionId) {
        return Result.success(ragChatService.getSession(userId, sessionId));
    }

    @Operation(summary = "根据自然语言问题解析候选商品")
    @PostMapping("/resolve-product")
    public Result<ProductResolutionVO> resolveProduct(@Valid @RequestBody ProductResolveRequestDTO request) {
        return Result.success(productResolutionService.resolve(request));
    }
}
