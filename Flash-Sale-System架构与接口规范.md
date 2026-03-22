# Flash-Sale-System 架构与接口规范

> 版本：V4.0  
> 状态：以当前仓库实现为准

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

## 2. 模块边界

### 2.1 gateway

职责：
- 统一入口
- JWT 鉴权
- 方法级白名单放行
- 清洗客户端伪造的内部身份头
- 向下游注入 `X-User-Id`
- Swagger/OpenAPI 聚合

当前路由：
- `/auth/**`
- `/product/**`
- `/seckill/**`
- `/seckill-product/**`
- `/order/**`

### 2.2 auth-service

职责：
- 登录
- 注册
- 获取当前用户信息
- 修改密码
- 退出登录占位接口

### 2.3 product-service

职责：
- 普通商品列表与详情
- 购物车增删改查
- 普通商品库存校验与金额计算
- 基于已选购物车商品创建普通订单
- 通过 OpenFeign 调用 `order-service` 内部建单接口

明确不负责：
- 普通订单查询
- 普通订单模拟支付
- 普通订单支付状态查询

### 2.4 seckill-service

职责：
- 秒杀商品列表与详情
- 秒杀请求入口
- Redis/Lua 库存与重复抢购控制
- 秒杀结果缓存
- RabbitMQ 建单消息投递

### 2.5 order-service

职责：
- 普通订单列表、详情、模拟支付、支付状态
- 秒杀订单列表、详情、模拟支付、支付状态
- 内部普通订单建单接口
- 秒杀消息消费与异步建单

明确不负责：
- 普通商品查询
- 普通商品库存扣减
- 购物车

## 3. 外部接口与内部接口分层

### 3.1 外部接口
外部接口面向浏览器、前端页面和 Swagger 调试，全部统一走网关。

典型外部接口：
- `/auth/**`
- `/product/**`
- `/seckill/**`
- `/seckill-product/**`
- `/order/**`

### 3.2 内部接口
内部接口只面向服务间调用，不向前端开放，不出现在对外文档主流程里。

当前内部接口：
- `POST /internal/orders/normal`
- `GET /internal/orders/normal/by-order-no`

设计原则：
- 内部接口可以传递更贴近服务边界的命令对象
- 内部接口不应被前端直接依赖
- 内部接口需要从 Swagger 对外展示中隐藏

## 4. 鉴权规范

### 4.1 外部鉴权方式
除白名单接口外，统一要求：

```http
Authorization: Bearer <token>
```

### 4.2 下游身份传递
网关鉴权成功后，向下游透传：

```http
X-User-Id: <userId>
```

约束：
- 下游业务服务不重复解析 JWT
- 下游统一依赖 `X-User-Id` 获取当前用户身份
- 网关必须先移除外部请求携带的同名头，再写入真实用户 ID

### 4.3 网关白名单
当前白名单支持“路径”与“方法 + 路径”两种规则。

当前建议白名单：
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

## 5. 统一响应规范

所有业务接口统一使用 `Result<T>`：

```json
{
  "code": 200,
  "message": "成功",
  "data": {},
  "timestamp": "2026-03-22T12:00:00"
}
```

字段说明：
- `code`：业务码
- `message`：中文提示
- `data`：业务数据，可以为对象、数组或 `null`
- `timestamp`：服务端返回时间

常见业务码约定：
- `200`：成功
- `400`：参数错误
- `401`：未登录或登录已失效
- `403`：无权限访问
- `500`：系统异常

设计要求：
- 网关的未授权返回也必须使用 JSON 结构
- 前端统一按 `code`、`message` 和 HTTP 状态联合处理

## 6. 接口设计规范

### 6.1 路径规范
- 统一使用复数资源名，如 `/products`、`/normal-orders`
- 查询列表优先使用 `GET`
- 创建资源优先使用 `POST`
- 更新资源优先使用 `PUT`
- 删除资源优先使用 `DELETE`

