# ddd-scaffold 工程编码与质量门禁规范 (Conventions)

本文档是 `ddd-scaffold` 的详细工程规范。所有代码变更必须严格遵守，CI 质量门禁会自动执法。

---

## 一、代码风格与编程规范（对齐阿里黄山版）

### 1. 结构与语法

- **分支大括号**：`if`、`else`、`for`、`while`、`do` 必须使用大括号 `{}`，即使只有单行。
- **Switch 分支**：`switch` 语句必须包含 `default` 分支，即使为空或抛出异常。
- **禁止魔法值**：代码中禁止出现未经预定义的字面量数字或字符串；必须使用枚举或常量类。
- **类型使用规约**：
  - POJO / DO / DTO / VO 属性必须使用包装类型（如 `Long`、`Integer`、`BigDecimal`），防止拆箱 NPE；
  - 方法内部的高频局部变量推荐使用基本类型。
- **集合与字符串判空**：
  - 字符串处理必须使用 `org.apache.commons.lang3.StringUtils`（`isNotBlank` / `isBlank`）；
    严禁使用 `org.springframework.util.StringUtils` 或手动编写
    `str == null || str.isEmpty()`。
  - 集合判空必须使用 `CollectionUtils.isEmpty()` / `isNotEmpty()`。
  - 集合方法返回空时，必须返回 `Collections.emptyList()` 或不可变空集合，严禁返回 `null`。
- **相等判断防空**：使用 `Objects.equals(a, b)` 或常量放左侧，禁止在不可控对象上直接调用 `.equals()`。

### 2. 事务与数据库规约

- **事务回滚声明**：`@Transactional` 必须显式声明 `rollbackFor = Exception.class`，防止受检异常跳过回滚。
- **SQL 与 MyBatis**：
  - 严禁使用 `SELECT *`，必须显式书写需要查询的列名。
  - 参数绑定必须使用 `#{}`，严禁使用 `${}` 防止 SQL 注入。
  - 批量插入/更新必须评估单批次数据量（推荐 ≤ 500 条）。
- **主键（ADR-0006）**：
  - 统一 UUID v7 字符串，由 `domain/support/Ids.newId()` 在领域工厂方法内生成；
  - 严禁新增自增主键，严禁为回填主键给聚合加 setter（`attachId()` 已废弃删除）；
  - MyBatis 插入不使用 `useGeneratedKeys`；DAO 的主键类型为 `String`。
- **Schema 纪律（ADR-0007）**：
  - 结构变更只走 `src/main/resources/db/migration/V{n}__{描述}.sql`，**已发布迁移禁止改内容**；
  - 状态列一律 `VARCHAR + CHECK`，取值清单与领域枚举严格一致；严禁 `SMALLINT/TINYINT` 数字码 + 注释；
  - 时间列统一 `gmt_create / gmt_modified`；已规划能力（有 spec 的）建表时一次到位，不留追补迁移；
  - DDL 保持 H2(MySQL 模式) 与 MySQL 双兼容，不写引擎/字符集/表级 COMMENT 等方言子句。
- **平台级上限单一来源**：分页上限、页码上界等底线参数只定义在 `domain/support/PageLimits`，
  api 校验注解与仓储兜底钳制必须引用同一常量，禁止两处各写一份字面量。

### 2.1 配置与凭证规约

- **关键配置 fail-closed**：启动缺失即失败的配置必须让应用**起不来**（`@ConfigurationProperties` + 校验/
  强类型绑定），严禁"取不到就静默回落默认值"——静默默认值是把配置错误推迟到生产事故。
- **凭证治理**：密钥/令牌只走环境变量或配置中心，严禁入库、严禁进日志、严禁硬编码；
  具备可轮换性（轮换不需改代码）；对外部系统的调用凭证只存在于 infrastructure 适配器内。

### 3. 日志与异常规约

