# ddd-scaffold 架构设计与技术决策说明

## 1. 架构全景

本项目遵循标准四层洋葱/六边形架构思想，并在工程落地中采用**工业级轻量化裁剪**：

```text
+-------------------------------------------------------------------+
|                            API 表现层                              |
|   (OrderController, OrderAssembler, OrderCreateRequest, R)        |
+-------------------------------------------------------------------+
                                  │
                                  ▼
+-------------------------------------------------------------------+
|                           APPLICATION 应用层                       |
|   (OrderApplicationService, CreateOrderCommand, PageResult)       |
+-------------------------------------------------------------------+
                                  │
                                  ▼
+-------------------------------------------------------------------+
|                             DOMAIN 领域层                          |
|   (Order 聚合根, OrderItem 实体, Money 值对象,                     |
|    OrderPricingService 领域服务, OrderRepository/Gateway 端口)     |
+-------------------------------------------------------------------+
                                  ▲ (DIP 依赖倒置)
                                  │
+-------------------------------------------------------------------+
|                        INFRASTRUCTURE 基础设施层                   |
|   (OrderRepositoryImpl, OrderMapper, OrderDO, OrderConverter,     |
|    DemoProductGateway, DomainConfig, MyBatisConfig)               |
+-------------------------------------------------------------------+
```

### 1.1 分层职责与边界约束

| 层级 | 职责说明 | 典型类 | 访问边界 |
| :--- | :--- | :--- | :--- |
| **api** | HTTP 接入、参数前置校验、领域聚合/值对象向 VO 转换、全局异常映射 | `OrderController`, `OrderAssembler` | 仅调用 Application 层；可只读引用 Domain 层枚举/VO 进行视图组装 |
| **application** | 用例流程编排、开启事务边界、跨聚合协调、事件发布。**严禁编写业务规则** | `OrderApplicationService` | 调用 Domain 聚合根、领域服务与仓储/网关接口；不可直接触碰持久化实现 |
| **domain** | 纯净业务内核。包含聚合根不变量、状态跃迁校验、富值对象计算、领域服务规则 | `Order`, `Money`, `OrderPricingService` | 零框架依赖（纯 JDK）；对外暴露 Repository 与 Gateway 端口接口 |
| **infrastructure** | 技术中间件与外部能力适配。数据库访问、RPC/HTTP 网关通信、Spring Bean 装配 | `OrderRepositoryImpl`, `DemoProductGateway` | 实现 Domain 端口；对上层业务完全隐形（依赖注入） |

---

## 2. 关键设计决策（ADR 概要）

### 决策 1：单 Maven 模块 + ArchUnit 架构守护（替代多模块地狱）

- **背景**：教科书级 DDD 常将工程拆解为 5~6 个 Maven 子模块（`api/app/domain/infra/start`），
  导致日常字段变更需在多个 pom 与模块间反复横跳，编译与调试成本高昂。
- **裁决**：统一在单个 Maven 模块中按清晰包路径组织，通过 **ArchUnit** 编写架构测试（`ArchitectureTest`），
  在构建与 CI 阶段硬性断言依赖方向与领域层零框架约束。
- **收益**：兼备单模块的高效开发体验与物理多模块的强防腐隔离。

### 决策 2：依赖倒置端口：Repository 与 Gateway

- **背景**：领域层既需要存储数据，又需要依赖外部系统数据（如订单定价需要获取外部商品中心价格）。
- **裁决**：
  1. `domain/repository`：面向数据库存储的依赖倒置端口，入参出参仅允许领域对象；
  2. `domain/gateway`：面向外部微服务/RPC 的依赖倒置端口（防腐层 ACL），屏蔽外部技术协议；
  3. 实现类均位于 `infrastructure`，由 Spring 容器在运行时组装。

### 决策 3：领域服务纯 Java 化与基础设施装配

- **背景**：领域服务（如 `OrderPricingService`）若使用 `@Service`，将导致 Spring 注解污染领域层。
- **裁决**：`OrderPricingService` 保持纯 Java 类，
  由 `infrastructure/config/DomainConfig` 显式声明 `@Bean`，装配控制权归属于基础设施层，领域层保持纯净。

### 决策 4：状态机内聚与 DB CAS 乐观防护

- **背景**：订单具有严格的生命周期跃迁（待支付 -> 已支付 / 已取消），且面临并发支付/重复提交风险。
- **裁决**：
  1. 聚合根内部维护状态流转逻辑，禁止外部直接 `setStatus`；
  2. 仓储层使用带有期望状态的 SQL 原子更新：
     `UPDATE t_order SET status=?, gmt_modified=? WHERE id=? AND status=?`；
  3. 更新受影响行数为 0 时，抛出 `ORDER_CONCURRENT_MODIFY` 并回滚事务，保障极端并发下的一致性。

### 决策 5：主键 UUID v7 应用侧生成（ADR-0006）

- **背景**：自增主键让聚合在工厂方法里"没有身份"，只能等仓储插入后回填，逼出 `attachId()` 这种"仅供仓储调用"的封装破洞；
  多实例唯一性还要另配雪花/号段基础设施；64 位 ID 出到前端又必须全局序列化成字符串。
- **裁决**：`domain/support/Ids` 生成 UUID v7，聚合创建即拥有身份；仓储只持久化不回填，`attachId()` 删除。
- **收益**：封装完整、多实例唯一天然成立、时间有序使主键索引保持顺序插入。

### 决策 6：Flyway 单一 baseline 与 schema 纪律（ADR-0007）

- **背景**：原实现有 `schema.sql`（H2）与 `doc/sql/init.sql`（MySQL 人工执行）两份结构定义，靠人工保持同构，
  MySQL 侧无版本概念；状态列用 `TINYINT` + 注释，脏数据难以审计。
- **裁决**：结构变更只走 `db/migration/` 单一 baseline；主键 UUID v7 字符串、
  状态列 `VARCHAR + CHECK` 与领域枚举同源、
  时间列统一、已规划能力一次建全、DDL 双库兼容。
- **收益**：schema 成为有版本、有校验和、可回放的工件，CI 全链路测试顺带验证迁移可执行。

### 决策 7：API 契约单一来源与 CI 漂移门禁（ADR-0009）

- **背景**：前后端契约靠口头约定与各写一份类型维持，漂移在联调甚至上线后才暴露。
- **裁决**：OpenAPI schema 为唯一事实源，`contracts/openapi.json` 由测试导出并入库，CI 用
  `git status --porcelain contracts/` 阻断漂移；前端类型一律由该文件生成。
- **收益**：契约漂移从"联调期人肉发现"变成"CI 期自动阻断"，零额外基础设施。

---

## 3. 数据流转与对象生命周期

```text
[HTTP Request]
       │
       ▼
OrderCreateRequest (DTO)
       │ (Controller 转命令)
       ▼
CreateOrderCommand (Record)
       │ (Application 协调网关与定价服务)
       ▼
Order (聚合根创建: Order.create(...))
       │
       ▼
OrderRepository.save(order)
       │ (Converter 转换，主键由领域层已生成，无需回填)
       ▼
OrderDO / OrderItemDO (MyBatis 入库)
       │
       ▼
OrderCreatedEvent (应用层事务提交后事件发布)
       │
       ▼
OrderResponse (Assembler 组装 VO 返回前端)
```
