# Flash-Sale-System 架构与接口规范

## 文档定位

本文档回答 4 个问题：

1. 当前系统由哪些模块组成
2. 每个模块的职责边界是什么
3. 外部接口和内部接口如何分层
4. 鉴权、透传、统一响应的约定是什么

更细的前后端联调口径请看 [前后端交互规范.md](C:/Users/NeoZeng/Desktop/flash-sale-system/前后端交互规范.md)。

## 总体架构

```text
Vue 3 前端
   ↓
Gateway
   ├─ auth-service
   ├─ product-service
   ├─ seckill-service
   ├─ order-service
   └─ ai-service

公共基础设施
   ├─ MySQL
   ├─ Redis
   ├─ RabbitMQ
   └─ Nacos
```

整体设计原则：

- 所有对外流量优先走 `gateway`
- 认证、地址归 `auth-service`
- 商品、购物车、普通下单入口归 `product-service`
- 订单查询、支付、取消归 `order-service`
- 秒杀流量入口和预扣库存归 `seckill-service`
- AI 问答与知识库归 `ai-service`
- 公共模型、工具类、响应结构归 `common`

## 模块边界

### `gateway`

职责：

- 统一入口
- JWT 鉴权
- 白名单放行
- 用户身份透传
- 网关限流
- Swagger 聚合

不负责：

- 业务落库
- 订单状态流转
- 商品业务逻辑

### `auth-service`

职责：

- 登录、注册、退出登录
- 用户信息查询
- 修改密码
- 地址管理
- Token 黑名单与 token version 维护

### `product-service`

职责：

- 普通商品列表与详情
- 购物车管理
- 普通订单创建入口
- 普通商品缓存

说明：

- 它负责“从商品和购物车视角发起普通下单”
- 订单查询、支付、取消不在这里做

### `order-service`

职责：

- 普通订单查询、支付、取消
- 秒杀订单查询、支付、取消
- 秒杀异步建单落库
- 超时取消和补偿

说明：

- 它是订单生命周期中心
- 既管理普通订单，也管理秒杀订单

### `seckill-service`

职责：

- 秒杀商品列表与详情
- 秒杀入口
- Redis 库存预热
- Redis + Lua 原子预扣
- 秒杀结果查询

说明：

- 它只负责秒杀请求入口和高并发预处理
- 真实订单创建在 `order-service` 中异步完成

### `ai-service`

职责：

- 商品问答
- 会话管理
- 候选商品解析
- 知识库同步与统计
- AI 缓存与上下文管理

### `common`

职责：

- 统一响应结构
- 公共异常
- JWT 工具
- 公共 Redis Key 约定
- 通用请求头常量

## 外部接口与内部接口分层

### 外部接口

对前端和第三方联调开放的接口，应满足：

- 默认经由 `gateway` 暴露
- 使用统一响应结构
- 受网关鉴权和限流控制

当前主要外部路径前缀：

- `/auth`
- `/product`
- `/order`
- `/seckill-product`
- `/seckill`
- `/ai`

### 内部接口

服务间调用接口使用 `/internal/**`，只服务于微服务内部协作。

当前已存在的典型内部接口：

- `/internal/orders/normal`
- `/internal/orders/normal/by-order-no`
- `/internal/products/normal-orders/restore-stock`

内部接口规则：

- 不作为前端联调入口
- 不承诺长期兼容前端调用
- 可以按服务间协作需要演进

## 鉴权规范

### 外部请求

标准做法：

- 请求头使用 `Authorization: Bearer <token>`
- 前端不应信任或手工构造最终的 `X-User-Id`

### 网关透传

网关行为：

- 移除外部伪造的 `X-User-Id`
- 校验 token 后向下游写入真实用户 ID

### 白名单接口

典型匿名接口包括：

- 登录、注册
- 普通商品列表与详情
- 秒杀商品列表与详情
- Swagger/OpenAPI 文档入口

具体白名单仍以 `gateway` 配置为准。

## 统一响应结构

所有外部接口统一使用：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

约定：

- `code = 200` 表示业务成功
- 业务失败时由 `code/message` 表达
- 前端统一从 `data` 取业务载荷

## 核心业务链路

### 普通商品下单链路

1. 前端查询商品与购物车
2. 前端调用 `POST /product/normal-orders`
3. `product-service` 组装下单请求并调用 `order-service` 内部接口
4. `order-service` 落库普通订单
5. 前端再通过 `/order/normal-orders/**` 查询、支付、取消

### 秒杀链路

1. 前端调用 `POST /seckill/{productId}`
2. `seckill-service` 用 Redis + Lua 做库存与重复校验
3. 秒杀消息投递到 RabbitMQ
4. `order-service` 消费消息并创建秒杀订单
5. 前端通过 `/seckill/result/{productId}` 轮询结果
6. 后续通过 `/order/seckill-orders/**` 处理支付与取消

### AI 问答链路

1. 前端调用 `POST /ai/chat`
2. `ai-service` 获取或创建会话
3. 结合上下文、历史记录、知识库检索和路由策略生成回答
4. 会话与历史写入 DB，并将热点上下文写入 Redis

## 配置分层约定

当前后端采用三层配置：

- `application.yml`：服务名、Nacos 接入、基础入口
- `application-local.yml`：本地兜底配置
- Nacos：共享配置与服务私有配置

推荐共享 Data ID：

- `flash-sale-common.yaml`
- `flash-sale-jwt.yaml`
- `flash-sale-mysql.yaml`
- `flash-sale-redis.yaml`
- `flash-sale-rabbitmq.yaml`

推荐服务私有 Data ID：

- `auth-service.yaml`
- `gateway.yaml`
- `product-service.yaml`
- `order-service.yaml`
- `seckill-service.yaml`
- `ai-service.yaml`

## 文档维护规则

- 入口、端口、启动方式变化时，优先更新 [README.md](C:/Users/NeoZeng/Desktop/flash-sale-system/README.md)
- 模块边界、链路、接口层次变化时，更新本文档
- 页面访问规则和前端 API 调用口径变化时，更新 [前后端交互规范.md](C:/Users/NeoZeng/Desktop/flash-sale-system/前后端交互规范.md)
