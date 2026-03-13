1. 商品模块
2. 秒杀基础版（DB版）
3. 订单查询
4. Redis商品缓存
5. Redis库存预热
6. 防重复下单
7. Lua原子扣减
8. RabbitMQ生产者
9. RabbitMQ消费者
10. 幂等控制
11. 接口限流
12. 前端联调
13. 压测与优化
14. README修订

# 总体开发路线（10天）

```text
Day1  系统设计 + 项目初始化
Day2  用户登录模块
Day3  商品模块
Day4  秒杀核心逻辑
Day5  Redis库存 + Lua脚本
Day6  RabbitMQ异步下单
Day7  订单模块
Day8  接口限流
Day9  前端页面
Day10 项目优化 + README
```

------

# Day1：系统设计 + 项目初始化（非常关键）

时间：5小时

很多人跳过这一步，这是错误的。

你要完成 **3 件事**。

------

## 1. 项目架构设计（1小时）

先画架构图：

```text
用户
 │
Nginx
 │
Gateway
 │
Seckill Service
 │
Redis
 │
RabbitMQ
 │
Order Service
 │
MySQL
```

保存到：

```
docs/architecture.png
```

------

## 2. 创建项目结构（1小时）

项目目录：

```
flash-sale-system
│
├── gateway
├── seckill-service
├── order-service
├── common
├── frontend
└── docs
```

技术：

- Spring Boot
- Maven

------

## 3. 数据库设计（2小时）

创建数据库：

```sql
flash_sale
```

表：

### user

```sql
id
username
password
create_time
```

### product

```sql
id
name
price
stock
start_time
end_time
```

### seckill_order

```sql
id
user_id
product_id
status
create_time
```

------

## 4. 环境准备（1小时）

启动：

- MySQL
- Redis
- RabbitMQ

------

# Day2：用户登录模块

时间：6小时

目标：

实现

```
JWT登录
```

接口：

```
POST /api/login
```

流程：

```
用户登录
→ 校验账号密码
→ 生成 JWT
→ 返回 token
```

完成：

- login API
- JWT工具类
- 登录拦截器

------

# Day3：商品模块

时间：5小时

目标：

实现

```
秒杀商品列表
```

接口：

```
GET /api/products
```

返回：

```json
[
 {
  "id":1,
  "name":"iPhone",
  "price":5999,
  "seckillPrice":999,
  "stock":100
 }
]
```

完成：

- 商品表 CRUD
- 商品查询接口
- Redis缓存商品

Redis key：

```
seckill:product:{id}
```

------

# Day4：秒杀核心逻辑

时间：6小时

实现接口：

```
POST /api/seckill/{productId}
```

流程：

```
用户点击秒杀
↓
判断秒杀时间
↓
判断库存
↓
判断是否重复购买
```

暂时可以直接访问数据库。

------

# Day5：Redis库存 + Lua脚本（核心技术）

时间：6小时

目标：

解决：

```
库存超卖
```

流程：

```
Redis预加载库存
```

key：

```
seckill:stock:{productId}
```

库存操作：

用 **Lua脚本**

逻辑：

```
判断库存
扣库存
判断用户是否抢过
```

保证：

```
原子性
```

这是项目 **最重要技术点之一**。

------

# Day6：RabbitMQ异步下单

时间：6小时

问题：

秒杀请求太多。

解决：

```
MQ削峰
```

流程：

```
秒杀成功
↓
发送MQ消息
↓
消费者创建订单
```

消息：

```json
{
"userId":1,
"productId":2
}
```

消费者：

```
order-service
```

------

# Day7：订单模块

时间：5小时

接口：

```
GET /api/orders
```

功能：

查看：

```
我的订单
```

数据库：

```
seckill_order
```

------

# Day8：接口限流

时间：4小时

防止：

```
刷秒杀接口
```

实现：

```
Redis + 令牌桶
```

限制：

```
1秒10次
```

key：

```
rate_limit:{userId}
```

------

# Day9：前端页面

时间：6小时

使用：

```
Vue3 + Element Plus
```

做 **4个页面**：

1 登录页
2 商品列表
3 秒杀按钮
4 我的订单

不要花太多时间在 UI。

------

# Day10：项目优化 + README

时间：4小时

必须做：

### 1 架构图

放到：

```
README.md
```

------

### 2 项目说明

写：

```
项目背景
技术栈
架构设计
核心技术
```

------

### 3 GitHub上传

仓库结构：

```
flash-sale-system
```

README写清楚。

------

# 每天学习 + 项目时间建议

每天：

```
项目开发 5h
Java基础 1h
算法 1题
```

保持：

```
技术 + 项目
```

同时进步。

------

# 项目完成后，你的简历会变成

项目1：

```
高并发秒杀系统
```

项目2：

```
分布式短链接系统
```

技术栈：

```
Java
Spring Boot
Redis
RabbitMQ
MySQL
Vue
```

这是 **非常标准的 Java 实习配置**。

------

最后给你一个真实建议：

**不要急着写代码。**

明天第一件事是：

```
画架构图
设计Redis
设计数据库
```

工程能力 **70%来自设计**。

------

如果你愿意，我可以帮你再补一个 **非常关键的东西**：

👉 **秒杀系统 Redis Key 完整设计（企业级写法）**

很多项目其实就是 **Redis设计不专业**，这个会直接影响你项目质量。