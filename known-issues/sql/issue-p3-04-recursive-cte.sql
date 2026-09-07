-- 问题 P3-4: 递归 CTE 未支持
-- 入口: SqlLineageParser.parserSingleSelectSql
-- 现象: collectCteSources 在把 r 放入结果 map 之前解析其子查询，
--       故递归体内的 FROM r 被当作真实表，来源出现虚拟名 r.id（血缘不完整）
-- 期望: 识别递归 CTE，自引用不应作为真实表泄漏；不会死循环（当前也不会）
WITH RECURSIVE r AS (
    SELECT id FROM base
    UNION ALL
    SELECT id FROM r
)
SELECT id FROM r
