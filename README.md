# ddd-scaffold 手搓轻量 DDD 架构脚手架

[![CI](https://github.com/qiyangfan/ddd-scaffold/actions/workflows/ci.yml/badge.svg)](https://github.com/qiyangfan/ddd-scaffold/actions/workflows/ci.yml)

公司架构规范里唯一推荐的轻量 DDD 方案：**不引入任何 DDD 专用框架依赖**（COLA 不纳入选型池），用包分层 + ArchUnit 架构守护测试把"依赖规则（硬性约束）"落到 CI。核心业务域、规则持续迭代的业务用这套；简单 CRUD 用 [mvc-scaffold](../mvc-scaffold)。

## 技术栈（2026-09 裁决版本）

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 21（LTS） | 现代微服务黄金基线（虚拟线程/模式匹配），CI 矩阵覆盖 21/25 |
| Node.js | 24 | CI 与前端/契约工具链对齐版本 |
| Spring Boot | 4.1.1 | 3.5 线已于 2026-06-30 OSS 停更，新项目不再钉死 |
| MyBatis | spring-boot-starter 4.1.0 | 只允许 infrastructure 层依赖 |
| springdoc-openapi | 3.1.1 | 访问 `/swagger-ui.html`（端口 8081） |
| ArchUnit | 1.5.0（test） | 依赖规则在测试期强制执行，违反即构建失败 |
| Spotless | 2.44.3 | 代码格式化门禁，支持 `mvn spotless:apply` 一键排版 |
| SpotBugs | 4.8.6.6 | 字节码静态缺陷扫描门禁 |
| PMD | 3.26.0 | 代码规范与潜在缺陷静态检查门禁 |
| JaCoCo | 0.8.12 | 单元测试指令与分支覆盖率门禁 |
| H2 / MySQL | Boot 管理 | 默认 H2（MySQL 模式）开箱即跑 |

## 快速开始

```bash
mvn test -DskipTests=false          # 全量测试（含架构守护测试）
mvn spring-boot:run                 # 默认 H2 内置库
# 生产/联调：先执行 doc/sql/init.sql，再
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

## 包结构（固定，直接落地）

```
com.example.biz
├── api                 # 接口层：Controller、请求/响应 DTO、装配器、全局异常；只做校验/转发/组装
│   ├── assembler       # 聚合 -> 响应 DTO 的唯一转换点
│   ├── controller
│   ├── dto
│   └── web             # R / 全局异常 / Jackson 配置
├── application         # 应用层：用例编排、事务、领域事件发布；不写业务规则
│   ├── command         # 用例入参（api 转换后传入）
│   ├── OrderApplicationService
│   └── PageResult
├── domain              # 领域层（核心）：零框架依赖，纯 JDK
│   ├── aggregate       # Order 聚合根（状态机 + 不变量）
│   ├── entity          # OrderItem 聚合内实体
│   ├── valueobject     # Money / OrderStatus / ProductInfo
│   ├── service         # OrderPricingService 定价规则（无状态领域服务）
│   ├── repository      # OrderRepository 接口（仅定义）
│   ├── gateway         # ProductGateway 外部系统端口（对规范包结构的补充，见下）
│   ├── event           # 领域事件（纯 POJO）
│   └── exception       # DomainException / DomainErrorCode
└── infrastructure      # 基础设施层：实现 domain 定义的接口
    ├── repository      # OrderRepositoryImpl + Mapper + DO + Converter
    ├── gateway         # DemoProductGateway（演示实现，替换为 RPC/HTTP）
    ├── event           # 事件消费（AFTER_COMMIT，演示进程内实现）
    └── config          # Spring 装配（MapperScan、领域服务 Bean）
```

## 依赖规则（ArchUnit 强制）

```
api ──> application ──> domain <──(依赖倒置)── infrastructure
```

1. **api → application → domain**：单向，任何层禁止反向触碰 api；
2. **infrastructure 依赖 domain**（DIP）：domain 绝不反向依赖 infrastructure，且 infrastructure 不可被任何层直接触碰（只通过接口被注入）；
3. **domain 零框架依赖**：禁止 Spring / Jakarta / MyBatis / Swagger / Lombok 出现在 domain；
4. **domain 允许外层只读访问**：api 组装视图、翻译领域异常时可直接读聚合/枚举，单向性不破坏。

以上由 `ArchitectureTest` 在 `mvn test` 阶段强制执行——**写错依赖方向，构建直接红**。

### 对规范包结构的唯一补充：`domain/gateway`

规范已固定 repository（持久化端口）。当领域逻辑还需要外部系统数据（如定价要查商品价），端口放哪？本脚手架定义 `domain/gateway`：与 repository 同属依赖倒置端口，repository 面向存储、gateway 面向外部系统，实现都在 `infrastructure/`。不接受可整体移除，demo 改为 application 层取数传入。

## 四层职责极简定义（示例对照）

| 层 | 职责 | 本例对应 |
|----|------|----------|
| api | 收 HTTP、参数校验、转命令、组装返回 | `OrderController` + `OrderAssembler` |
| application | 一个方法一个用例；开事务；协调领域对象与仓储；**不写业务规则** | `OrderApplicationService.createOrder/payOrder/cancelOrder` |
| domain | 全部核心业务规则；仓储接口只定义"要什么数据" | `Order`（状态机）、`OrderPricingService`（满减定价）、`Money`（金额语义） |
| infrastructure | 技术实现：Mapper 读写、网关调外部、模型转换、异常翻译 | `OrderRepositoryImpl`、`DemoProductGateway` |

**可读性对照**：业务规则问句"满 500 减 80 还是满 100 减 10？已支付能不能取消？"——答案只在 `domain/`；"什么时候开事务、先查哪个服务？"——答案只在 `application/`。

## 轻量取舍（不搞重型 DDD）

- ✅ 必选：聚合根、仓储接口、依赖倒置、网关防腐、全局异常翻译、ArchUnit 守护
- ⚖️ 可选：值对象（本例 Money 演示了为什么值得写）、领域事件（业务不需要就删 `event/`）
- ❌ 不做：CQRS 读写分离、事件溯源、聚合版本号乐观锁框架化、领域事件总线框架（Spring 事件够用）

## 关键机制

- **领域服务无 Spring 化**：`OrderPricingService` 是纯类，由 `infrastructure/config/DomainConfig` 声明为 Bean——domain 保持零依赖，装配权在基础设施层。
- **聚合回填主键**：仓储插入后经 `order.attachId()` 回填自增 ID（javadoc 标明仅供仓储调用）。
- **状态变更双层防重**：聚合状态机挡住串行非法迁移；仓储 `updateStatus(order, expectStatus)` 用 `UPDATE ... WHERE id=? AND status=?` 的 CAS 兜住并发丢失更新，冲突抛 `ORDER_CONCURRENT_MODIFY` 并回滚事务。
- **外部调用与事务**：当前演示网关在事务内取数无碍；替换为真实 RPC/HTTP 后应把外部调用移出事务（前置取数或 TransactionTemplate），避免连接池被拖垮。
- **领域事件**：应用层发布，`@TransactionalEventListener(AFTER_COMMIT)` 消费；多实例/跨服务场景把监听器换成 MQ 生产者，语义不变。
- **统一响应/错误码**：与 mvc-scaffold 完全同构（`R` + A/B/C 分级）；`DomainException` 在 api 层 `GlobalExceptionHandler` 翻译。
- **Long→String 序列化**、**分页上限 200**、**明细批量装载避免 N+1**：同 mvc-scaffold 约定。
- **订单不做删除**：状态机模型以终态（CANCELLED）终结，物理/逻辑删除都不适用于订单主档。

## 多实例部署约定

1. 应用无状态；进程内事件仅本实例可见，跨实例必须走 MQ/事务消息。
2. 订单号生成是时间戳+随机段的演示实现，生产替换为雪花 ID/号段模式（多实例唯一）。
3. 定时任务带分布式锁；共享状态走 Redis；写接口按需幂等——本例示范了完整防重组合：**内存状态机**拒绝非法迁移 + **DB 层状态 CAS**（`UPDATE ... WHERE status=期望值`）兜底并发双支付，冲突返回 `B0208` 提示客户端重试。
4. 深分页用游标方案替代大 offset。

## 新增一个业务域（清单）

1. `domain/<聚合>`：聚合根 + 实体 + 值对象 + 领域服务，规则全部内聚，单测先行。
2. `domain/repository`（存储端口）、`domain/gateway`（外部端口）只写接口。
3. `infrastructure/`：Mapper + DO + Converter 实现仓储；gateway 实现外部端口（显式超时 + 异常翻译）。
4. `application/`：一个用例一个方法，`@Transactional(rollbackFor = Exception.class)`，发布事件。
5. `api/`：DTO（英文校验 message）+ 装配器 + Controller。
6. 跑 `ArchitectureTest` 确认依赖方向没被破坏；补聚合单测 + 全链路集成测试。

## 测试与质量门禁

```bash
mvn clean verify -DskipTests=false  # 全量验证：测试 + Spotless + PMD + SpotBugs + JaCoCo + ArchUnit
mvn spotless:apply                  # 格式化代码（若 Spotless 校验不通过可一键修复）
```

- `MoneyTest` / `OrderTest` / `OrderPricingServiceTest`：纯领域单测，不启 Spring，毫秒级。
- `ArchitectureTest`：ArchUnit 守护分层方向（api -> application -> domain <- infra）、domain 零框架依赖、聚合切片无环。
- `OrderFlowTest`：HTTP 全链路（H2 + 演示网关），覆盖创建/支付/取消/异常分支/分页。
- `Spotless` / `PMD` / `SpotBugs` / `JaCoCo`：在 `verify` 阶段自动化执行质量门禁，未达标直接阻断构建。
