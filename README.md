# Flash Sale System

## 项目简介

这是一个“普通商品商城 + 秒杀活动”一体化练手项目，当前已经具备完整的前后端联调基础：

- 后端采用 Spring Cloud 微服务拆分
- 网关统一做鉴权、转发、限流和 Swagger 聚合
- 普通商品与秒杀商品分链路处理
- 前端采用 Vue 3 + Vite + Element Plus，已具备登录、注册、商城首页、秒杀页、购物车、结算页、订单中心和账户中心

当前仓库分为两个子项目：

- `flash-sale-serve`：后端微服务
- `flash-sale-ui`：前端商城页面

## 当前能力概览

### 账号与身份

- 用户登录
- 用户注册
- 获取当前用户信息
- 修改密码
- 收货地址增删改查
- 设置默认收货地址

### 普通商品链路

- 商品列表与详情
- 支持商品名搜索
- 支持分类筛选
- 支持通过分类关键词命中商品搜索
- 购物车增删改查
- 独立结算页
- 创建普通订单
- 普通订单列表、详情、模拟支付、取消、支付状态查询

### 秒杀链路

- 秒杀商品列表与详情
- 发起秒杀
- 轮询秒杀结果
- 秒杀订单列表、详情、模拟支付、取消、支付状态查询

### 工程与联调能力

- Gateway 统一入口
- Nacos 注册中心 + 配置中心
- Swagger 聚合文档
- MySQL / Redis / RabbitMQ 基础设施
- 请求链路日志与限流基础能力

## 项目结构

```text
flash-sale-system
├─ flash-sale-serve
│  ├─ auth-service
│  ├─ common
│  ├─ gateway
│  ├─ order-service
│  ├─ product-service
│  ├─ seckill-service
│  └─ docs
├─ flash-sale-ui
├─ README.md
├─ Flash-Sale-System架构与接口规范.md
├─ 前后端交互规范.md
├─ 前端完整化待补充后端接口清单.md
├─ 已实现技术栈解析与使用说明.md
├─ planning.md
└─ REVIEW_AUDIT.md
```

## 技术栈

### 后端

- Java 17
- Spring Boot 3.2.5
- Spring Cloud 2023.0.1
- Spring Cloud Alibaba 2023.0.1.0
- Spring Cloud Gateway
- OpenFeign
- MyBatis
- SpringDoc OpenAPI
- MySQL
- Redis
- RabbitMQ
- Nacos

### 前端

- Vue 3
- Vue Router 4
- Vite 6
- Element Plus
- Axios

## 启动说明

### 1. 准备基础设施

请先启动：

- MySQL
- Redis
- RabbitMQ
- Nacos

### 2. 初始化数据库

SQL 文件位于：

- `flash-sale-serve/docs/sql`

至少需要完成：

- 初始化基础表结构
- 导入普通商品与秒杀商品演示数据
- 如果你使用了唯一索引增强，还需要执行用户名唯一索引脚本

### 3. 初始化 Nacos 配置

Nacos 相关文档位于：

- `flash-sale-serve/docs/nacos-config-guide.md`
- `flash-sale-serve/docs/nacos-templates/README.md`

当前项目采用：

- `application.yml` 负责 Nacos 接入入口
- `application-local.yml` 负责本地兜底配置
- Nacos 负责共享配置与服务私有配置

推荐上传的 Data ID：

- `flash-sale-common.yaml`
- `flash-sale-jwt.yaml`
- `auth-service.yaml`
- `gateway.yaml`
- `product-service.yaml`
- `order-service.yaml`
- `seckill-service.yaml`

如需把数据库、Redis、RabbitMQ 也统一收敛到 Nacos，可以继续上传：

- `flash-sale-mysql.yaml`
- `flash-sale-redis.yaml`
- `flash-sale-rabbitmq.yaml`

### 4. 启动后端服务

建议启动顺序：

1. `auth-service`
2. `product-service`
3. `seckill-service`
4. `order-service`
5. `gateway`

说明：

- 当前仓库内没有 Maven Wrapper
- 如果本机未安装 `mvn`，请通过 IDE 自带 Maven 启动服务

### 5. 启动前端

在 `flash-sale-ui` 目录执行：

```bash
npm install
npm run dev
```

## 默认访问地址

### 前端

- 商城前端：`http://localhost:5173`

### 后端

- 网关入口：`http://localhost:8080`
- Swagger 聚合页：`http://localhost:8080/swagger-ui.html`

## 当前前端主要路由

- `/#/login`
- `/#/register`
- `/#/app/home`
- `/#/app/flash`
- `/#/app/cart`
- `/#/app/profile`
- `/#/app/profile/orders`
- `/#/app/profile/account`
- `/#/app/profile/security`
- `/#/checkout`

其中：

- 首页和秒杀页支持匿名访问
- 购物车、个人中心、订单中心、账户信息、密码安全和结算页需要登录

## 核心后端模块职责

### `gateway`

- 统一入口
- JWT 鉴权
- 白名单放行
- 请求头透传
- Swagger 聚合
- 网关限流

### `auth-service`

- 登录、注册、退出登录
- 当前用户信息
- 修改密码
- 收货地址管理

### `product-service`

- 普通商品查询与详情
- 商品搜索与分类筛选
- 购物车能力
- 基于购物车创建普通订单

### `order-service`

- 普通订单查询、支付、取消
- 秒杀订单查询、支付、取消
- 内部普通订单创建接口
- 秒杀订单异步消费落库

### `seckill-service`

- 秒杀商品查询
- 秒杀请求入口
- Redis + Lua 控制秒杀库存
- RabbitMQ 投递异步建单消息

## 文档索引

根目录文档分工如下：

- `README.md`：项目总览、启动方式、入口说明
- `Flash-Sale-System架构与接口规范.md`：微服务边界、关键链路、接口分层与鉴权规则
- `前后端交互规范.md`：页面访问规则、联调口径、核心接口约定
- `前端完整化待补充后端接口清单.md`：仍值得继续补齐的后端能力
- `已实现技术栈解析与使用说明.md`：技术栈落地情况
- `planning.md`：当前阶段规划与建议推进顺序
- `REVIEW_AUDIT.md`：最近一轮项目与文档审计结论

## 当前已知说明

- 当前控制台直接查看中文 Markdown 时，PowerShell 可能出现乱码，这是终端编码问题，不代表文件内容错误
- 当前仓库本身没有 `mvnw`，后端编译和启动需要依赖本机 Maven 或 IDE
- `application-local.yml` 中的同名配置可能覆盖 Nacos 配置，联调时要注意配置优先级
