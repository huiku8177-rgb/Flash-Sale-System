# Tasks
- [x] Task 1: 实现RagChatService最小可运行版本
  - [x] SubTask 1.1: 注入EmbeddingClient和ChatModelClient依赖
  - [x] SubTask 1.2: 实现chat方法接收用户question
  - [x] SubTask 1.3: 调用EmbeddingClient对question做embedding
  - [x] SubTask 1.4: 构造最小prompt（不包含知识库内容）
  - [x] SubTask 1.5: 调用ChatModelClient获取answer
  - [x] SubTask 1.6: 返回ChatResponseVO，sources返回占位说明
  - [x] SubTask 1.7: 预留retrieveKnowledge扩展点，返回空列表

# Task Dependencies
无
