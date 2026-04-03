# 实现最小可运行RagChatService Spec

## Why
当前RagChatServiceImpl存在TODO占位符，需要实现最小可运行版本以验证AI服务基础流程，为后续完整RAG功能奠定基础。

## What Changes
- 实现RagChatService的最小可运行版本
- 注入EmbeddingClient和ChatModelClient依赖
- 实现基础问答流程：question -> embedding -> prompt -> answer
- 预留知识检索扩展点，但不实现复杂检索逻辑
- 返回ChatResponseVO，sources字段返回占位说明

## Impact
- Affected specs: AI服务基础问答能力
- Affected code: RagChatServiceImpl.java

## ADDED Requirements
### Requirement: 最小可运行RAG问答
系统SHALL提供基础的AI问答功能，支持用户提问并返回AI回答。

#### Scenario: 成功问答
- **WHEN** 用户提交问题
- **THEN** 系统调用EmbeddingClient生成向量
- **AND** 系统构造基础prompt
- **AND** 系统调用ChatModelClient获取回答
- **AND** 系统返回ChatResponseVO包含answer和sources占位信息

#### Scenario: 扩展点预留
- **WHEN** 后续需要接入知识检索
- **THEN** retrieveKnowledge方法提供清晰的扩展点
- **AND** 不影响现有基础问答流程

## MODIFIED Requirements
无

## REMOVED Requirements
无