- **日志输出规范**：
  - 日志文本必须使用英文（面向运维监控与国际化）。
  - 必须使用参数占位符 `{}`，严禁使用字符串 `+` 拼接日志。
  - `log.error` 记录异常时，最后一个参数必须传递异常对象 `e`，以便打印完整堆栈。
  - 严禁使用 `System.out.println`、`System.err.println` 或 `e.printStackTrace()`。
- **异常捕获与翻译**：
  - 严禁捕获异常后吞掉（Empty Catch Block）。
  - 自定义异常必须保留原始根因（`new BizException(code, e)`），禁止断裂调用栈。

---

## 二、质量门禁准出标准（Quality Gates）

本项目配置了七道自动化质量门禁，执行命令：

```bash
mvn clean verify -DskipTests=false
# 改了接口代码，提交前必须同步契约（CI 会做漂移门禁）
mvn clean verify -DskipTests=false -Dcontract.export=true

# 文档门禁：规则见 .markdownlint.json；CI 用同一条命令拦。
# glob 必须加引号，否则 bash 把 ** 当单层 *，会漏掉 README.md 与 doc/adr/*.md
npx --yes markdownlint-cli2@0.23.3 --config .markdownlint.json "**/*.md"
```

| 工具 | 门禁类型 | 阈值 / 规则 | 修复命令 / 处理方案 |
| :--- | :--- | :--- | :--- |
| **Spotless (3.10.4)** | 格式化规范 | 严格 4 空格缩进、去除无用 import、尾部换行 | `mvn spotless:apply` 一键自动格式化 |
| **PMD (3.28.0 / PMD 7.17.0)** | 静态代码分析 | 0 Violation（优先排查并发、潜在空指针、异常丢失） | 查看 `target/pmd.xml` 针对性修复代码 |
| **SpotBugs (4.10.5.0)** | 字节码缺陷扫描 | 0 Bug（Effort=Max, Threshold=Medium） | 查看 `target/spotbugs.xml` 修复潜在缺陷 |
| **JaCoCo (0.8.15)** | 单元测试覆盖率 | Instruction ≥ 70%, Branch ≥ 50% | 为未覆盖分支补充针对性单测用例 |
| **ArchUnit (1.5.1)** | 架构守护测试 | 100% 规则通过（分层单向依赖、domain 零框架依赖、无环） | 检查类所属包及 import 方向，严禁违背 DIP |
| **markdownlint (0.23.3)** | 文档结构与行宽 | 标题/列表/表格空行、表格分隔行风格、围栏须标语言、正文行宽 ≤ 80 | `npx --yes markdownlint-cli2@0.23.3 --config .markdownlint.json "**/*.md"` 定位后手工修 |
| **契约漂移** | 接口契约一致性 | `git status --porcelain contracts/` 必须为空 | `mvn clean verify -DskipTests=false -Dcontract.export=true` 后提交 `contracts/openapi.json` |

> **JDK 25 专项注意（ADR-0008）**：JDK 24+ 起 javac 不再默认从 classpath 发现注解处理器，
> Lombok 失效时不会直接报错，只会报"变量未初始化"这类间接错误。`maven-compiler-plugin` 的
> `annotationProcessorPaths` 是必需配置，新增注解处理器必须在此登记。

---

## 三、Git 提交与发布规范

- **提交信息必须中文**：所有 `git commit -m` 消息使用中文撰写，格式对齐 Conventional Commits：
  - `feat(order): 新增订单退款状态跃迁规则`
  - `fix(pricing): 修复满减优惠金额超过订单总额时的边界下限问题`
  - `test(aggregate): 补充订单并发取消的 CAS 重试单测`
  - `refactor(repo): 优化明细批量插入逻辑以避免长事务`

> **行宽规则的边界**：`MD013` 对表格行与代码块豁免（见 `.markdownlint.json`）——表格行无法折行，代码折了就是破坏代码。
这不是放松标准，是两类内容在结构上不可能满足 80 列。
