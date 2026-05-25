INSERT OVERWRITE TABLE dw.daily_order PARTITION (dt='2026-05-20')
SELECT o.order_id
    , o.user_id
    , o.amount
    , o.status
FROM ods.orders o
WHERE o.order_date = '2026-05-20'
