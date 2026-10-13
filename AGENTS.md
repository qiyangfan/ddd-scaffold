# AGENTS.md — ddd-scaffold 架构宪法与执行者工作契约

> **项目定位**：轻量手搓 DDD 架构脚手架（标准简化版 DDD，对标工业级 TCP/IP 4 层模型），**不引入任何 DDD 专用重量级框架**（COLA 不纳入选型池），通过包分层 + ArchUnit 架构守护测试 + Spotless/PMD/SpotBugs/JaCoCo 质量门禁将依赖规则与代码质量固化至 CI。

---

## 一、架构宪法（不可逾越的六条红线，违背即打回）

1. **分层依赖倒置铁律（DIP）**
   - 依赖方向严格单向且内聚：`api ──> application ──> domain <──(依赖倒置)── infrastructure`。
   - `api`：只负责协议解析、入参校验、转命令、组装返回 DTO。任何层禁止反向触碰 `api`。
   - `application`：负责业务用例编排、事务边界（`@Transactional`）、领域事件发布，**绝对不写业务规则**。只能被 `api` 调用。
   - `domain`：核心业务逻辑内核，包含聚合根、实体、值对象、领域服务。允许被外层只读访问（读状态/组装视图），但**绝不反向依赖任何层**。
   - `infrastructure`：实现 domain 定义的 Repository 与 Gateway 端口，技术细节下沉。**禁止任何业务层直接触碰 infrastructure 内部类**（仅通过 Spring 依赖注入接口）。
   - **门禁执行**：`ArchitectureTest` 在编译与测试期通过 ArchUnit 强制断言，违背依赖方向直接构建失败。

2. **领域层纯洁性铁律（Domain Framework-Free）**
   - `domain/` 包内必须保持 **100% 纯 JDK**，**禁止引入任何框架与三方类库注解与类型**（包括但不限于 Spring、Jakarta、MyBatis、Swagger、Lombok、PageHelper 等）。
   - 领域服务（如 `OrderPricingService`）是纯 Java 类，禁止使用 `@Service`；必须由基础设施层 `infrastructure/config/DomainConfig` 显式声明为 Spring Bean。

3. **聚合根自治与不变量铁律**
   - 聚合根是状态与一致性的绝对边界，**禁止充血模型退化为贫血模型**。
   - 状态字段私有化，禁止外部直接 `setStatus`；状态机跃迁只能通过聚合根业务方法（如 `order.pay()` / `order.cancel()`）执行并强校验前置不变量。
   - 聚合构造器私有化，仅暴露 `create()` 工厂方法（业务创建）与 `restore()` 工厂方法（仅供仓储层从数据库重建聚合）。
   - 仓储层落库必须通过数据库 CAS 兜底（`UPDATE ... WHERE id=? AND status=?`），防范高并发丢失更新，冲突抛 `ORDER_CONCURRENT_MODIFY` 并回滚事务。

4. **端口与防腐隔离铁律**
   - 仓储接口（`domain/repository/`）面向聚合根，方法参数与返回值只能是聚合根或值对象，禁止泄露 DO。
   - 外部系统调用（如查商品信息）必须在 `domain/gateway/` 定义防腐端口接口，技术通信实现留在 `infrastructure/gateway/`，外部系统的 DTO/VO 严禁渗透进领域层。

5. **多实例无状态与事件约束**
   - 进程保持无状态：禁止使用 JVM 内存保存分布式共享状态，禁止静态可变变量。
   - 进程内领域事件（`@TransactionalEventListener(AFTER_COMMIT)`）仅适用于单实例事务解耦；多实例或跨服务协作时，必须在基础设施层监听器处替换为 MQ 消息投递。
   - 订单号/流水号生成当前为开箱演示逻辑，生产多实例部署必须替换为雪花算法（Snowflake）或分布式号段服务。

6. **质量门禁 100% 全绿铁律**
   - 任何改动提交前，必须在本地完整执行 `./mvnw clean verify -DskipTests=false`（或 `mvn clean verify -DskipTests=false`）。
   - **Spotless** 格式化检查、**PMD** 静态分析（0 violation）、**SpotBugs** 字节码缺陷扫描（0 bug）、**JaCoCo** 覆盖率门禁（Instruction ≥ 70%, Branch ≥ 50%）、**ArchUnit** 架构守护必须全部通过。

