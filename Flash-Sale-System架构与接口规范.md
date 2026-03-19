# Flash-Sale-System 架构与接口规范

> 版本：V2.0  
> 状态：以当前仓库代码为准

## 1. 总体架构

```text
Browser
  -> Gateway
      -> auth-service
      -> seckill-service
      -> order-service
  -> MySQL
  -> Redis
  -> RabbitMQ
  -> Nacos
```

## 2. 服务边界

### 2.1 gateway

职责：

- 统一入口
- JWT 鉴权
- 白名单放行
- 向下游透传 `X-User-Id`
- 路由转发

当前路由：

- `/auth/**`
- `/order/**`
- `/seckill/**`
- `/seckill-product/**`
- `/product/**`

### 2.2 auth-service

职责：

- 注册
- 登录
- 获取当前用户
- 修改密码

### 2.3 seckill-service

职责：

- 普通商品查询
- 秒杀商品查询
- 秒杀请求入口
- 秒杀结果查询
- 秒杀库存控制
- 秒杀结果 Redis 缓存与数据库兜底

### 2.4 order-service

职责：

- 秒杀订单查询
- 秒杀订单模拟支付
- 普通订单创建
- 普通订单详情
- 普通订单模拟支付
- 两类订单支付状态查询

## 3. 鉴权规范

### 3.1 白名单

- `POST /auth/login`
- `POST /auth/register`

### 3.2 受保护接口

除白名单外，其他接口默认都需要：

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
- 商品接口当前也属于受保护接口

## 4. 统一响应规范

统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2026-03-19T12:00:00"
}
```

通用业务码：

- `200`
- `400`
- `401`
- `403`
- `500`
- `2001`
- `2002`
- `2003`

## 5. 当前数据库实体约定

### 5.1 用户

- 表：`user`

核心字段：

- `id`
- `username`
- `password`
- `create_time`

### 5.2 普通商品

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

### 5.3 秒杀商品

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

### 5.4 秒杀订单

- 表：`seckill_order`

核心字段：

- `id`
- `user_id`
- `product_id`
- `status`
- `create_time`

### 5.5 普通订单

- 表：`normal_order`
- 表：`normal_order_item`

核心字段：

- 主表：`order_no`、`user_id`、`order_status`、`total_amount`、`pay_amount`、`pay_time`、`remark`、`address_snapshot`
- 明细表：`product_id`、`product_name`、`product_subtitle`、`product_image`、`sale_price`、`quantity`、`item_amount`

## 6. 当前接口分层约定

### 6.1 Auth

- `POST /auth/login`
- `POST /auth/register`
- `POST /auth/logout`
- `GET /auth/me`
- `POST /auth/updatePassword`

### 6.2 商品

- `GET /product/products`
- `GET /product/products/{id}`
- `GET /seckill-product/products`
- `GET /seckill-product/products/{id}`

### 6.3 秒杀

- `POST /seckill/{productId}`
- `GET /seckill/result/{productId}`

### 6.4 订单

秒杀订单：

- `GET /order/orders`
- `GET /order/orderDetail/{id}`
- `POST /order/seckill-orders/{id}/pay`
- `GET /order/seckill-pay-status/{id}`

普通订单：

- `POST /order/checkout`
- `GET /order/normal-orders`
- `GET /order/normal-orders/{id}`
- `POST /order/normal-orders/{id}/pay`
- `GET /order/pay-status/{id}`

## 7. 订单状态约定

### 7.1 秒杀订单

- `0`：待支付
- `1`：已支付
- `2`：已取消

### 7.2 普通订单

- `0`：待支付
- `1`：已支付
- `2`：已取消
- `3`：已发货
- `4`：已完成

## 8. 前端接入约定

- 前端统一走网关，不再直连服务
- 前端不再使用 `/pay-product/**`
- 前端统一不使用 `/seckill-order/**` 历史接口
- 购物车当前为前端本地状态，不作为后端服务能力描述

## 9. 当前保留的实现特点

- 秒杀结果支持 Redis 结果查询 + 数据库兜底
- 秒杀订单与普通订单都已支持模拟支付
- 修改密码已改为基于当前登录用户，并校验旧密码

## 10. 版本说明

- `V2.0`：同步普通商品、普通订单、秒杀支付、`/auth/me`、修改密码校验、前端统一接网关后的现状
