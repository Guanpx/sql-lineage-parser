-- 问题 P3-3: 单表裸列推断边界 —— 无别名子查询未登记导致裸列被误归属
-- 入口: SqlLineageParser.parserSingleSelectSql
-- 现象: FROM 含「一个带别名的表 t a」+「一个无别名子查询」，
--       无别名子查询不进 aliasToTableMap，导致 map.size()==1，
--       裸列 x 被错误归属到 t（实际来源不明确）
-- 期望: 存在多个表源（含未登记子查询）时不应触发单表裸列推断
SELECT x
FROM t a
JOIN (SELECT id FROM u) ON a.id = 1
