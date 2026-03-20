# Flash Sale System

## 项目简介

这是一个以“普通商品 + 秒杀商品”双链路为核心的电商练手项目。当前仓库已经完成了前后端联调闭环，并且后端已经拆分为清晰的微服务边界：

- 认证链路：登录、注册、获取当前用户、修改密码
- 普通商品链路：商品列表、商品详情、普通下单、普通订单查询、模拟支付、支付状态
- 秒杀链路：秒杀商品列表、秒杀商品详情、发起秒杀、轮询结果、异步建单、订单查询、模拟支付、支付状态
- 文档链路：Swagger/OpenAPI 已接入网关聚合页

当前所有前端业务请求统一走网关：`http://localhost:8080`

## 当前目录

```text
flash-sale-system
├── flash-sale-serve
│   ├── auth-service
│   ├── common
│   ├── gateway
│   ├── order-service
│   ├── product-service
│   └── seckill-service
├── flash-sale-ui
├── Flash-Sale-System架构与接口规范.md
├── 前后端交互规范.md
├── 前端完整化待补充后端接口清单.md
├── 已实现技术栈解析与使用说明.md
├── planning.md
└── REVIEW_AUDIT.md
```

## 当前后端架构

```text
Browser
  -> Gateway
      -> auth-service
      -> product-service
      -> seckill-service
      -> order-service

product-service
  -> OpenFeign
      -> order-service (内部建单接口)

seckill-service
  -> RabbitMQ
      -> order-service (异步创建秒杀订单)
```

### 服务职责

- `gateway`
  统一入口、JWT 鉴权、文档聚合、路由转发
- `auth-service`
  登录、注册、获取当前用户、修改密码
- `product-service`
  普通商品列表/详情、普通商品库存校验、普通下单入口
- `seckill-service`
  秒杀商品列表/详情、秒杀请求入口、秒杀结果查询、Redis/Lua 秒杀控制
- `order-service`
  普通订单查询/支付、秒杀订单查询/支付、内部普通建单、秒杀异步建单消费

## 网关路由

- `/auth/**` -> `auth-service`
- `/product/**` -> `product-service`
- `/seckill/**` -> `seckill-service`
- `/seckill-product/**` -> `seckill-service`
- `/order/**` -> `order-service`

## 鉴权约定

- 白名单仅包含 `POST /auth/login`、`POST /auth/register`
- 其他接口统一要求：

```http
Authorization: Bearer <token>
```

- 网关鉴权成功后，会向下游透传：

```http
X-User-Id: <userId>
```

- Swagger 文档中展示的对外鉴权方式也已统一为 `Authorization: Bearer <token>`

## Swagger / OpenAPI

统一入口：

- `http://localhost:8080/swagger-ui.html`

聚合文档分组：

- 认证服务
- 普通商品服务
- 秒杀服务
- 订单服务

原始文档地址：

- `/v3/api-docs/auth-service`
- `/v3/api-docs/product-service`
- `/v3/api-docs/seckill-service`
- `/v3/api-docs/order-service`

## 当前已落地接口

### 认证接口

- `POST /auth/login`
- `POST /auth/register`
- `POST /auth/logout`
- `GET /auth/me`
- `POST /auth/updatePassword`

### 普通商品接口

- `GET /product/products`
- `GET /product/products/{id}`
- `POST /product/normal-orders`

### 秒杀接口

- `GET /seckill-product/products`
- `GET /seckill-product/products/{id}`
- `POST /seckill/{productId}`
- `GET /seckill/result/{productId}`

### 普通订单接口

- `GET /order/normal-orders`
- `GET /order/normal-orders/{id}`
- `POST /order/normal-orders/{id}/pay`
- `GET /order/normal-orders/{id}/pay-status`

### 秒杀订单接口

- `GET /order/seckill-orders`
- `GET /order/seckill-orders/{id}`
- `POST /order/seckill-orders/{id}/pay`
- `GET /order/seckill-orders/{id}/pay-status`

## 关键实现说明

### 普通商品链路

- 普通商品能力已经从 `order-service` 中解耦到 `product-service`
- 前端普通下单入口已经切换为 `POST /product/normal-orders`
- `product-service` 会先校验商品与库存，再通过 OpenFeign 调用 `order-service`
- `order-service` 不再直接依赖普通商品表，只负责普通订单落库、查询与支付

### 秒杀链路

- 秒杀请求先经过 Redis + Lua 做库存与防重控制
- 成功后通过 RabbitMQ 异步投递建单消息
- `order-service` 消费消息创建秒杀订单
- 秒杀结果优先查 Redis，过期后再回查数据库兜底

### 前端现状

- 登录页：登录、注册
- 首页：普通商品列表与秒杀预览
- 秒杀页：秒杀商品浏览、发起秒杀、轮询结果
- 购物车页：普通商品本地下单草稿、秒杀提醒草稿
- 个人中心：普通订单与秒杀订单聚合展示

## 启动建议

### 后端

建议按依赖顺序启动：

1. MySQL
2. Redis
3. RabbitMQ
4. Nacos
5. `auth-service`
6. `product-service`
7. `seckill-service`
8. `order-service`
9. `gateway`

### 前端

在 `flash-sale-ui` 目录执行：

```bash
npm install
npm run dev
```

默认网关地址：

- `http://localhost:8080`

### 文档验证

后端服务与网关启动后，可直接访问：

- `http://localhost:8080/swagger-ui.html`

## 相关阅读

- `前后端交互规范.md`：前后端联调用接口契约
- `Flash-Sale-System架构与接口规范.md`：模块边界、路由与调用链说明
- `已实现技术栈解析与使用说明.md`：当前技术栈真实落地情况
- `planning.md`：下一阶段规划
- `REVIEW_AUDIT.md`：当前审计结论
