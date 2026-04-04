package com.flashsale.aiservice.controller;

import com.flashsale.aiservice.domain.dto.KnowledgeSyncRequestDTO;
import com.flashsale.aiservice.domain.vo.KnowledgeStatsVO;
import com.flashsale.aiservice.domain.vo.KnowledgeSyncResultVO;
import com.flashsale.aiservice.service.KnowledgeSyncService;
import com.flashsale.common.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "知识管理", description = "知识同步与统计接口")
@RequestMapping("/ai/knowledge")
public class KnowledgeController {

    private final KnowledgeSyncService knowledgeSyncService;

    @Operation(summary = "触发知识同步")
    @PostMapping("/sync")
    public Result<KnowledgeSyncResultVO> sync(@RequestBody(required = false) KnowledgeSyncRequestDTO request) {
        KnowledgeSyncRequestDTO actualRequest = request == null ? new KnowledgeSyncRequestDTO() : request;
        return Result.success(knowledgeSyncService.sync(actualRequest));
    }

    @Operation(summary = "查询同步任务状态")
    @GetMapping("/sync/{taskId}")
    public Result<KnowledgeSyncResultVO> getSyncTask(@PathVariable String taskId) {
        return Result.success(knowledgeSyncService.getTask(taskId));
    }

    @Operation(summary = "查询知识库统计")
    @GetMapping("/stats")
    public Result<KnowledgeStatsVO> getStats() {
        return Result.success(knowledgeSyncService.getStats());
    }
}
