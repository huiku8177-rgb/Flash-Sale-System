# Flash-Sale-System 架构与接口规范

> 版本：2026-03-26  
> 状态：基于当前仓库实现整理

## 1. 总体架构

```text
Browser
  -> Gateway
      -> auth-service
      -> product-service
      -> seckill-service
      -> order-service
      -> ai-service

product-service
  -> OpenFeign
      -> order-service (/internal/orders/normal)

seckill-service
  -> Redis + Lua
  -> RabbitMQ
      -> order-service
```

基础设施：

- MySQL
- Redis
- RabbitMQ
- Nacos

## 2. 模块边界

### 2.1 gateway

职责：

- 统一外部访问入口
- JWT 鉴权
- 白名单路径放行
- 移除外部伪造的内部身份头
- 向下游透传真实 `X-User-Id`
- Swagger/OpenAPI 聚合
- 基础限流

### 2.2 auth-service

职责：

- 登录
- 注册
- 获取当前用户
- 修改密码
- 收货地址管理

当前对外路径：

- `/auth/login`
- `/auth/register`
- `/auth/logout`
- `/auth/me`
- `/auth/updatePassword`
- `/auth/addresses/**`

### 2.3 product-service

职责：

- 普通商品列表与详情
- 商品搜索
- 按分类筛选
- 购物车增删改查
- 普通订单结算入口

当前对外路径：

- `/product/products`
- `/product/products/{id}`
- `/product/cart/**`
- `/product/normal-orders`

说明：

- 当前商品搜索支持商品名关键词
- 当前商品搜索也支持通过分类关键词命中分类商品，例如“酒水饮料”

### 2.4 order-service

职责：

- 普通订单列表、详情、支付、取消、支付状态
- 秒杀订单列表、详情、支付、取消、支付状态
- 内部普通订单创建接口
- 秒杀异步订单消费落库

当前对外路径：

- `/order/normal-orders/**`
- `/order/seckill-orders/**`

当前内部路径：

- `/internal/orders/normal`
- `/internal/orders/normal/by-order-no`

### 2.5 seckill-service

职责：

- 秒杀商品列表与详情
- 秒杀请求入口
- 秒杀结果查询
- Redis + Lua 库存控制
- RabbitMQ 投递异步建单消息

当前对外路径：

- `/seckill/{productId}`
- `/seckill/result/{productId}`
- `/seckill-product/products`
- `/seckill-product/products/{id}`

### 2.6 common

职责：

- 通用返回模型
- 公共异常/常量
- JWT 工具
- Redis Key 与状态常量
- 通用 Web 头部常量

### 2.7 ai-service

职责：

- 商品知识问答
- 多轮会话管理
- 自然语言商品候选解析
- 商品与规则知识同步
- 知识库统计与同步任务查询
- SpringDoc OpenAPI 文档输出，供 Gateway 聚合

当前对外路径：

- `/ai/chat`
- `/ai/chat/sessions`
- `/ai/chat/sessions/{sessionId}`
- `/ai/chat/resolve-product`
- `/ai/knowledge/sync`
- `/ai/knowledge/sync/{taskId}`
- `/ai/knowledge/stats`

## 3. 外部接口与内部接口分层

### 3.1 外部接口

外部接口统一经由 Gateway 访问，供前端、Swagger 和联调工具使用。

网关当前路由：

- `/auth/**` -> `auth-service`
- `/product/**` -> `product-service`
- `/seckill/**` -> `seckill-service`
- `/seckill-product/**` -> `seckill-service`
- `/order/**` -> `order-service`
- `/ai/**` -> `ai-service`

Swagger/OpenAPI 聚合路由：

- `/v3/api-docs/auth-service` -> `auth-service`
- `/v3/api-docs/product-service` -> `product-service`
- `/v3/api-docs/seckill-service` -> `seckill-service`
- `/v3/api-docs/order-service` -> `order-service`
- `/v3/api-docs/ai-service` -> `ai-service`

Swagger 聚合入口：

- `http://localhost:8080/swagger-ui.html`

### 3.2 内部接口

内部接口只用于服务间调用，不直接面向前端。

当前内部接口示例：

- `POST /internal/orders/normal`
- `GET /internal/orders/normal/by-order-no`
- `POST /product/internal/normal-orders/stock/reserve`
- `POST /product/internal/normal-orders/stock/restore`

约束：

- 前端不直接调用内部接口
- 内部接口的变化应优先在模块边界文档中记录
- 内部接口不应出现在对外 Swagger 分组中

## 4. 鉴权规范

### 4.1 外部鉴权方式

除白名单接口外，其余接口统一要求：

```http
Authorization: Bearer <token>
```

### 4.2 下游身份透传

Gateway 鉴权成功后向下游写入：

```http
X-User-Id: <userId>
```

约束：

- 下游服务不重复解析 JWT
- 下游服务统一依赖 `X-User-Id` 获取当前用户
- Gateway 必须先清理外部同名头，再写入真实身份

### 4.3 白名单接口

当前典型白名单包括：

