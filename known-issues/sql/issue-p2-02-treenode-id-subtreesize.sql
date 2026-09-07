-- 问题 P2-2: TreeNode 兄弟节点 id 相同 + subtreeSize 语义错误
-- 入口: SqlLineageParser.parserSingleSelectSql，观察每个输出列 TreeNode.getId()
-- 现象: 三个输出列 id 全部为 1（均等于 root.id + 1）；subtreeSize 只统计直接子节点
-- 期望: 兄弟节点 id 应互不相同；subtreeSize 应为整棵子树节点数
SELECT a.id, a.name, a.age
FROM users a
