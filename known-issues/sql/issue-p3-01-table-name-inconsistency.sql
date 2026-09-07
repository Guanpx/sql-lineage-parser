-- 问题 P3-1: 源列表名含 schema，与目标表 schema/table 拆分表示不一致
-- 入口: SqlLineageParser.parserDmlSql
-- 现象: 目标 targetSchema=db / targetTable=dst（拆分）；
--       而源列 tableName="db2.src"（schema 内嵌在字符串里）
-- 期望: 源列与目标表使用统一的表标识表示（要么都拆分，要么都全限定）
INSERT INTO db.dst
SELECT a.id
FROM db2.src a
