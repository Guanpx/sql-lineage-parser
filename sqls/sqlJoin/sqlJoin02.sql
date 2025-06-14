-- 左连接，指定具体字段，右表是带查询的子表
SELECT t1.id, t1.name, t1.value1, t1.dt,
       t2.id AS t2_id, t2.description, t2.value2, t2.dt AS t2_dt
FROM table1 t1
LEFT JOIN (
  SELECT id, description, value2, dt
  FROM table2
  WHERE dt >= '2023-01-01'
) t2 ON t1.id = t2.id;