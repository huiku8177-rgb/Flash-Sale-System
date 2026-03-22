# Flash Sale System

## 项目简介
这是一个以“普通商品购买 + 秒杀商品抢购”双链路为核心的全栈练手项目。当前仓库已经完成前后端主链路联调，后端按微服务边界拆分，前端具备登录、商品浏览、购物车、独立结算、订单查询与模拟支付能力。

当前后端统一通过网关对外提供服务：

- 网关地址：`http://localhost:8080`
- Swagger 聚合入口：`http://localhost:8080/swagger-ui.html`

当前前端开发地址：

- 前端地址：`http://localhost:5173`

## 当前能力概览

### 认证链路
- 用户登录
- 用户注册
- 获取当前用户信息
- 修改密码
- 退出登录

### 普通商品链路
- 普通商品列表
- 普通商品详情
- 购物车增删改查
- 商品卡片“立即购买”
- 独立结算页
- 基于已选购物车商品创建普通订单
- 普通订单列表、详情、模拟支付、支付状态查询

### 秒杀链路
- 秒杀商品列表
- 秒杀商品详情
- 发起秒杀
- 轮询秒杀结果
- 秒杀订单列表、详情、模拟支付、支付状态查询

### 文档链路
- 微服务 OpenAPI 文档
- 网关 Swagger 聚合页
- 中文接口描述与设计规范文档

## 项目结构

```text
flash-sale-system
├─ flash-sale-serve
│  ├─ auth-service
│  ├─ common
│  ├─ gateway
│  ├─ order-service
│  ├─ product-service
│  └─ seckill-service
├─ flash-sale-ui
├─ README.md
├─ Flash-Sale-System架构与接口规范.md
├─ 前后端交互规范.md
├─ 前端完整化待补充后端接口清单.md
├─ 已实现技术栈解析与使用说明.md
├─ planning.md
└─ REVIEW_AUDIT.md
```

## 后端模块边界

### `gateway`
- 统一入口
- JWT 鉴权
- 方法级白名单放行
- 向下游注入 `X-User-Id`
- Swagger 聚合与文档转发

### `auth-service`
- 登录、注册
- 当前用户信息
- 修改密码

### `product-service`
- 普通商品列表与详情
- 购物车
- 普通商品库存校验
- 基于购物车已选商品创建普通订单
- 通过 OpenFeign 调用 `order-service` 内部建单接口

### `seckill-service`
- 秒杀商品列表与详情
- 秒杀提交流程
- Redis + Lua 秒杀控制
- RabbitMQ 异步建单消息投递

### `order-service`
- 普通订单查询、模拟支付、支付状态查询
- 秒杀订单查询、模拟支付、支付状态查询
- 普通订单内部建单接口
- 秒杀异步订单消费与落库

## 关键调用链

### 普通商品下单
`前端 -> gateway -> product-service -> OpenFeign -> order-service`

说明：
- 前端不再直接提交商品 `items` 创建普通订单
- `product-service` 会读取当前用户已选中的购物车商品
- 校验商品状态、库存、金额后调用 `order-service` 内部接口建单

### 秒杀下单
`前端 -> gateway -> seckill-service -> Redis/Lua -> RabbitMQ -> order-service`

说明：
- 秒杀入口同步返回抢购受理结果
- 订单创建由消息驱动异步完成
- 前端通过轮询结果接口确认是否抢购成功

## 网关路由

- `/auth/**` -> `auth-service`
- `/product/**` -> `product-service`
- `/seckill/**` -> `seckill-service`
- `/seckill-product/**` -> `seckill-service`
- `/order/**` -> `order-service`

## 当前鉴权规则

### 公开放行接口
- `POST /auth/login`
- `POST /auth/register`
- `GET /product/products`
- `GET /product/products/**`
- `GET /seckill-product/products`
- `GET /seckill-product/products/**`
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs`
- `/v3/api-docs/**`
- `/error`
- `/favicon.ico`

### 受保护接口
除白名单外，其余接口统一要求：

```http
Authorization: Bearer <token>
```

网关鉴权成功后会向下游透传：

```http
X-User-Id: <userId>
```

## 前端路由约定

### 匿名可访问页面
- `/#/app/home`
- `/#/app/flash`

### 必须登录页面
- `/#/app/cart`
- `/#/app/profile`
- `/#/checkout`

### 额外约定
- 商品详情弹层点击前会先校验登录态
- 未登录点击“查看详情”会直接跳转登录页
- 商品卡片支持“加入购物车”和“立即购买”
- 结算页为独立页面，不复用商城主页面

## 当前核心接口

### 认证
- `POST /auth/login`
- `POST /auth/register`
- `POST /auth/logout`
- `GET /auth/me`
- `POST /auth/updatePassword`

### 普通商品
- `GET /product/products`
- `GET /product/products/{id}`

### 购物车
- `GET /product/cart/items`
- `POST /product/cart/items`
- `PUT /product/cart/items/{id}`
- `DELETE /product/cart/items/{id}`
- `DELETE /product/cart/items`

### 普通订单
- `POST /product/normal-orders`
- `GET /order/normal-orders`
- `GET /order/normal-orders/{id}`
- `POST /order/normal-orders/{id}/pay`
- `GET /order/normal-orders/{id}/pay-status`

### 秒杀
- `GET /seckill-product/products`
- `GET /seckill-product/products/{id}`
- `POST /seckill/{productId}`
- `GET /seckill/result/{productId}`

### 秒杀订单
- `GET /order/seckill-orders`
- `GET /order/seckill-orders/{id}`
- `POST /order/seckill-orders/{id}/pay`
- `GET /order/seckill-orders/{id}/pay-status`

## 启动建议

### 基础设施
1. MySQL
2. Redis
3. RabbitMQ
4. Nacos

### 后端服务
1. `auth-service`
2. `product-service`
3. `seckill-service`
4. `order-service`
5. `gateway`

### 前端
在 `flash-sale-ui` 目录执行：

```bash
npm install
npm run dev
```

## 说明
- 当前环境中没有 `mvn` 或 `mvnw`，仓库内无法直接完成 Maven 编译验证
- 前端近期构建已通过，仍有 Vite 的包体积告警，但不影响功能

## 相关文档
- `Flash-Sale-System架构与接口规范.md`：模块边界、接口分层、设计约束
- `前后端交互规范.md`：联调口径、请求响应规范、页面权限与接口契约
- `前端完整化待补充后端接口清单.md`：当前仍值得继续补充的后端能力
