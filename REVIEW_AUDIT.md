# Flash-Sale-System 审查报告

本报告基于当前仓库代码、配置与 Markdown 文档做静态审查，重点检查：

1. 代码规范是否统一。
2. 前后端接口与交互约定是否一致。
3. Markdown 文档是否与当前实现存在偏差。

---

## 一、结论摘要

### 1. 代码规范层面

整体上，项目已经形成了比较清晰的分层：

- 前端：`api`、`router`、`views`、`composables`、`stores` 分离明确。
- 后端：`controller`、`service`、`mapper`、`domain` 分层明确。
- 通用能力：`common` 模块集中放置 `Result`、异常、JWT、Redis Key。

但仍存在几类明显的不统一：

- **Java 版本约定不统一**：父工程统一声明 `Java 17`，但 `gateway` 模块编译插件仍使用 `source/target=11`。
- **依赖风格不统一**：`order-service` 同时引入了 `Spring Data JPA`，但当前实现实际走的是 MyBatis XML Mapper，容易造成技术栈描述混乱。
- **注入风格不统一**：绝大多数类使用 `@RequiredArgsConstructor` 构造注入，但 `UserController` 仍保留字段注入 `@Autowired`。

### 2. 前后端约定层面

当前前后端主链路已经基本对齐，尤其是这些接口是吻合的：

- `/auth/login`
- `/auth/register`
- `/product/products`
- `/product/products/{id}`
- `/seckill-product/products`
- `/seckill-product/products/{id}`
- `/seckill/{productId}`
- `/seckill/result/{productId}`
- `/order/orders`
- `/order/orderDetail/{id}`

前端 Axios 也已经按统一约定处理：

- 自动携带 `Authorization: Bearer <token>`。
- 统一处理 `Result<T>` 的 `code != 200`。
- 统一处理网关 `401` 并跳转登录页。

不过仍有一些约定层面的风险点：

- **商品接口也需要鉴权**，这与常见“商品可匿名访问”的预期不同，文档里虽有说明，但 README 中没有突出这点。
- **`/auth/logout` 和 `/auth/me` 仍是占位实现**，前端已规避强依赖，但文档必须持续标注“不可作为完整能力使用”。
- **订单接口存在两套路径**：`/order/**` 与 `/seckill-order/**`，前者已纳入网关，后者未纳入网关，容易让后续开发误用。

### 3. Markdown 文档层面

存在明显“已实现 / 规划中 / 过时内容混写”的现象，主要集中在：

- README 仍混入较早期的规划内容。
- `planning.md` 仍以老接口前缀 `/api/**` 和旧目录名描述系统。
- README 的技术栈描述与当前代码实现不完全一致。
- README 中的架构图链接是本机 Windows 绝对路径，仓库内无法使用。

---

## 二、代码规范审查

### 1. 后端分层与命名

优点：

- `auth-service`、`gateway`、`seckill-service`、`order-service`、`common` 的职责边界清晰。
- 商品与秒杀商品链路已经拆分：`Product*` 与 `SeckillProduct*` 命名清楚。
- 统一响应 `Result<T>` 与统一业务码 `ResultCode` 已形成稳定约定。

建议关注：

#### 1.1 Java 版本不统一

父工程明确声明：统一使用 `Java 17`，但 `gateway/pom.xml` 仍配置为 Java 11 编译，这与仓库中“已实现技术栈说明”的叙述不一致，也破坏了多模块统一编译基线。

#### 1.2 注入方式不统一

目前大多数 Controller/Service 使用构造注入（`@RequiredArgsConstructor`），这是一致且更推荐的写法；但 `UserController` 仍然使用字段注入 `@Autowired`，建议统一为构造注入。

#### 1.3 依赖声明不够收敛

`order-service` 引入了 `spring-boot-starter-data-jpa`，但从当前控制器、服务、Mapper 与 XML 的实现看，核心链路依然是 MyBatis 风格。若短期不使用 JPA，建议去掉，避免误导文档与维护者。

