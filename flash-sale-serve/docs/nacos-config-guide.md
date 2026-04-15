# Nacos 配置中心接入指南

## 目标

当前项目建议保留两层本地配置：

- `application.yml`：通用配置
- `application-local.yml`：本机启动配置

真正的公共配置和环境配置，统一收敛到 Nacos 配置中心。

## 先明确一件事

Nacos 有两个能力：

- 注册中心：服务注册与发现
- 配置中心：统一管理配置

你现在项目里已经接入了注册中心。
如果要做“统一管理公共配置”，要额外接入配置中心。

## 推荐的配置分层

### 本地文件保留

本地文件只保留“应用启动前必须知道”的内容：

- `spring.application.name`
- `spring.profiles.active`
- `spring.cloud.nacos.server-addr`
- `spring.cloud.nacos.discovery.namespace`
- `spring.cloud.nacos.config.namespace`
- `spring.config.import`

原因很简单：应用必须先知道去哪里找 Nacos，才能从 Nacos 里拉别的配置。

### 建议放到 Nacos 的配置

建议把配置拆成“共享配置 + 服务私有配置”。

#### 共享配置

- `flash-sale-common.yaml`
  - `server.forward-headers-strategy`
  - `mybatis.configuration.map-underscore-to-camel-case`
  - 通用日志级别
  - 通用 OpenFeign 超时

- `flash-sale-mysql.yaml`
  - `spring.datasource.*`

- `flash-sale-redis.yaml`
  - `spring.data.redis.*`

- `flash-sale-rabbitmq.yaml`
  - `spring.rabbitmq.*`

- `flash-sale-jwt.yaml`
  - `flash-sale.jwt.*`

#### 服务私有配置

- `gateway.yaml`
- `auth-service.yaml`
- `product-service.yaml`
- `order-service.yaml`
- `seckill-service.yaml`
- `ai-service.yaml`

这里放各服务独有的内容，比如：

- gateway 路由、鉴权白名单、限流
- order 定时任务参数
- seckill 预热和 MQ 路由参数
- ai-service 模型地址、模型名称、检索参数、规则文档和 SpringDoc 文档开关

## 推荐接入步骤

### 1. 给每个服务加 Nacos Config 依赖

在每个服务的 `pom.xml` 增加：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

### 2. 在 `application.yml` 中加配置导入

推荐写法：

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

说明：

- `optional:` 表示 Nacos 中暂时没有这份配置也不会直接启动失败
- `${spring.application.name}.yaml` 能让每个服务自动加载自己的专属配置

### 3. 在 `application-local.yml` 中保留 Nacos 入口信息

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

这里不要再放数据库、Redis、RabbitMQ 这些真正业务配置了。

### 4. 在 Nacos 控制台创建 Data ID

建议创建这些 Data ID：

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

推荐统一：

- Namespace：你的项目命名空间
- Group：`FLASH_SALE`
- 格式：`YAML`

## 迁移顺序建议

不要一次全搬，按这个顺序最稳：

1. 先把 MySQL、Redis、RabbitMQ 搬到 Nacos
2. 再把 JWT 和通用超时参数搬到 Nacos
3. 最后再搬 gateway 路由、限流、秒杀参数、AI 模型参数这类业务配置

这样即使中途有问题，也比较容易定位。

## Swagger / OpenAPI 聚合说明

当前网关聚合 Swagger 文档，推荐在 `gateway.yaml` 中维护：

- `/v3/api-docs/auth-service` -> `auth-service`
- `/v3/api-docs/product-service` -> `product-service`
- `/v3/api-docs/seckill-service` -> `seckill-service`
- `/v3/api-docs/order-service` -> `order-service`
- `/v3/api-docs/ai-service` -> `ai-service`

网关聚合入口：

- `http://localhost:8080/swagger-ui.html`

AI 服务直连入口：

- `http://localhost:8085/swagger-ui.html`
- `http://localhost:8085/v3/api-docs`

网关鉴权白名单需要包含：

- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs`
- `/v3/api-docs/**`

`ai-service.yaml` 中需要开启：

```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
```

## 当前项目最适合的落地方式

我建议你现在先维持：

- `application.yml`
- `application-local.yml`

等你确认 Nacos 配置中心也要正式启用时，再做下一步代码改造：

- 给 6 个服务加 `nacos-config` 依赖
- 把共享配置 Data ID 建起来
- 再把各服务的本地业务配置逐步迁过去

## 我建议的下一步

如果你要我继续帮你落地，我建议按这个顺序：

1. 我先帮你把 6 个服务补上 `spring-cloud-starter-alibaba-nacos-config`
2. 再把 `application.yml` 改成支持 `spring.config.import`
3. 最后我帮你生成一套可直接导入 Nacos 的 YAML 模板
