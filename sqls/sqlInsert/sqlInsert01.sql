INSERT INTO dw.user_summary (user_id, user_name, total_amount)
SELECT u.id, u.name, SUM(o.amount) AS total
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id, u.name