---

## 二、代码与模型规范（对齐阿里黄山版）

### 1. 语言规范
- **默认语言 = 简体中文**：面向开发者的对话、解释、代码注释（Javadoc/行注释）、技术文档、Git Commit Message、PR 描述必须使用简体中文。
- **英文硬性规范**：业务错误码信息（ErrorMessage）、DTO 校验信息（`@NotNull(message = "...")`）、HTTP 响应提示、日志文本（`log.info/error`）、代码标识符（类名/变量名/方法名/表名）一律使用英文。

### 2. 对象分层流转与转换模型
- **禁止一个对象穿透到底**，遵循严格的两端收敛映射：
  - **API 边界**：`OrderCreateRequest / OrderPageRequest` ➔ `OrderAssembler` ➔ `OrderResponse`（VO 视图对象，Money 和枚举在 Assembler 中解包为基础标量，禁止 domain 类型直接序列化暴露）。
  - **用例编排**：`CreateOrderCommand / OrderPageQuery`（应用层入参，优先使用 Java Record）。
  - **领域层**：`Order`（聚合根）、`OrderItem`（聚合内实体）、`Money / OrderStatus`（值对象）。
  - **持久化边界**：`OrderDO / OrderItemDO` ➔ `OrderConverter` ➔ `OrderMapper`（MyBatis 数据持久化对象，严密封闭在 `infrastructure/repository/` 内部）。
- **禁止反射拷贝**：严禁使用 `BeanUtils.copyProperties`，所有对象转换必须在 Assembler / Converter 中显式赋值。

### 3. Java 核心零容忍规则
- 条件分支 `if`/`else`/`for`/`while` 即使单行也必须使用大括号 `{}`。
- `switch` 必须包含 `default` 分支。
- 严禁魔法值，所有状态、业务类型必须使用预定义枚举或常量。
- POJO 属性必须使用包装类型（如 `Long`、`Integer`），局部变量推荐基本类型。
- 判空防 NPE：字符串使用 `org.apache.commons.lang3.StringUtils`（严禁使用 Spring StringUtils 或手写 `== null || isEmpty()`）；集合判空使用 `CollectionUtils`。
- `@Transactional` 必须显式声明 `rollbackFor = Exception.class`。
- 日志严禁拼接字符串，必须使用占位符 `{}`；捕获异常并抛出或记录日志时，必须保留原始异常 `e` 根因。
- 严禁使用 `System.out.println` 或 `e.printStackTrace()`。

---

## 三、四步验证流程（每次修改必跑）

1. **编译验证**：`mvn compile test-compile` 确认语法与注解处理器零告警。
2. **测试与门禁执行**：`mvn clean verify -DskipTests=false` 完整跑通所有单元测试、集成测试、Spotless、PMD、SpotBugs、JaCoCo 与 ArchUnit。
3. **代码审查**：内置 `/code-review` 审查改动（安全变更加 `/security-review`），核查是否违背 DDD 六条宪法红线。
4. **策略验证**：核验测试覆盖充分，`git status` 确认无遗留废弃文件。

---

## 四、高频踩坑清单（动手前必读）

- **坑 1**：在 `domain/` 下的类上顺手添加了 `@Component`、`@Service`、`@Autowired`、`@Getter` 或 Spring 的注解。
  - *后果*：`ArchitectureTest.domainShouldBeFrameworkFree` 直接红灯阻断构建。
  - *解法*：保持纯 Java，通过构造方法传参，在 `infrastructure/config/DomainConfig` 声明 Bean。
- **坑 2**：在 Controller 或 ApplicationService 中直接引入并操作 `OrderDO`。
  - *后果*：破坏了 DDD 封装界限，DO 泄露导致领域模型退化。
  - *解法*：外层只处理领域聚合 `Order`，DO 仅允许出现在 `infrastructure/repository/`。
- **坑 3**：修改了代码格式导致 Spotless 拦截失败。
  - *解法*：直接执行 `mvn spotless:apply` 一键自动修复格式。
- **坑 4**：Spring Boot 4 中 `starter-web` 已更名为 `starter-webmvc`，编写测试时使用 `spring-boot-starter-webmvc-test`。
