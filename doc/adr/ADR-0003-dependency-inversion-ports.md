# ADR-0003: 依赖倒置端口设计（Repository 与 Gateway）

## 状态

已接受 (Accepted) — 2026-09

## 上下文 (Context)

业务系统必然需要持久化存储和与外部上下游系统通信（如订单域在计价时需要查询商品中心获取商品价格与有效性）。
在传统架构中，代码往往直接在业务层调用 DAO 或 FeignClient，导致外部系统的接口模型、异常和通信协议直接泄露进业务内核，形成高耦合。

## 决策 (Decision)

1. **依赖倒置原则 (DIP)**：领域层只负责定义“我需要什么数据/能力”，不关心“怎么实现的”；
2. **Repository 端口**：在 `domain/repository/` 定义 `OrderRepository` 接口，
   接口出入参只能是聚合根（`Order`）或值对象，严禁出现 `OrderDO`；
3. **Gateway 外部端口**：在 `domain/gateway/` 定义 `ProductGateway` 端口，
   出入参为领域值对象（`ProductInfo`），作为外部系统的防腐隔离接口；
4. **技术实现下沉**：
   - `OrderRepositoryImpl`、`OrderMapper`、`OrderDO`、
     `OrderConverter` 位于 `infrastructure/repository/`；
   - `DemoProductGateway` 位于 `infrastructure/gateway/`；
   由 Spring 在启动时将基础设施实现注入到 Application/Domain 中。

## 后果 (Consequences)

### 正向收益

- 领域层完全隔绝外部依赖的技术震荡（外部商品系统改版或底层数据库切换均不影响领域规则）；
- 单元测试时可直接通过 Mockito 对 Repository 和 Gateway 接口打桩，实现业务规则的完全独立验证。
