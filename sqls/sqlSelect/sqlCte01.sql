WITH active_users AS (
    SELECT user_id, name, last_login
    FROM users
    WHERE status = 'ACTIVE'
), top_orders AS (
    SELECT user_id, SUM(amount) AS total_amount
    FROM orders
    WHERE order_date >= '2026-01-01'
    GROUP BY user_id
)
SELECT au.user_id
    , au.name AS user_name
    , au.last_login
    , to_orders.total_amount AS spending
FROM active_users au
LEFT JOIN top_orders to_orders ON au.user_id = to_orders.user_id
