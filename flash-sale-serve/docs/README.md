# 后端文档入口

这个目录存放 `flash-sale-serve` 的部署、配置和数据库相关文档，面向后端开发、自测联调和 GitHub 展示。

## 目录结构

```text
flash-sale-serve/docs
├─ README.md
├─ docker-compose.infrastructure.yml
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

1. [中间件 Docker Compose](./docker-compose.infrastructure.yml)
2. [Nacos 配置接入指南](./nacos-config-guide.md)
3. [Nacos 模板说明](./nacos-templates/README.md)
4. [AI Service 本地接入说明](./ai-service-setup.md)
5. `sql` 目录下的初始化脚本和增量脚本

## Docker Compose

当前仓库提供了一份本地联调用的中间件编排文件：

- [docker-compose.infrastructure.yml](./docker-compose.infrastructure.yml)

默认包含：

- `Redis 7.2`，端口 `6379`，密码 `123321`
- `RabbitMQ 3.13 Management`，端口 `5672 / 15672`，账号 `guest / guest`
- `Nacos 2.3`，端口 `8848 / 9848 / 9849`

可选包含：

- `MySQL 8.0`，端口 `3306`，账号 `root / 123456`，数据库 `flash_sale`

启动基础中间件：

```bash
docker compose -f flash-sale-serve/docs/docker-compose.infrastructure.yml up -d
```

连同 MySQL 一起启动：

```bash
docker compose -f flash-sale-serve/docs/docker-compose.infrastructure.yml --profile with-mysql up -d
```

停止并删除容器：

```bash
docker compose -f flash-sale-serve/docs/docker-compose.infrastructure.yml down
```

如果同时启用了 Compose 里的 MySQL，并且你本机已经占用了 `3306`，请先调整端口映射，或者只启动默认的 `Redis / RabbitMQ / Nacos`。

## 本地联调说明

仓库里提交的 `Nacos` 模板和部分默认配置仍然保留云服务器地址 `192.168.100.130`，这是当前项目的主口径，不建议为了本地 Docker 联调直接改掉并提交。

如果你只是想在本机用这份 Compose 临时起一套中间件，建议用以下方式覆盖：

- 启动服务时通过环境变量覆盖，例如：
  - `FLASH_SALE_NACOS_ADDR=localhost:8848`
  - `FLASH_SALE_REDIS_HOST=localhost`
  - `FLASH_SALE_RABBITMQ_HOST=localhost`
- 或者在本地 Nacos 中导入一份面向本机环境的副本配置
- 或者仅把 Compose 当作演示环境，不替换当前云服务器配置

也就是说：

- 提交到仓库的模板，继续保留云环境地址
- 本机 Docker 联调，用环境变量或本地副本覆盖

## 文档职责

### 1. Nacos 与服务配置

- [nacos-config-guide.md](./nacos-config-guide.md)
  说明当前项目如何接入 Nacos 配置中心、哪些配置适合迁移到 Nacos，以及推荐的配置分层方式。
- [nacos-templates/README.md](./nacos-templates/README.md)
  说明模板目录里每个 YAML 的作用，以及导入 Nacos 时的建议顺序。

### 2. AI 服务本地联调

- [ai-service-setup.md](./ai-service-setup.md)
  说明 `ai-service` 的本地启动方式、Swagger 地址、网关聚合要求和 API Key 放置方式。
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
- 环境差异配置优先放到 Nacos 或运行时环境变量
- 模板文件保持“可复制、可改值、不直接携带生产密钥”的原则
- SQL 脚本尽量按“初始化”和“增量修复”拆开

## 当前维护重点

目前这组文档重点覆盖 4 件事：

- 后端服务如何通过 Nacos 管理配置
- 本地联调如何快速起 Redis / RabbitMQ / Nacos
- AI 服务如何安全地做本地调试
- 数据库脚本和模板文件应该从哪里看、怎么用
