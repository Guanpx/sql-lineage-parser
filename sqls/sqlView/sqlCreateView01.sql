CREATE VIEW dw.v_active_user AS
SELECT u.id AS user_id
    , u.name AS user_name
    , SUM(o.amount) AS total_amount
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE u.status = 'ACTIVE'
GROUP BY u.id, u.name
