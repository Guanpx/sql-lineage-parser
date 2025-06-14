SELECT 'order' AS source, order_id, customer_id, order_date, total_amount
FROM orders
WHERE order_date = '2023-12-25'
UNION
SELECT 'invoice' AS source, invoice_id, customer_id, invoice_date, amount
FROM invoices
WHERE invoice_date = '2023-12-25';