- `POST:/auth/login`
- `POST:/auth/register`
- `GET:/product/products`
- `GET:/product/products/**`
- `GET:/seckill-product/products`
- `GET:/seckill-product/products/**`
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs`
- `/v3/api-docs/**`
- `/error`
- `/favicon.ico`
- `GET:/ai/health`

## 5. 统一响应规范

所有业务接口统一返回：

```json
{
  "code": 200,
  "message": "成功",
  "data": {},
  "timestamp": "2026-03-26T12:00:00"
}
```

字段说明：

- `code`：业务码
- `message`：中文提示
- `data`：业务数据，可为对象、数组或 `null`
- `timestamp`：服务端响应时间

## 6. 当前核心对外接口

### 6.1 认证与账户

- `POST /auth/login`
- `POST /auth/register`
- `POST /auth/logout`
- `GET /auth/me`
- `POST /auth/updatePassword`
- `GET /auth/addresses`
- `GET /auth/addresses/{id}`
- `POST /auth/addresses`
- `PUT /auth/addresses/{id}`
- `DELETE /auth/addresses/{id}`
- `PUT /auth/addresses/{id}/default`

### 6.2 普通商品与购物车

- `GET /product/products`
- `GET /product/products/{id}`
- `GET /product/cart/items`
- `POST /product/cart/items`
- `PUT /product/cart/items/{id}`
- `DELETE /product/cart/items/{id}`
- `DELETE /product/cart/items`
- `POST /product/normal-orders`

### 6.3 普通订单

- `GET /order/normal-orders`
- `GET /order/normal-orders/{id}`
- `POST /order/normal-orders/{id}/pay`
- `POST /order/normal-orders/{id}/cancel`
- `GET /order/normal-orders/{id}/pay-status`

### 6.4 秒杀

- `GET /seckill-product/products`
- `GET /seckill-product/products/{id}`
- `POST /seckill/{productId}`
- `GET /seckill/result/{productId}`

### 6.5 秒杀订单

- `GET /order/seckill-orders`
- `GET /order/seckill-orders/{id}`
- `POST /order/seckill-orders/{id}/pay`
- `POST /order/seckill-orders/{id}/cancel`
- `GET /order/seckill-orders/{id}/pay-status`

### 6.6 AI 问答与知识库

- `POST /ai/chat`
- `GET /ai/chat/sessions`
- `GET /ai/chat/sessions/{sessionId}`
- `DELETE /ai/chat/sessions/{sessionId}`
- `POST /ai/chat/resolve-product`
- `POST /ai/knowledge/sync`
- `GET /ai/knowledge/sync/{taskId}`
- `GET /ai/knowledge/stats`

说明：

- `productType` 用于区分普通商品与秒杀商品，推荐前端在商品详情页提问时与 `productId` 一并传入
- 网关文档入口中 AI 分组对应 `/v3/api-docs/ai-service`

## 7. 两条核心业务链路

### 7.1 普通商品下单链路

```text
前端 -> Gateway -> product-service -> OpenFeign -> order-service
```

说明：

1. 前端不再直接提交商品 `items` 创建普通订单
2. `product-service` 读取当前用户已选中的购物车商品
3. `product-service` 校验库存、商品状态和金额
4. `product-service` 通过 Feign 调用 `order-service` 内部建单接口
5. 前端再调用普通订单支付接口完成模拟支付

### 7.2 秒杀链路

```text
前端 -> Gateway -> seckill-service -> Redis/Lua -> RabbitMQ -> order-service
```

说明：

1. 秒杀入口先做库存和重复下单控制
2. 同步返回排队/受理结果
3. 异步由 `order-service` 落库创建秒杀订单
4. 前端通过轮询结果接口确认是否抢购成功

## 8. 搜索与分类约定

### 8.1 商品列表查询参数

`GET /product/products`

支持参数：

- `name`
- `status`
- `categoryId`
- `categories`
- `categories[]`

### 8.2 搜索语义

当前搜索支持两类命中方式：

- 商品名称模糊匹配
- 分类名称关键词匹配

示例：

- 搜索 `耳机`：按商品名命中
- 搜索 `酒水饮料`：按分类命中
- 搜索 `饮料`：按分类关键词命中

## 9. Nacos 配置分层约定

当前推荐方式：

- `application.yml`：服务名、Nacos 地址、`spring.config.import`
- `application-local.yml`：本地兜底配置
- Nacos：共享配置与服务私有配置

推荐 Data ID：

- `flash-sale-common.yaml`
- `flash-sale-jwt.yaml`
- `gateway.yaml`
- `auth-service.yaml`
- `product-service.yaml`
- `order-service.yaml`
- `seckill-service.yaml`
- `ai-service.yaml`

扩展共享配置可使用：

- `flash-sale-mysql.yaml`
- `flash-sale-redis.yaml`
- `flash-sale-rabbitmq.yaml`

## 10. 文档维护约定

- 启动方式变化时优先更新 `README.md`
- 模块边界变化时优先更新本文件
- 页面联调口径变化时优先更新 `前后端交互规范.md`
- 仍未完成的接口能力变化时优先更新 `前端完整化待补充后端接口清单.md`