---

## 三、前后端约定审查

### 1. 当前已经对齐的约定

#### 1.1 网关入口与前端默认配置一致

前端 `.env.example` 默认网关地址为 `http://localhost:8080`，与 Gateway 配置端口一致。

#### 1.2 路由与 API 调用基本一致

前端 API 文件与后端 Controller 路由匹配良好，说明当前联调主链路是通的（至少从静态定义层面如此）。

#### 1.3 鉴权约定一致

后端网关：

- 放行 `/auth/login`、`/auth/register`
- 其他接口默认要求 Bearer Token
- 向下游透传 `X-User-Id`

前端也按这个模型实现了：

- 请求拦截器统一带 token
- 收到 401 自动清理登录态并跳回登录页

### 2. 当前存在的约定风险

#### 2.1 `/product/**` 与 `/seckill-product/**` 也在网关鉴权范围内

这意味着：首页商品与秒杀商品列表当前都属于“登录后可见”。

这并不是错误，但要确认是否符合业务预期：

- 如果你希望“首页游客可看商品，只有下单才登录”，那么当前实现和文档都需要调整。
- 如果你就是希望整个前台必须登录后使用，那么 README 里应该明确写出来。

#### 2.2 占位接口仍存在

当前 `POST /auth/logout` 与 `POST /auth/me` 都返回占位成功值，不是真实用户会话能力。这种情况下：

- 前端不能把它们当成正式业务接口。
- 文档必须持续标记“占位实现”。

#### 2.3 订单接口重复

当前订单服务保留了两套查询接口：

- `/order/orders`、`/order/orderDetail/{id}`
- `/seckill-order/list`、`/seckill-order/{id}`

其中前者是主入口，后者更像兼容/历史遗留接口。建议后续明确：

- 要么保留但文档中声明“兼容接口，不推荐前端使用”；
- 要么逐步移除，减少歧义。

---

## 四、Markdown 文档审查

### 1. `README.md`

#### 主要问题

1. **技术栈描述不完全准确**
   - 文档写了 **MyBatis Plus**，但当前代码使用的是 `mybatis-spring-boot-starter` 与 XML / 注解 Mapper，不能等同于 MyBatis Plus。
   - 文档前端技术栈写了 `Vue3 + Element Plus + Axios`，但当前仓库还明确使用了 `Vue Router` 与 `Vite`，建议补齐。

2. **包含本地绝对路径图片**
   - README 中架构图使用的是 Windows 本机路径：`C:\Users\...`。
   - 这在仓库、GitHub、协作环境中都会失效。

3. **“已实现”和“规划中”混写**
   - README 一部分在介绍当前落地能力，一部分又写成整体愿景（如 Redis/Lua/MQ 核心逻辑总览）。
   - 如果这些能力已经部分实现，建议明确为“已实现 / 部分实现 / 规划中”。

4. **基础设施描述偏理想化**
   - README 写了 Nginx、Docker，但当前仓库没有对应配置文件或部署说明。
   - 如果只是目标方案，建议标注为“可选部署方案，仓库暂未提供配置”。

### 2. `planning.md`

这是当前偏差最大的文档之一。

#### 主要问题

1. **目录名已经过时**
   - 文档里写的是：`gateway / seckill-service / order-service / common / frontend / docs`
   - 实际仓库是：`flash-sale-serve/*` 与 `flash-sale-ui`。

2. **接口前缀已经过时**
   - 文档仍使用 `/api/login`、`/api/products`、`/api/seckill/{productId}` 这种早期设计。
   - 当前真实接口已经演进为 `/auth/**`、`/product/**`、`/seckill-product/**`、`/seckill/**`、`/order/**`。

3. **文档偏“开发计划”而非“当前状态说明”**
   - 如果保留该文件，建议在文件开头明确：这是历史规划，不是当前接口真相。

