package com.flashsale.aiservice.controller;

import com.flashsale.aiservice.domain.dto.KnowledgeSyncRequestDTO;
import com.flashsale.aiservice.domain.vo.KnowledgeStatsVO;
import com.flashsale.aiservice.domain.vo.KnowledgeSyncResultVO;
import com.flashsale.aiservice.service.KnowledgeSyncService;
import com.flashsale.common.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "知识管理", description = "知识同步任务与知识库状态查询接口")
@RequestMapping("/ai/knowledge")
public class KnowledgeController {

    private final KnowledgeSyncService knowledgeSyncService;

    @Operation(
            summary = "触发知识同步",
            description = "手动触发一次知识库同步，可用于全量或增量刷新。",
            requestBody = @RequestBody(
                    required = false,
                    content = @Content(
                            schema = @Schema(implementation = KnowledgeSyncRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "全量同步",
                                    value = """
                                            {
                                              "syncType": "FULL",
                                              "forceRebuild": true
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "同步任务已创建")
    @PostMapping("/sync")
    public Result<KnowledgeSyncResultVO> sync(
            @org.springframework.web.bind.annotation.RequestBody(required = false) KnowledgeSyncRequestDTO request) {
        KnowledgeSyncRequestDTO actualRequest = request == null ? new KnowledgeSyncRequestDTO() : request;
        return Result.success(knowledgeSyncService.sync(actualRequest));
    }

    @Operation(summary = "查询同步任务状态", description = "按任务 ID 查询知识同步任务的执行结果。")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/sync/{taskId}")
    public Result<KnowledgeSyncResultVO> getSyncTask(
            @Parameter(description = "同步任务 ID", example = "task-001")
            @PathVariable String taskId) {
        return Result.success(knowledgeSyncService.getTask(taskId));
    }

    @Operation(summary = "查询知识库统计", description = "返回当前知识库文档数量、分块数量、命中率和最后同步状态。")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/stats")
    public Result<KnowledgeStatsVO> getStats() {
        return Result.success(knowledgeSyncService.getStats());
    }
}
