-- =============================================================================
-- ddd-scaffold 全量基线（Flyway 单一 baseline）
-- 取代历史上的 doc/sql/init.sql 手工脚本与 spring.sql.init 自动执行，schema 变更只走 Flyway。
-- 设计约定（详见 doc/adr/ADR-0006 / ADR-0007）：
--   1. 主键 UUID v7 字符串，应用侧生成，禁用自增主键（自增会逼出仓储回填这种封装破洞）；
--   2. 状态列 VARCHAR + CHECK，与领域枚举共用同一份取值清单，禁 SMALLINT 数字码；
--   3. 时间列统一 gmt_create / gmt_modified，DB 默认值兜底，应用侧显式赋值优先；
--   4. 已规划能力（审计、反馈、归档等）在建表时一次到位，不留 V(n) 追补迁移；
--   5. 订单为状态机模型，终态归档而非删除，主档不设删除位；
--   6. DDL 保持 H2(MySQL 模式) 与 MySQL 双兼容，不写引擎/字符集/表级 COMMENT 等方言特有子句。
-- =============================================================================

CREATE TABLE t_order
(
    id              VARCHAR(36)    NOT NULL COMMENT '主键(UUID v7)',
    order_no        VARCHAR(32)    NOT NULL COMMENT '订单号',
    user_id         BIGINT         NOT NULL COMMENT '用户ID',
    total_amount    DECIMAL(12, 2) NOT NULL COMMENT '商品总额',
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
    pay_amount      DECIMAL(12, 2) NOT NULL COMMENT '应付金额',
    status          VARCHAR(16)    NOT NULL DEFAULT 'UNPAID' COMMENT '状态:UNPAID/PAID/CANCELLED',
    remark          VARCHAR(255)   DEFAULT NULL COMMENT '备注',
    gmt_create      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_order_no (order_no),
    KEY idx_order_user_id (user_id),
    CONSTRAINT ck_order_status CHECK (status IN ('UNPAID', 'PAID', 'CANCELLED'))
);

CREATE TABLE t_order_item
(
    id           VARCHAR(36)    NOT NULL COMMENT '主键(UUID v7)',
    order_id     VARCHAR(36)    NOT NULL COMMENT '订单ID',
    product_id   BIGINT         NOT NULL COMMENT '商品ID',
    product_name VARCHAR(128)   NOT NULL COMMENT '商品名(下单快照)',
    unit_price   DECIMAL(12, 2) NOT NULL COMMENT '单价(下单快照)',
    quantity     INT            NOT NULL COMMENT '数量',
    line_amount  DECIMAL(12, 2) NOT NULL COMMENT '行小计',
    gmt_create   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_order_item_order_id (order_id)
);
