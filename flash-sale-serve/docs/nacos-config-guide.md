# Nacos 配置中心接入指南

这个文档说明当前 `flash-sale-serve` 项目里，Nacos 配置中心应该怎么接、配置怎么拆、迁移顺序怎么排，以及哪些内容不适合放进 Git 或 Nacos。

## 文档目标

当前项目建议保留“两层本地配置 + 一层 Nacos 配置中心”的结构：

- `application.yml`：应用公共默认配置
- `application-local.yml`：本机启动入口和本地覆盖
- Nacos：共享配置、环境差异配置、服务私有配置

核心原则不是“把所有配置都塞进 Nacos”，而是把真正需要统一管理的部分收口进去。

## 先区分两件事

Nacos 在这个项目里有两类能力：

- 注册中心：服务注册与发现
- 配置中心：统一管理配置

当前项目已经接入了注册中心。如果要把 MySQL、Redis、RabbitMQ、JWT、网关路由等配置统一管理，还需要显式接入配置中心能力。

## 推荐的配置分层

### 本地文件保留什么

本地文件只保留“服务启动前必须先知道”的配置：

- `spring.application.name`
- `spring.profiles.active`
- `spring.cloud.nacos.server-addr`
- `spring.cloud.nacos.discovery.namespace`
- `spring.cloud.nacos.config.namespace`
- `spring.config.import`

原因很简单：应用必须先知道去哪个 Nacos、读哪个 namespace / group，之后才能把其他配置拉下来。

### 建议放到 Nacos 的共享配置

建议拆成几份共享 Data ID：

- `flash-sale-common.yaml`
  - 通用日志级别
  - `server.forward-headers-strategy`
  - MyBatis 通用配置
  - OpenFeign 通用超时

- `flash-sale-mysql.yaml`
  - `spring.datasource.*`

- `flash-sale-redis.yaml`
  - `spring.data.redis.*`

- `flash-sale-rabbitmq.yaml`
  - `spring.rabbitmq.*`

- `flash-sale-jwt.yaml`
  - `flash-sale.jwt.*`

### 建议放到 Nacos 的服务私有配置

每个服务保留一份自己的 Data ID：

- `gateway.yaml`
- `auth-service.yaml`
- `product-service.yaml`
- `order-service.yaml`
- `seckill-service.yaml`
- `ai-service.yaml`

这类配置通常包括：

- 网关路由、鉴权白名单、限流规则
- 秒杀预热参数、MQ 路由参数
- 订单补偿、任务调度、超时参数
- AI 模型地址、模型名、检索参数、SpringDoc 开关

## 推荐接入方式

### 1. 给服务补 Nacos Config 依赖

每个需要从 Nacos 拉配置的服务，都需要在 `pom.xml` 里增加：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

### 2. 在 `application.yml` 里声明导入顺序

推荐使用下面这类写法：

```yaml
spring:
  config:
    import:
      - optional:nacos:flash-sale-common.yaml?group=FLASH_SALE
      - optional:nacos:flash-sale-mysql.yaml?group=FLASH_SALE
      - optional:nacos:flash-sale-redis.yaml?group=FLASH_SALE
      - optional:nacos:flash-sale-rabbitmq.yaml?group=FLASH_SALE
      - optional:nacos:flash-sale-jwt.yaml?group=FLASH_SALE
      - optional:nacos:${spring.application.name}.yaml?group=FLASH_SALE
```

这里有两个关键点：

- `optional:` 表示某份配置暂时不存在时，不直接把应用启动打死
- `${spring.application.name}.yaml` 可以让各服务自动加载自己的私有配置

### 3. 在 `application-local.yml` 保留 Nacos 入口信息

示例：

```yaml
spring:
  cloud:
    nacos:
      server-addr: 192.168.100.130:8848
      discovery:
        namespace: cac2e129-5c78-402a-bdd1-b9dbb0892486
        register-enabled: true
      config:
        namespace: cac2e129-5c78-402a-bdd1-b9dbb0892486
        group: FLASH_SALE
        file-extension: yaml
        refresh-enabled: true
```

这里不要再继续存放数据库、Redis、RabbitMQ 之类真正的业务配置，否则配置分层会重新混乱。

### 4. 在 Nacos 控制台创建 Data ID

建议至少创建这些 Data ID：

- `flash-sale-common.yaml`
- `flash-sale-mysql.yaml`
- `flash-sale-redis.yaml`
- `flash-sale-rabbitmq.yaml`
- `flash-sale-jwt.yaml`
- `gateway.yaml`
- `auth-service.yaml`
- `product-service.yaml`
- `order-service.yaml`
- `seckill-service.yaml`
- `ai-service.yaml`

推荐统一约定：

- Namespace：项目独立 namespace
- Group：`FLASH_SALE`
- 格式：`YAML`

## 迁移顺序建议

不要一次性把所有配置一起搬走。当前项目更稳的顺序是：

1. 先迁 MySQL、Redis、RabbitMQ
2. 再迁 JWT 和通用超时配置
3. 最后迁网关路由、限流、秒杀参数、AI 参数等业务配置

这个顺序的好处是，先把基础设施层稳定下来，再动业务配置，排障会容易很多。

## Swagger / OpenAPI 聚合说明

当前项目通过网关聚合 Swagger，建议在 `gateway.yaml` 中维护下面这些路由：

- `/v3/api-docs/auth-service`
- `/v3/api-docs/product-service`
- `/v3/api-docs/seckill-service`
- `/v3/api-docs/order-service`
- `/v3/api-docs/ai-service`

默认入口：

- 网关聚合 Swagger UI：`http://localhost:8080/swagger-ui.html`
- AI 服务直连 Swagger UI：`http://localhost:8085/swagger-ui.html`
- AI 服务直连 OpenAPI：`http://localhost:8085/v3/api-docs`

同时，网关鉴权白名单必须继续放开：

- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs`
- `/v3/api-docs/**`

## 密钥和敏感信息的放置原则

下面这类内容不建议进 Git 跟踪文件：

- 第三方模型 API Key
- 生产密码
- 真实内网地址如果不适合公开

尤其是 `ai-service` 的 DashScope Key，建议继续使用运行时环境变量：

- `FLASH_SALE_AI_API_KEY`

模板文件可以保留字段名和默认结构，但不要保留真实密钥。

## 当前项目最适合的落地方式

如果你现在还在本地开发阶段，最实际的方式是：

- 保留 `application.yml`
- 保留 `application-local.yml`
- 把共享环境配置逐步迁入 Nacos
- 继续使用模板目录维护 Data ID 样例

这样既不会一下子把启动链路搞复杂，也能让配置中心逐步成形。

## 建议配合阅读

- [docs/README.md](./README.md)
- [Nacos 模板说明](./nacos-templates/README.md)
- [AI Service 本地接入说明](./ai-service-setup.md)
