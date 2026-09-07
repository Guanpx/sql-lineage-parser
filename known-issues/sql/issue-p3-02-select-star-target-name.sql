-- 问题 P3-2: SELECT * 场景目标列名回退为字面量 "*"
-- 入口: SqlLineageParser.parserDmlSql / parserCreateViewSql
-- 现象: getTargetColumnAt(0) 返回 "*"，且 * 无法展开为具体列
-- 期望: 识别 SELECT * 并结合元数据展开，或至少标记为通配符而非把 "*" 当列名
CREATE VIEW dw.v AS
SELECT *
FROM users
