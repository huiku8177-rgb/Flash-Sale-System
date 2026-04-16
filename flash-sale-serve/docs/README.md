# 后端文档入口

这个目录存放 `flash-sale-serve` 的部署、配置和数据库相关文档，面向后端开发、自测联调和后续 GitHub 展示。根目录 `README.md` 负责项目入口，这里负责后端专题说明。

## 目录结构

```text
flash-sale-serve/docs
├─ README.md
├─ ai-service-local-env.example.ps1
├─ ai-service-setup.md
├─ nacos-config-guide.md
├─ nacos-templates
│  ├─ README.md
│  └─ *.yaml
└─ sql
   └─ *.sql
```

## 推荐阅读顺序

1. [Nacos 配置接入指南](./nacos-config-guide.md)
2. [Nacos 模板说明](./nacos-templates/README.md)
3. [AI Service 本地接入说明](./ai-service-setup.md)
4. `sql` 目录下的初始化脚本和增量脚本

## 文档职责

### 1. Nacos 与服务配置

- [nacos-config-guide.md](./nacos-config-guide.md)
  说明当前项目怎么接入 Nacos 配置中心、哪些配置适合进 Nacos、推荐的分层方式和迁移顺序。

- [nacos-templates/README.md](./nacos-templates/README.md)
  说明模板目录里每个 YAML 的用途，以及导入 Nacos 时的建议顺序。

### 2. AI 服务本地联调

- [ai-service-setup.md](./ai-service-setup.md)
  说明 `ai-service` 的本地启动方式、Swagger 地址、网关聚合要求、API Key 的安全放置方式。

- [ai-service-local-env.example.ps1](./ai-service-local-env.example.ps1)
  PowerShell 会话级环境变量示例，只影响当前窗口，适合本地调试。

### 3. 数据库脚本

`sql` 目录用于存放：

- 初始化脚本
- 表结构补丁
- 索引补丁
- 数据修复脚本
- 示例数据脚本

当前常用脚本包括：

- `flash_sale_init.sql`
- `seed_normal_product_demo.sql`
- `ai-service-chat.sql`
- `add_normal_order_tables.sql`
- `add_seckill_order_lifecycle_fields.sql`
- `repair_seckill_product.sql`

## 使用约定

- 真实密钥不要提交到 Git 跟踪文件
- 环境差异配置优先走 Nacos 或运行时环境变量
- 模板文件保持“可复制、可改值、不可直接带密钥上线”的原则
- SQL 脚本尽量按“初始化”与“增量修复”拆开，避免后续难以追溯

## 当前维护重点

目前这组文档重点覆盖 3 件事：

- 后端服务如何通过 Nacos 分层管理配置
- AI 服务如何安全地做本地联调
- 数据库脚本和模板文件应该从哪里看、怎么用

如果后面要继续扩展，优先补：

- 服务启动顺序和依赖检查清单
- MySQL / Redis / RabbitMQ / Nacos 的环境示例
- 各服务的接口联调说明
