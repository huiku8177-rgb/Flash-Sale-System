# Flash-Sale-System 架构与接口规范（V1.1）

> 本文档聚焦当前**已完成模块**：`auth-service`（登录/注册）与 `gateway`（统一路由 + JWT 鉴权）。

---

## 1. 当前架构总览（已落地部分）

```text
客户端
  │
  ▼
Gateway（路由 + 鉴权 + 用户透传）
  ├── /auth/**   -> auth-service（登录/注册，免鉴权）
  ├── /order/**  -> order-service（需鉴权）
  └── /seckill/**-> seckill-service（需鉴权）
```

### 1.1 Gateway 职责边界

- 统一入口：所有请求先进入 Gateway。
- 鉴权校验：提取 `Authorization: Bearer <token>` 并校验 JWT。
- 白名单放行：登录、注册接口无需 token。
- 用户透传：鉴权成功后向下游追加 `X-User-Id` 请求头。

### 1.2 Auth-Service 职责边界

- 用户注册：账号唯一性校验 + BCrypt 密码加密存储。
- 用户登录：账号密码校验成功后签发 JWT。
- 认证能力集中：JWT 签发逻辑由 `common` 模块的 `JwtTool` 提供。

---

## 2. 统一响应规范

所有业务接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2026-03-11T17:30:00"
}
```

字段说明：

- `code`：业务状态码（非 HTTP 状态码）
- `message`：业务提示信息
- `data`：业务数据（可为 `null`）
- `timestamp`：服务端响应时间

### 2.1 状态码约定

#### 通用状态码

- `200`：成功
- `400`：请求参数错误
- `401`：未认证
- `403`：无权限
- `500`：系统异常

#### 秒杀业务状态码（预留）

- `2001`：库存不足
- `2002`：重复秒杀
- `2003`：通用业务异常

---

## 3. Auth-Service 接口规范（已实现）

Base Path：`/auth`

### 3.1 用户注册

- **URL**：`POST /auth/register`
- **鉴权**：否（白名单）
- **请求体**：

```json
{
  "username": "neo",
  "password": "123456"
}
```

- **成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "timestamp": "2026-03-12T10:20:00"
}
```

- **失败场景（建议）**：
  - 用户名已存在 -> `code=2003`，`message=用户名已存在`

### 3.2 用户登录

- **URL**：`POST /auth/login`
- **鉴权**：否（白名单）
- **请求体**：

```json
{
  "username": "neo",
  "password": "123456"
}
```

- **成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "username": "neo",
    "token": "eyJhbGciOiJSUzI1NiJ9..."
  },
  "timestamp": "2026-03-12T10:21:00"
}
```

- **失败响应（示例）**：

```json
{
  "code": 401,
  "message": "unauthorized",
  "data": null,
  "timestamp": "2026-03-12T10:21:10"
}
```

---

## 4. Gateway 鉴权规范（已实现）

### 4.1 路由规则

- `/auth/**` -> `auth-service`
- `/order/**` -> `order-service`
- `/seckill/**` -> `seckill-service`
- `/seckill-product/**` -> `seckill-service`

### 4.2 白名单

当前白名单路径：

- `/auth/login`
- `/auth/register`

### 4.3 鉴权流程

1. 判断请求路径是否命中白名单；命中则直接放行。
2. 非白名单请求，读取 `Authorization` 头。
3. 校验头格式必须为：`Bearer <token>`。
4. 调用 `JwtTool.parseToken(token)` 解析用户信息。
5. 解析失败 -> 返回 HTTP 401。
6. 解析成功 -> 向下游透传请求头：`X-User-Id: <userId>`。

### 4.4 交互约束

- 下游服务禁止自行重复解析 JWT，统一依赖 Gateway 透传用户身份。
- 下游服务获取当前用户时，统一读取 `X-User-Id`。

---

## 5. 安全设计说明（已落地）

### 5.1 密码安全

- 注册时使用 `BCryptPasswordEncoder` 加密存储。
- 登录时使用 `matches` 进行哈希校验，不做明文比较。

### 5.2 JWT 安全

- 使用 `RSA` 密钥对签名（`RS256`）。
- Token 携带 `user` 载荷与过期时间。
- 解析时进行：
  - 签名校验
  - 过期校验
  - 载荷格式校验

---

## 6. 当前实现与后续规划边界

### 6.1 当前商品表设计说明

- 秒杀商品当前使用 `seckill_product` 表。
- 普通商品当前使用 `product` 表，已接通首页展示与商品详情场景。
- 当前秒杀商品接口已拆分到 `/seckill-product/**`；普通商品接口已落地到 `/product/**`。
- 源码命名上，秒杀商品链路已统一调整为 `SeckillProductController / Service / Mapper / PO / VO`，避免与普通商品 `ProductPO / ProductVO` 混淆。

### 已完成

- Auth-Service 登录注册主流程
- Gateway 全局鉴权过滤器
- JWT 签发与解析工具
- 统一返回结构与状态码

### 待完成（下一阶段）

- 普通商品查询与详情接口
- 秒杀接口（库存校验 + 防重复购买）
- Redis + Lua 原子扣库存
- RabbitMQ 异步下单
- 订单查询接口
- 统一异常处理器（`@RestControllerAdvice`）

---

## 7. 联调示例

### 7.1 登录获取 token

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"neo","password":"123456"}'
```

### 7.2 携带 token 访问受保护接口

```bash
curl http://localhost:8080/order/test \
  -H "Authorization: Bearer <your-token>"
```

---

## 8. 版本记录

- `V1.0`：统一返回规范初版。
- `V1.1`：补充已实现的 Auth 与 Gateway 架构、接口、鉴权流程规范。
