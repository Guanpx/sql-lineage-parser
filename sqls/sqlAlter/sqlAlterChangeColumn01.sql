ALTER TABLE dw.orders
CHANGE COLUMN amount order_amount DECIMAL(18, 2) COMMENT '订单金额'
