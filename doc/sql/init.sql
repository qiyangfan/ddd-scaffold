-- =============================================
-- ddd-scaffold MySQL 建表脚本
-- 遵循手册建表规约：必备三字段 gmt_create / gmt_modified / deleted（订单为状态机模型，主档不做删除位）
-- =============================================
CREATE DATABASE IF NOT EXISTS ddd_scaffold DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE ddd_scaffold;

CREATE TABLE IF NOT EXISTS t_order
(
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no        VARCHAR(32)    NOT NULL COMMENT '订单号',
    user_id         BIGINT         NOT NULL COMMENT '用户ID',
    total_amount    DECIMAL(12, 2) NOT NULL COMMENT '商品总额',
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
    pay_amount      DECIMAL(12, 2) NOT NULL COMMENT '应付金额',
    status          TINYINT        NOT NULL DEFAULT 0 COMMENT '状态:0待支付 1已支付 2已取消',
    remark          VARCHAR(255)   DEFAULT NULL COMMENT '备注',
    gmt_create      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_order_no (order_no),
    KEY idx_order_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '订单表';

CREATE TABLE IF NOT EXISTS t_order_item
(
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id     BIGINT         NOT NULL COMMENT '订单ID',
    product_id   BIGINT         NOT NULL COMMENT '商品ID',
    product_name VARCHAR(128)   NOT NULL COMMENT '商品名(下单快照)',
    unit_price   DECIMAL(12, 2) NOT NULL COMMENT '单价(下单快照)',
    quantity     INT            NOT NULL COMMENT '数量',
    line_amount  DECIMAL(12, 2) NOT NULL COMMENT '行小计',
    gmt_create   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_order_item_order_id (order_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细表';
