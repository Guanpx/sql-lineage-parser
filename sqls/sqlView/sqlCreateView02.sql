CREATE VIEW dw.v_order_summary (uid, total) AS
SELECT a.user_id, SUM(a.amount)
FROM orders a
GROUP BY a.user_id
