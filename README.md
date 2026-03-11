

# Flash Sale System（高并发秒杀系统）

## 一、项目介绍

Flash Sale System 是一个模拟电商平台秒杀活动的高并发系统。
项目重点解决高并发场景下的 **库存超卖、系统压力过大、请求突发流量** 等问题。

系统通过 **Redis缓存、Lua脚本、消息队列、接口限流** 等技术手段，实现高并发秒杀业务的稳定运行。

该项目主要用于学习和实践：
- 高并发系统设计
- Redis缓存策略
- 消息队列异步处理
- 接口限流
- 微服务架构

---

## 二、技术栈

### 后端
- Java
- Spring Boot
- Spring Cloud Gateway
- MyBatis Plus
- Redis
- RabbitMQ
- MySQL
- JWT

### 前端
- Vue3
- Element Plus
- Axios

### 基础设施
- Nginx
- Docker（可选）

---

## 三、系统架构



系统整体架构如下：

![image-20260311103524352](C:\Users\NeoZeng\AppData\Roaming\Typora\typora-user-images\image-20260311103524352.png)

```text
用户
 │
Nginx（负载均衡）
 │
Gateway（统一网关）
 │
Seckill Service（秒杀服务）
 │
Redis（缓存库存）
 │
RabbitMQ（异步订单）
 │
Order Service（订单服务）
 │
MySQL（数据存储）
```

**系统核心思想：**

- 秒杀请求只处理 **库存校验**
- 订单创建通过 **MQ异步处理**
- Redis承担 **高并发读写压力**

------

## 四、核心业务流程

### 秒杀流程

Plaintext

```
用户点击秒杀 -> Gateway鉴权 -> Redis检查库存 -> Redis扣减库存 -> 发送MQ消息 -> 返回秒杀结果
```

### 后台消费者流程

Plaintext

```
RabbitMQ消费消息 -> 创建订单 -> 写入数据库
```

------

## 五、数据库设计

**数据库名称：** `flash_sale`

### 1. 用户表 (`user`)

| **字段**      | **类型** | **说明** |
| ------------- | -------- | -------- |
| `id`          | bigint   | 用户ID   |
| `username`    | varchar  | 用户名   |
| `password`    | varchar  | 密码     |
| `create_time` | datetime | 创建时间 |

### 2. 商品表 (`product`)

| **字段**        | **类型** | **说明**     |
| --------------- | -------- | ------------ |
| `id`            | bigint   | 商品ID       |
| `name`          | varchar  | 商品名称     |
| `price`         | decimal  | 原价         |
| `seckill_price` | decimal  | 秒杀价格     |
| `stock`         | int      | 库存         |
| `start_time`    | datetime | 秒杀开始时间 |
| `end_time`      | datetime | 秒杀结束时间 |

### 3. 秒杀订单表 (`seckill_order`)

| **字段**      | **类型** | **说明** |
| ------------- | -------- | -------- |
| `id`          | bigint   | 订单ID   |
| `user_id`     | bigint   | 用户ID   |
| `product_id`  | bigint   | 商品ID   |
| `status`      | int      | 订单状态 |
| `create_time` | datetime | 创建时间 |

------

## 六、Redis Key 设计

Redis 在系统中主要用于：**秒杀库存、商品缓存、用户秒杀状态、接口限流**。

### 1. 秒杀库存

- **Key**: `seckill:stock:{productId}`
- **Value**: `100` (存储商品库存数量)

### 2. 用户是否抢购

防止用户重复抢购。

- **Key**: `seckill:user:{productId}:{userId}`
- **Value**: `1`

### 3. 秒杀商品缓存

- **Key**: `seckill:product:{productId}`
- **Value**: 商品信息 JSON

### 4. 商品列表缓存

- **Key**: `seckill:product:list`
- **Value**: 商品列表 JSON

### 5. 接口限流

用于控制用户请求频率。

- **Key**: `rate_limit:{userId}`
- **Value**: 请求次数

------

## 七、防止库存超卖

**核心方案：** 使用 **Redis + Lua脚本**

**Lua脚本逻辑：**

1. 判断库存是否大于0
2. 判断用户是否已抢购
3. 扣减库存
4. 标记用户已抢购

**Lua脚本保证：** 原子性、高并发安全

**示例伪代码逻辑：**

Lua

```
if stock > 0 then
    stock = stock - 1
else
    return fail
end
```

------

## 八、消息队列设计

**系统使用：** `RabbitMQ`

**用于处理：** 订单异步创建

### 消息结构

JSON

```
{
  "userId": 1,
  "productId": 1001
}
```

### 消费流程

```
MQ消息` -> `Order Service` -> `创建订单` -> `写入数据库
```

**优点：**

- 削峰填谷
- 降低数据库压力
- 提升系统吞吐量

------

## 九、接口设计

### 1. 登录接口

- **请求**: `POST /api/login`
- **返回**: JWT token

### 2. 商品列表

- **请求**: `GET /api/products`
- **返回**: 返回商品列表

### 3. 秒杀接口

- **请求**: `POST /api/seckill/{productId}`
- **流程**: `验证token` -> `Redis扣库存` -> `发送MQ` -> `返回结果`

### 4. 我的订单

- **请求**: `GET /api/orders`
- **返回**: 查看用户订单

------

## 十、接口限流设计

为防止恶意刷接口，系统实现：**Redis + 令牌桶算法**

- **限制：** 1秒最多10次请求

------

## 十一、前端页面设计

**前端使用：** Vue3 + Element Plus

| **页面**       | **功能**     |
| -------------- | ------------ |
| **登录页**     | 用户登录     |
| **商品列表页** | 展示秒杀商品 |
| **秒杀结果页** | 显示抢购结果 |
| **订单页**     | 查看订单     |

------

## 十二、项目目录结构

Plaintext

```
flash-sale-system
 │
 ├── gateway            # API网关
 ├── seckill-service    # 秒杀服务
 ├── order-service      # 订单服务
 ├── common             # 公共模块
 ├── frontend           # Vue前端
 └── docs               # 项目文档
```

------

## 十三、项目亮点

1. 使用 **Redis缓存库存**，减少数据库压力
2. 使用 **Lua脚本保证库存扣减原子性**
3. 使用 **RabbitMQ实现订单异步处理**
4. 使用 **JWT实现用户认证**
5. 使用 **Redis实现接口限流**
6. 使用 **Nginx实现负载均衡**

------

## 十四、开发计划（10天）

- **Day 1**:  系统设计 + 项目初始化
- **Day 2**:  用户登录模块
- **Day 3**:  商品模块
- **Day 4**:  秒杀核心逻辑
- **Day 5**:  Redis库存 + Lua脚本
- **Day 6**:  RabbitMQ异步下单
- **Day 7**:  订单模块
- **Day 8**:  接口限流
- **Day 9**:  前端页面
- **Day 10**: 项目优化 + README

------

## 十五、学习目标

通过该项目掌握：

- 高并发系统设计
- Redis缓存策略
- 消息队列削峰
- 分布式系统基础
- Java后端工程实践

------

## 十六、未来优化方向

后续可以增加：

- 分布式锁
- Sentinel限流
- Elasticsearch日志分析
- 分布式ID（Snowflake）
- 微服务注册中心（Nacos）

------

## 十七、作者

**Java Backend Developer**

- **技术栈**：Java / Spring Boot / Redis / MySQL / RabbitMQ / Vue

```
---

Would you like me to help you write the `init.sql` database initialization script or the specific Lua script (`seckill.lua`) to include in your project's `docs` folder?
```