### 3. `已实现技术栈解析与使用说明.md`

这个文档整体和当前代码的贴合度较高，是当前最接近“真实现状”的文档之一。

#### 仍建议修正的点

- 文档中写“微服务模块统一基于 Java 17 运行”，但 `gateway` 模块编译插件仍是 Java 11。
- 文档中写“已配置但业务能力尚未完整落地：Redis、RabbitMQ、秒杀核心领域模型与订单领域模型”，而当前仓库其实已经有：
  - Lua 脚本
  - MQ Producer / Consumer
  - 秒杀结果轮询
  - 订单查询

更准确的表达应该是：

- **Redis/Lua/MQ 主链路已具备基础实现**；
- **但压测、完整容灾、运维化配置仍未完备**。

### 4. `Flash-Sale-System架构与接口规范.md`

该文档整体质量较高，但有一个表述层面的轻微不一致：

- 开头的“当前架构总览”只列了 `/auth/**`、`/order/**`、`/seckill/**`。
- 但后文的 Gateway 路由规则中，又补充了 `/seckill-product/**`。
- 实际代码中还配置了 `/product/**`。

建议开头的总览也同步更新，避免读者误以为商品接口不走网关。

### 5. `前后端交互规范.md`

这是当前最贴近前后端联调现实的一份文档，整体可信度较高。

#### 仍建议补充的点

- 文档已说明 `/seckill-order/**` 未接入网关，这点很好；但可以再强调“前端统一不要使用这组接口”。
- 文档已说明商品接口需要登录，建议把这点放到更靠前位置，避免前端先入为主认为商品列表是匿名接口。

### 6. `前端完整化待补充后端接口清单.md`

这份文档定位清晰：它是“下一阶段待补接口清单”，而不是“现状说明”，因此总体没有明显问题。

它与当前前端代码也基本一致，特别是：

- 购物车仍是前端本地草案；
- 个人中心仍缺资料、地址、支付状态等接口；
- 首页运营位和搜索联想仍未后端化。

这份文档可以继续保留。

---

## 五、建议的修复优先级

### P0（建议优先修）

1. 修正 `README.md` 中的错误技术栈描述（尤其是 **MyBatis Plus**）。
2. 修正 README 架构图链接，改为仓库内相对路径资源。
3. 修正 `planning.md` 中的旧目录与旧接口前缀，或明确标注其为历史规划。
4. 统一 `gateway` 模块的 Java 编译版本到 17。

### P1（建议近期修）

1. 将 `UserController` 改为构造注入。
2. 清理 `order-service` 中当前未使用或容易误导的 JPA 依赖。
3. 在 README 中明确“商品接口当前也需要登录”。
4. 在架构规范总览中补充 `/product/**` 与 `/seckill-product/**`。

### P2（可迭代优化）

1. 给“已实现/部分实现/待实现”建立统一标签体系，避免所有文档口径不一。
2. 清理历史遗留接口文档，减少 `/seckill-order/**` 这类重复入口造成的认知负担。
3. 如果后续保留购物车为前端草案，建议在 README 单独声明“当前无真实购物车后端”。

---

## 六、总体评价

从仓库现状看，你的项目**已经不是“纯规划阶段”**，而是具备了：

- 统一网关鉴权
- 登录注册
- 普通商品查询
- 秒杀商品查询
- 秒杀发起 + 轮询结果
- 订单查询
- 前端联调页面

所以当前最大的问题已经不再是“代码有没有搭起来”，而是：

- **文档分层不够清晰**；
- **一部分历史文档没有随着代码演进同步更新**；
- **少量代码规范（Java 版本、依赖、注入方式）还有不统一**。

如果你下一步是准备继续完善项目，我建议优先先做一次“文档与约定收口”，再继续扩功能。这样后续不管是自己维护还是给别人看，理解成本都会低很多。
