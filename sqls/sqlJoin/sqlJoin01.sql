SELECT c.customer_id, c.customer_name, c.customer_email, o.order_id, o.order_date, o.order_amount
FROM customers c
LEFT JOIN orders o ON c.customer_id = o.customer_id;