### 6.2 参数规范
- 查询条件优先使用 query 参数
- 创建和更新优先使用 JSON 请求体
- 当前登录用户 ID 不允许前端显式传入业务请求体
- 用户身份统一由网关透传 `X-User-Id`

### 6.3 文档规范
- Swagger/OpenAPI 对外文案统一使用简体中文
- 只保留 2 到 3 个完整版示例接口
- 其他接口使用简化版 `@Operation(summary = "...")`
- 内部接口使用 `@Hidden` 隐藏

### 6.4 DTO / VO 规范
- DTO 只描述请求参数
- VO 只描述返回结构
- DTO 与 VO 使用中文 `@Schema` 描述
- 必填、长度、最小值等约束优先放在 DTO 上

## 7. 当前对外接口总览

### 7.1 认证接口
- `POST /auth/login`
- `POST /auth/register`
- `POST /auth/logout`
- `GET /auth/me`
- `POST /auth/updatePassword`

### 7.2 普通商品接口
- `GET /product/products`
- `GET /product/products/{id}`

### 7.3 购物车接口
- `GET /product/cart/items`
- `POST /product/cart/items`
- `PUT /product/cart/items/{id}`
- `DELETE /product/cart/items/{id}`
- `DELETE /product/cart/items`

### 7.4 普通订单接口
- `POST /product/normal-orders`
- `GET /order/normal-orders`
- `GET /order/normal-orders/{id}`
- `POST /order/normal-orders/{id}/pay`
- `GET /order/normal-orders/{id}/pay-status`

### 7.5 秒杀接口
- `GET /seckill-product/products`
- `GET /seckill-product/products/{id}`
- `POST /seckill/{productId}`
- `GET /seckill/result/{productId}`

### 7.6 秒杀订单接口
- `GET /order/seckill-orders`
- `GET /order/seckill-orders/{id}`
- `POST /order/seckill-orders/{id}/pay`
- `GET /order/seckill-orders/{id}/pay-status`

## 8. 普通商品下单设计约束

### 8.1 当前下单口径
`POST /product/normal-orders` 现在不是前端直接传商品 `items` 建单，而是：

- 前端在购物车中勾选商品
- 或通过“立即购买”先将当前商品设置为本次结算目标
- 结算页只提交 `remark` 与 `addressSnapshot`
- `product-service` 读取当前用户已选购物车项
- 校验商品、库存、价格并调用 `order-service` 内部接口建单

### 8.2 当前请求体

```json
{
  "remark": "工作日白天送达",
  "addressSnapshot": "{\"receiver\":\"Neo\",\"mobile\":\"13800000000\",\"detail\":\"上海市浦东新区xxx路100号\"}"
}
```

### 8.3 原因
- 订单商品来源以后端购物车数据为准，更稳
- 能避免前端篡改商品数量和金额口径
- 有利于后续扩展结算预览、批量选中、地址管理

## 9. 前端页面权限规范

### 9.1 匿名可访问
- `/#/app/home`
- `/#/app/flash`

### 9.2 必须登录
- `/#/app/cart`
- `/#/app/profile`
- `/#/checkout`

### 9.3 特殊规则
- 商品详情弹层在点击前先校验登录态
- 未登录点击“查看详情”直接跳转登录页
- 购物车和结算页为用户态页面，不允许匿名进入

## 10. Swagger 规范

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

推荐要求：
- 服务标题、分组名、摘要、示例全部使用简体中文
- 通过 `@Server(url = "/")` 让 Try it out 默认沿用网关入口

## 11. 当前核心数据表

### 11.1 用户
- `user`

### 11.2 普通商品
- `product`

### 11.3 购物车
- `cart_item`

### 11.4 秒杀商品
- `seckill_product`

### 11.5 普通订单
- `normal_order`
- `normal_order_item`

### 11.6 秒杀订单
- `seckill_order`

## 12. 后续建议

优先级最高的后续能力：
1. 地址管理
2. 购物车结算预览
3. 订单取消 / 超时关闭
4. 文档与测试进一步补齐
