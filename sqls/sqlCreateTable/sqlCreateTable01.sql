CREATE TABLE IF NOT EXISTS dw.user_profile (
    user_id BIGINT COMMENT '用户ID',
    user_name STRING COMMENT '用户名',
    amount DECIMAL(18, 2) DEFAULT 0 COMMENT '账户金额'
)
COMMENT '用户画像表'
PARTITIONED BY (
    dt STRING COMMENT '业务日期',
    region STRING COMMENT '地域'
)
