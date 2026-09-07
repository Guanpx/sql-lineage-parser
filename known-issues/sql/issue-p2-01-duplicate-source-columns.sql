-- 问题 P2-1: 来源列不去重
-- 入口: SqlLineageParser.parserSingleSelectSql
-- 现象: emp.sal 会被收集两次（一次来自 SUM 参数，一次来自 PARTITION BY）
-- 期望: 同一 (tableName, name, constant) 的来源列只出现一次
SELECT SUM(sal) OVER (PARTITION BY sal) AS c
FROM emp
