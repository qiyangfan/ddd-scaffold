# ddd-scaffold 交接说明（Handoff）

## 状态

已交付 — 2026-10-17。39 个测试全绿，七道门禁全过，已推送 `main`，CI 绿灯。

## 本次变更集做了什么

把"面向大厂 + AI-native"调研所得的工程实践一次性反哺进脚手架，共八项：

| # | 事项 | 落点 |
| --- | --- | --- |
| 1 | JDK 21 → 25 单一基线 | `pom.xml` + `ci.yml` |
| 2 | 质量门禁工具链升级 | `pom.xml` |
| 3 | 主键 UUID v7 应用侧生成 | `domain/support/Ids.java` |
| 4 | 状态列 `VARCHAR + CHECK` | `OrderStatus` + `V1__baseline.sql` |
| 5 | Flyway 单一 baseline | `db/migration/` |
| 6 | API 契约入库 + 漂移门禁 | `contracts/` + `OpenApiContractTest` |
| 7 | 畸形入参 fail-closed | Controller + `GlobalExceptionHandler` |
| 8 | 文档集规范化 + markdownlint 门禁 | `doc/` + `.markdownlint.json` |

## 关键决策与依据

全部写成 ADR 了，**改代码前先读 ADR，不要凭记忆推断**：

- **ADR-0006** 主键 UUID v7：删除 `Order.attachId()`，聚合在工厂方法里就拥有身份，
  仓储只持久化不回填。
- **ADR-0007** Flyway 单一 baseline：schema 只走迁移，禁双脚本；
  状态列 `VARCHAR + CHECK` 与领域枚举共用同一份取值清单。
- **ADR-0008** JDK 25 与门禁升级：记录了升 25 实测踩到的四个真实门槛（见下）。
- **ADR-0009** 契约单一来源：`contracts/openapi.json` 入库，CI 用 git diff 阻断漂移。

### 升 JDK 25 的四个真实门槛（最容易踩，务必先读 ADR-0008）

1. **JaCoCo 0.8.12 只官方支持到 Java 22**，跑在 25 上覆盖率统计失真而非报错；
2. **JDK 24+ javac 不再默认从 classpath 发现注解处理器**，Lombok 静默失效——
   不报错，只报"变量未初始化"这类间接错误，最难排查；
3. **PMD 3.26 → 3.28 内置 PMD 7**，规则收紧，一次报出多个真实违规；
4. **Boot 4 把 Flyway 自动配置移出 `spring-boot-autoconfigure`**，
   必须显式依赖 `spring-boot-flyway`，否则 Flyway 根本不跑、库表空却构建成功。

第 2、4 条是"安静失败"，比直接报错危险得多。

## 质量门禁（七道，缺一不可）

```bash
mvn clean verify -DskipTests=false -Dcontract.export=true
npx --yes markdownlint-cli2@0.23.3 --config .markdownlint.json "**/*.md"
```

| 门禁 | 拦什么 |
| --- | --- |
| Spotless 3.10.4 | 格式 |
| PMD 3.28.0（内置 PMD 7.17.0） | 静态缺陷 |
| SpotBugs 4.10.5.0 | 字节码缺陷 |
| JaCoCo 0.8.15 | Instruction ≥ 70%、Branch ≥ 50% |
| ArchUnit 1.5.1 | 分层方向、domain 零框架依赖、聚合切片无环 |
| markdownlint 0.23.3 | 文档结构与行宽 |
| 契约漂移 | `git status --porcelain contracts/` 必须为空 |

> 改了接口代码，必须重跑第一条命令并提交 `contracts/openapi.json`，
> 否则 CI 的契约漂移门禁直接红。

## 遗留待办（按优先级）

1. **`infinite-recursion` 静态告警是误报**：`Money.compareTo`/`hashCode` 的接收者是
   `BigDecimal` 字段而非 `this`，`R.ok()` 是 0 参→1 参重载派发。
   若将来把 ast-grep 升为门禁，需要配 suppress，**不要为了消告警去改生产代码**。
2. **sqlfluff 报 `V1__baseline.sql` 无法解析**：根因是未声明方言（脚本是 MySQL/H2-MySQL 方言，
   sqlfluff 默认按 ANSI 解析）。若引入 sqlfluff，需配 `dialect = mysql`。
3. **`DemoProductGateway` 是演示桩**： intentionally 简单，替换真实商品服务时走
   `domain/gateway` 端口，勿把外部调用塞进 domain。
4. **jscpd 会报与 `mvc-scaffold` 大量重复**：两个脚手架是独立仓库、必须各自成立，
   重复是设计意图。若在工作区级跑 jscpd，需把两项目排除。

## 接手后第一件事

先跑一遍上面两条命令确认门禁全绿，再读 `doc/adr/ADR-0008` 与 `ADR-0006`——
这两个 ADR 记录了本次最容易改错的地方。

## 相关文档

- `doc/architecture.md`：四层边界与关键设计决策
- `doc/conventions.md`：编码规约与七道门禁的修复命令
- `doc/adr/`：ADR-0001 ~ ADR-0009
- `doc/sql/README.md`：Flyway 工作流与 schema 纪律
