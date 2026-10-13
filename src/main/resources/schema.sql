-- H2（MySQL 兼容模式）启动初始化脚本，与 doc/sql/init.sql 保持同构
-- 订单为状态机模型，不做物理/逻辑删除，终态数据按归档策略处理
CREATE TABLE IF NOT EXISTS t_order
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    order_no        VARCHAR(32)    NOT NULL COMMENT '订单号',
    user_id         BIGINT         NOT NULL COMMENT '用户ID',
    total_amount    DECIMAL(12, 2) NOT NULL COMMENT '商品总额',
    discount_amount DECIMAL(12, 2) NOT NULL COMMENT '优惠金额',
    pay_amount      DECIMAL(12, 2) NOT NULL COMMENT '应付金额',
    status          TINYINT        NOT NULL COMMENT '状态:0待支付 1已支付 2已取消',
    remark          VARCHAR(255)   DEFAULT NULL COMMENT '备注',
    gmt_create      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间'
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_order_order_no ON t_order (order_no);
CREATE INDEX IF NOT EXISTS idx_order_user_id ON t_order (user_id);

CREATE TABLE IF NOT EXISTS t_order_item
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    order_id     BIGINT         NOT NULL COMMENT '订单ID',
    product_id   BIGINT         NOT NULL COMMENT '商品ID',
    product_name VARCHAR(128)   NOT NULL COMMENT '商品名(下单快照)',
    unit_price   DECIMAL(12, 2) NOT NULL COMMENT '单价(下单快照)',
    quantity     INT            NOT NULL COMMENT '数量',
    line_amount  DECIMAL(12, 2) NOT NULL COMMENT '行小计',
    gmt_create   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间'
);
CREATE INDEX IF NOT EXISTS idx_order_item_order_id ON t_order_item (order_id);
