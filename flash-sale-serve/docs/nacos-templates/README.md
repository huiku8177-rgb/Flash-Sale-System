# Nacos 模板说明

这个目录存放的是可直接复制到 Nacos 控制台的 YAML 模板。它们对应的是 `flash-sale-serve` 各服务建议使用的 Data ID，不负责保存真实密钥或最终环境值。

## 使用原则

- 模板文件用于提供字段结构和默认示例
- 导入前请按你的环境改 host、port、密码、命名空间等值
- 真实密钥不要提交到仓库，也不要直接写入模板
- 共享配置和服务私有配置要分开维护

## 建议的 Data ID 划分

### 共享配置

- `flash-sale-common.yaml`
- `flash-sale-mysql.yaml`
- `flash-sale-redis.yaml`
- `flash-sale-rabbitmq.yaml`
- `flash-sale-jwt.yaml`

### 服务私有配置

- `gateway.yaml`
- `auth-service.yaml`
- `product-service.yaml`
- `order-service.yaml`
- `seckill-service.yaml`
- `ai-service.yaml`

## 推荐导入顺序

为了降低排障成本，建议按下面顺序导入：

1. `flash-sale-common.yaml`
2. `flash-sale-mysql.yaml`
3. `flash-sale-redis.yaml`
4. `flash-sale-rabbitmq.yaml`
5. `flash-sale-jwt.yaml`
6. 各服务自己的 `*.yaml`

推荐统一约定：

- Namespace：项目独立 namespace
- Group：`FLASH_SALE`
- 格式：`YAML`

## 模板内容说明

### `flash-sale-common.yaml`

适合放：

- 通用日志级别
- MyBatis 通用配置
- OpenFeign 通用超时
- 其他跨服务可复用参数

### `flash-sale-mysql.yaml`

适合放：

- `spring.datasource.*`

### `flash-sale-redis.yaml`

适合放：

- `spring.data.redis.host`
- `spring.data.redis.port`
- `spring.data.redis.password`
- `spring.data.redis.database`
- 连接超时和 Lettuce 连接池参数
- Sentinel 场景下的 `master` / `nodes`

### `flash-sale-rabbitmq.yaml`

适合放：

- `spring.rabbitmq.*`

### `flash-sale-jwt.yaml`

适合放：

- `flash-sale.jwt.*`

### `gateway.yaml`

当前模板已经覆盖：

- `/auth/**`
- `/order/**`
- `/seckill/**`
- `/seckill-product/**`
- `/product/**`
- `/ai/**`
- Swagger / OpenAPI 聚合路由
- 鉴权白名单
- 基础限流规则

如果你后面继续维护聚合文档入口，优先在这份文件里改。

### `ai-service.yaml`

当前模板主要放：

- `springdoc.api-docs.enabled`
- `springdoc.swagger-ui.path`
- `ai.base-url`
- `ai.embedding-model`
- `ai.chat-model`

不要把真实 `FLASH_SALE_AI_API_KEY` 写进这个文件。运行时继续用环境变量注入。

## Swagger / OpenAPI 说明

通过当前模板导入后，Swagger 访问口径应保持一致：

- 网关聚合入口：`http://localhost:8080/swagger-ui.html`
- AI 服务直连入口：`http://localhost:8085/swagger-ui.html`
- AI 服务直连 OpenAPI：`http://localhost:8085/v3/api-docs`

网关白名单必须继续允许这些路径匿名访问：

- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs`
- `/v3/api-docs/**`

## 相关文档

- [docs/README.md](../README.md)
- [Nacos 配置中心接入指南](../nacos-config-guide.md)
- [AI Service 本地接入说明](../ai-service-setup.md)
