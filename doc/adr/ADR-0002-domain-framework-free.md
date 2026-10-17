# ADR-0002: 领域层 100% 纯 JDK 零框架依赖

## 状态

已接受 (Accepted) — 2026-09

## 上下文 (Context)

很多开发者在使用 Spring Boot 开发时，习惯在所有 Service、Entity 上随手打上 `@Service`、`@Component`、
`@Transactional`、`@Table` 等框架注解。
一旦领域模型与技术框架深度耦合：

1. 领域模型无法脱离 Spring 容器运行，单元测试必须启动庞大的容器上下文，测试耗时从毫秒级退化为秒级；
2. 数据库或 ORM 框架升级换代（如 MyBatis 升级为其他方案）时，领域核心代码直接受损；
3. 框架侵入污染了纯业务意图表达。

## 决策 (Decision)

1. **领域层零依赖**：`com.example.biz.domain..` 包内必须保持 100% 纯 JDK 代码，
   禁止 import 任何 `org.springframework..`、`jakarta..`、`org.apache.ibatis..`、
   `io.swagger..`、`lombok..` 类与注解；
2. **领域服务纯 Java 化**：`OrderPricingService` 是纯 Java 类，通过构造器注入纯 Java 依赖；
   在基础设施层 `infrastructure/config/DomainConfig` 中显式声明为 `@Bean`；
3. **极速单测**：领域层单元测试（`OrderTest`, `MoneyTest`, `OrderPricingServiceTest`）
   纯靠 JUnit 5 运行，无需 `@SpringBootTest`，单测在毫秒内完成。

## 后果 (Consequences)

### 正向收益

- 纯粹的业务逻辑表达，代码可读性与自解释性极高；
- 单元测试运行极快，开发反馈周期缩短到极致；
- 领域代码具备绝对的可移植性与长期生命力。

### 潜在风险与应对

- 开发者初上手时可能习惯用 `@Service`，
  ArchUnit 的 `domainShouldBeFrameworkFree` 规则会在测试阶段直接拦截并报错。
