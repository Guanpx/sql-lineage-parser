CREATE TABLE dw.top_users AS
SELECT u.id AS user_id
    , u.name AS user_name
    , SUM(o.amount) AS total_amount
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id, u.name
