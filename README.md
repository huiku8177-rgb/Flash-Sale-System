# Flash Sale System

## 项目简介

这是一个以“普通商品 + 秒杀商品”双链路为核心的电商练手项目，当前仓库已经具备完整的前后端联调能力：

- 认证：登录、注册、获取当前用户、修改密码
- 普通商品：列表、详情、加入购物车、本地下单、订单详情、模拟支付、支付状态
- 秒杀商品：列表、详情、发起秒杀、轮询结果、异步建单、订单详情、模拟支付、支付状态
- 前端：`flash-sale-ui` 已实现登录页、首页、秒杀页、购物车页、个人中心页

当前所有业务接口统一走网关 `http://localhost:8080`。

## 当前目录

```text
flash-sale-system
├── flash-sale-serve
│   ├── auth-service
│   ├── common
│   ├── gateway
│   ├── order-service
│   └── seckill-service
├── flash-sale-ui
├── Flash-Sale-System架构与接口规范.md
├── 前后端交互规范.md
├── 前端完整化待补充后端接口清单.md
├── 已实现技术栈解析与使用说明.md
├── planning.md
└── REVIEW_AUDIT.md
```

## 技术栈

### 后端

- Java 17
- Spring Boot
- Spring Cloud Gateway
- Spring Cloud Alibaba Nacos
- MyBatis
- MySQL
- Redis
- RabbitMQ
- JWT + RSA

### 前端

- Vue 3
- Vue Router
- Element Plus
- Axios
- Vite

## 当前架构

```text
Browser
  -> Gateway
      -> auth-service
      -> seckill-service
      -> order-service
  -> MySQL / Redis / RabbitMQ
```

### 网关当前已配置路由

- `/auth/**` -> `auth-service`
- `/order/**` -> `order-service`
- `/seckill/**` -> `seckill-service`
- `/seckill-product/**` -> `seckill-service`
- `/product/**` -> `seckill-service`

## 认证约定

- 仅 `/auth/login`、`/auth/register` 在白名单内
- 其他接口默认都需要 `Authorization: Bearer <token>`
- Gateway 鉴权通过后会向下游透传 `X-User-Id`
- 这意味着商品列表和商品详情当前也需要先登录

## 当前已落地接口

### Auth

- `POST /auth/login`
- `POST /auth/register`
- `POST /auth/logout`
- `GET /auth/me`
- `POST /auth/updatePassword`

### 普通商品

- `GET /product/products`
- `GET /product/products/{id}`

### 秒杀商品

- `GET /seckill-product/products`
- `GET /seckill-product/products/{id}`
- `POST /seckill/{productId}`
- `GET /seckill/result/{productId}`

### 秒杀订单

- `GET /order/orders`
- `GET /order/orderDetail/{id}`
- `POST /order/seckill-orders/{id}/pay`
- `GET /order/seckill-pay-status/{id}`

### 普通订单

- `POST /order/checkout`
- `GET /order/normal-orders`
- `GET /order/normal-orders/{id}`
- `POST /order/normal-orders/{id}/pay`
- `GET /order/pay-status/{id}`

## 业务现状

### 普通商品链路

- 已支持列表、详情、购物车本地草稿、创建普通订单
- 普通订单会落库到 `normal_order` 与 `normal_order_item`
- 支持订单详情、模拟支付、支付状态查询
- 当前购物车仍是前端本地状态，不是后端购物车服务

### 秒杀链路

- 秒杀请求经过 Redis/Lua 进行库存与重复购买控制
- 秒杀成功后通过 MQ 异步创建秒杀订单
- 前端轮询 `/seckill/result/{productId}` 获取结果
- Redis 结果过期后，后端会回查数据库兜底，避免真实成功被误判为失败
- 秒杀订单已支持模拟支付和支付状态查询

## 前端页面现状

- 登录页：登录、注册
- 首页：普通商品分类筛选、精选商品、秒杀预览
- 秒杀页：活动商品筛选、发起秒杀、轮询结果
- 购物车页：普通商品结算、秒杀草稿提醒
- 个人中心：普通订单与秒杀订单统一展示、详情、支付、查状态、修改密码

## 数据库补充说明

普通订单使用新增的两张表：

- `normal_order`
- `normal_order_item`

表结构脚本位于：

- `flash-sale-serve/docs/sql/add_normal_order_tables.sql`

## 启动建议

### 后端

按依赖顺序准备：

1. MySQL
2. Redis
3. RabbitMQ
4. Nacos
5. `auth-service`
6. `seckill-service`
7. `order-service`
8. `gateway`

### 前端

在 `flash-sale-ui` 下执行：

```bash
npm install
npm run dev
```

默认网关地址为 `http://localhost:8080`。

## 相关阅读

- `前后端交互规范.md`：给前端和联调用的接口契约
- `Flash-Sale-System架构与接口规范.md`：服务边界、认证和接口总览
- `前端完整化待补充后端接口清单.md`：下一阶段仍可继续补的能力
- `已实现技术栈解析与使用说明.md`：当前技术栈的真实落地情况
- `planning.md`：当前阶段计划与下一步演进路线
- `REVIEW_AUDIT.md`：本轮代码与文档审计结论
