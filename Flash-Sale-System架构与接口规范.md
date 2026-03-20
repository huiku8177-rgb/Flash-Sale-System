# Flash-Sale-System 架构与接口规范

> 版本：V3.0  
> 状态：以当前仓库代码为准

## 1. 总体架构

```text
Browser
  -> Gateway
      -> auth-service
      -> product-service
      -> seckill-service
      -> order-service

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

## 2. 服务边界

### 2.1 gateway

职责：

- 统一入口
- JWT 鉴权
- 白名单放行
- 向下游透传 `X-User-Id`
- 路由转发
- Swagger/OpenAPI 聚合入口

当前路由：

- `/auth/**`
- `/product/**`
- `/seckill/**`
- `/seckill-product/**`
- `/order/**`

### 2.2 auth-service

职责：

- 注册
- 登录
- 获取当前用户
- 修改密码

### 2.3 product-service

职责：

- 普通商品列表
- 普通商品详情
- 普通商品下单入口
- 普通商品库存校验与扣减
- 通过 OpenFeign 调用 `order-service` 创建普通订单

说明：

- 对外暴露普通下单入口
- 对内不承担订单查询与支付职责

### 2.4 seckill-service

职责：

- 秒杀商品列表
- 秒杀商品详情
- 秒杀请求入口
- 秒杀结果查询
- Redis/Lua 秒杀库存与防重控制
- 秒杀结果 Redis 缓存与数据库兜底

### 2.5 order-service

职责：

- 普通订单查询
- 普通订单模拟支付
- 普通订单支付状态查询
- 秒杀订单查询
- 秒杀订单模拟支付
- 秒杀订单支付状态查询
- 内部普通建单接口
- 消费秒杀消息并创建秒杀订单

说明：

- 不再承担普通商品查询与库存扣减职责
- 内部普通建单接口仅供 `product-service` 调用

## 3. 鉴权规范

### 3.1 白名单

- `POST /auth/login`
- `POST /auth/register`
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs/**`

### 3.2 受保护接口

除白名单外，其他接口统一需要：

```http
Authorization: Bearer <token>
```

Gateway 鉴权成功后向下游补充：

```http
X-User-Id: <userId>
```

### 3.3 设计约束

- 下游业务服务不重复解析 JWT
- 下游统一通过 `X-User-Id` 获取当前用户
- Swagger 对外契约只展示 `Authorization`，不展示内部头 `X-User-Id`

## 4. 文档规范

统一入口：

- `http://localhost:8080/swagger-ui.html`

当前聚合分组：

- 认证服务
- 普通商品服务
- 秒杀服务
- 订单服务

原始文档地址：

- `/v3/api-docs/auth-service`
- `/v3/api-docs/product-service`
- `/v3/api-docs/seckill-service`
- `/v3/api-docs/order-service`

## 5. 统一响应规范

统一返回结构：

```json
{
  "code": 200,
  "message": "成功",
  "data": {},
  "timestamp": "2026-03-20T12:00:00"
}
```

常见业务码：

- `200`：成功
- `400`：参数错误
- `401`：未登录或登录已失效
- `403`：无权限访问
- `500`：服务器内部错误
- `2001`：库存不足
- `2002`：请勿重复秒杀
- `2003`：业务处理失败

## 6. 数据库实体约定

### 6.1 用户

- 表：`user`

核心字段：

- `id`
- `username`
- `password`
- `create_time`

### 6.2 普通商品

- 表：`product`

核心字段：

- `id`
- `name`
- `subtitle`
- `category_id`
- `price`
- `market_price`
- `stock`
- `status`
- `main_image`
- `detail`

### 6.3 秒杀商品

- 表：`seckill_product`

核心字段：

- `id`
- `name`
- `price`
- `seckill_price`
- `stock`
- `status`
- `start_time`
- `end_time`

### 6.4 普通订单

- 表：`normal_order`
- 表：`normal_order_item`

核心字段：

- 主表：`order_no`、`user_id`、`order_status`、`total_amount`、`pay_amount`、`pay_time`、`remark`、`address_snapshot`
- 明细表：`product_id`、`product_name`、`product_subtitle`、`product_image`、`sale_price`、`quantity`、`item_amount`

### 6.5 秒杀订单

- 表：`seckill_order`

核心字段：

- `id`
- `user_id`
- `product_id`
- `seckill_price`
- `status`
- `create_time`

## 7. 对外接口约定

### 7.1 认证接口

- `POST /auth/login`
- `POST /auth/register`
- `POST /auth/logout`
- `GET /auth/me`
- `POST /auth/updatePassword`

### 7.2 普通商品接口

- `GET /product/products`
- `GET /product/products/{id}`
- `POST /product/normal-orders`

### 7.3 秒杀接口

- `GET /seckill-product/products`
- `GET /seckill-product/products/{id}`
- `POST /seckill/{productId}`
- `GET /seckill/result/{productId}`

### 7.4 普通订单接口

- `GET /order/normal-orders`
- `GET /order/normal-orders/{id}`
- `POST /order/normal-orders/{id}/pay`
- `GET /order/normal-orders/{id}/pay-status`

### 7.5 秒杀订单接口

- `GET /order/seckill-orders`
- `GET /order/seckill-orders/{id}`
- `POST /order/seckill-orders/{id}/pay`
- `GET /order/seckill-orders/{id}/pay-status`

## 8. 内部接口约定

仅服务间调用，不对前端开放：

- `POST /internal/orders/normal`
- `GET /internal/orders/normal/by-order-no`

调用关系：

- `product-service` -> OpenFeign -> `order-service`

设计原因：

- `product-service` 负责商品校验、库存扣减、订单快照组装
- `order-service` 只负责普通订单落库、查询与支付

## 9. 状态约定

### 9.1 秒杀订单状态

- `0`：待支付
- `1`：已支付
- `2`：已取消

### 9.2 普通订单状态

- `0`：待支付
- `1`：已支付
- `2`：已取消
- `3`：已发货
- `4`：已完成

### 9.3 秒杀结果状态

- `1`：秒杀成功
- `0`：处理中 / 排队中
- `-1`：秒杀失败

## 10. 当前实现特点

- 普通商品链路已从 `order-service` 解耦到 `product-service`
- 普通下单通过 OpenFeign 跨模块创建订单
- 秒杀链路仍为 Redis/Lua + RabbitMQ 异步建单
- 秒杀结果支持 Redis 结果查询 + 数据库兜底
- 普通订单与秒杀订单都已支持模拟支付
- Swagger 文档已统一接入网关聚合页，并统一中文展示
