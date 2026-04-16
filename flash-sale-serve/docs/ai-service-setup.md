# AI Service 本地接入说明

`ai-service` 是当前项目里负责商品问答、会话管理、候选商品解析和知识库同步的服务。这个文档只说明本地启动、接口验证和配置落点，不重复介绍业务流程。

## 适用场景

- 你要单独启动 `ai-service` 做本地调试
- 你要验证 Swagger / OpenAPI 是否可用
- 你要给本机或 IDE 注入 DashScope API Key
- 你要确认哪些配置应该放在 Nacos，哪些不应该进仓库

## 启动前确认

本地调试前，至少确认下面几项已经可用：

- `auth-service`、`product-service`、`seckill-service`、`gateway` 的基础联调链路可跑通
- MySQL、Redis、Nacos 已经启动
- 你手里有可用的 `FLASH_SALE_AI_API_KEY`
- 本地 Java / Maven 环境正常，项目当前没有内置 `mvnw`

如果只是验证服务自身接口，`product-service` 和 `seckill-service` 不一定必须同时启动；但涉及商品问答、候选商品解析或知识库同步时，仍然建议把依赖服务一起带起来。

## 接口文档地址

### 直连 `ai-service`

- Swagger UI: `http://localhost:8085/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8085/v3/api-docs`

### 通过网关聚合访问

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- AI Service OpenAPI JSON: `http://localhost:8080/v3/api-docs/ai-service`

## 网关侧要求

要让文档页正常打开，网关至少需要满足两类条件：

1. 路由存在

- `/ai/**` -> `lb://ai-service`
- `/v3/api-docs/ai-service` -> `lb://ai-service`，并重写到 `/v3/api-docs`

2. 鉴权白名单放行

- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs`
- `/v3/api-docs/**`
- `GET:/ai/health`

这些配置已经体现在 [nacos-templates/gateway.yaml](./nacos-templates/gateway.yaml) 里。如果你从 Nacos 加载网关配置，优先以这个模板为准。

## 本地注入 API Key 的推荐方式

不要把真实 `FLASH_SALE_AI_API_KEY` 写进：

- `application.yml`
- `application-local.yml`
- Nacos 模板
- Git 跟踪的任何明文配置文件

推荐使用下面两种项目级方式。

### 方式一：当前 PowerShell 会话生效

执行示例脚本：

- [ai-service-local-env.example.ps1](./ai-service-local-env.example.ps1)

脚本会设置这些变量：

- `FLASH_SALE_AI_ENABLED=true`
- `FLASH_SALE_AI_PORT=8085`
- `FLASH_SALE_AI_API_KEY=replace-with-your-real-key`
- `SPRING_PROFILES_ACTIVE=local`
- `FLASH_SALE_NACOS_CONFIG_ENABLED=false`

这个方案只影响当前 PowerShell 窗口。关闭终端后，变量就失效，不会污染别的项目。

### 方式二：IDE 运行配置

如果你用 IntelliJ IDEA 启动 `ai-service`，更干净的做法是把这些环境变量只写进该服务的 Run Configuration：

- `FLASH_SALE_AI_ENABLED=true`
- `FLASH_SALE_AI_API_KEY=your-real-key`
- `FLASH_SALE_AI_PORT=8085`

这样不会把 Key 扩散到系统级环境变量，也不会影响其他仓库。

## 当前 AI 服务的主要接口分组

- AI 问答：聊天、候选商品解析
- 会话管理：会话列表、详情、删除
- 知识库管理：同步、同步任务状态、知识库统计

如果你只想先验证服务是否活着，优先看健康检查和 Swagger，不要一上来就测知识库同步链路。

## 配置落点建议

### 适合留在运行时环境变量

- `FLASH_SALE_AI_API_KEY`

### 适合写进 `ai-service.yaml`

- `springdoc.api-docs.enabled`
- `springdoc.swagger-ui.path`
- `ai.enabled`
- `ai.base-url`
- `ai.chat-model`
- `ai.embedding-model`
- `ai.product-service-url`
- `ai.seckill-service-url`
- 检索、历史、置信度、清理、规则文档等非敏感配置

对应模板见：

- [nacos-templates/ai-service.yaml](./nacos-templates/ai-service.yaml)

## 本地联调建议

建议按下面顺序做自检：

1. 先直接访问 `http://localhost:8085/swagger-ui.html`
2. 再检查 `http://localhost:8085/v3/api-docs`
3. 然后通过网关访问 `http://localhost:8080/swagger-ui.html`
4. 最后再验证聊天、会话和知识库相关接口

这样能快速把“服务没起”“网关没配”“鉴权白名单没开”“模型 Key 不可用”这几类问题拆开。

## 当前实现补充说明

- 本地 / 开发环境下，当 DashScope embedding 不可用时，项目允许退回本地 hash 向量，目的是保证基本聊天和启动流程不被阻塞
- 这种退化只适合本地调试，不代表生产可接受的语义检索质量
- 最近已经修复了“Redis miss 被误判为空上下文”的问题，当前缓存 miss 会正确回源数据库，不会直接丢失会话上下文

## 相关文档

- [docs/README.md](./README.md)
- [Nacos 配置接入指南](./nacos-config-guide.md)
- [Nacos 模板说明](./nacos-templates/README.md)
