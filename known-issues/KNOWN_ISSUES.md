# 已知问题清单（Known Issues）

> 本文档记录代码审查中发现、但**尚未修复**的问题（P2 / P3）。
> P1 级两个正确性缺陷已在 2026-07-07 修复（见 UPDATE_DEV.md），不在此列。
>
> 每个问题都配有 `known-issues/sql/` 下的验证 SQL，可通过 `SqlLineageParser` 对应入口复现。
> 「实际输出」为当前代码（含 P1 修复后）的真实运行结果。

## 问题索引

| 编号 | 严重度 | 问题 | 验证 SQL |
|------|--------|------|----------|
| P2-1 | P2 | 来源列不去重 | `sql/issue-p2-01-duplicate-source-columns.sql` |
| P2-2 | P2 | TreeNode 兄弟节点 id 相同 + subtreeSize 语义错误 | `sql/issue-p2-02-treenode-id-subtreesize.sql` |
| P2-3 | P2 | TreeNode.getChildren 注释称不可变但返回可变 list | 无 SQL（API 契约问题） |
| P3-1 | P3 | 源列表名含 schema，与目标表拆分表示不一致 | `sql/issue-p3-01-table-name-inconsistency.sql` |
| P3-2 | P3 | SELECT * 目标列名回退为字面量 "*" | `sql/issue-p3-02-select-star-target-name.sql` |
| P3-3 | P3 | 单表裸列推断误归属（无别名子查询未登记） | `sql/issue-p3-03-single-table-bare-column-misattribution.sql` |
| P3-4 | P3 | 递归 CTE 未支持 | `sql/issue-p3-04-recursive-cte.sql` |
| P3-5 | P3 | CTE / 子查询重复解析开销（性能） | 无 SQL（代码级） |

---

## P2-1 来源列不去重

- **位置**: `core/parser/sql/expr/ExprParseContext.java#recordSource`
- **验证 SQL**: `sql/issue-p2-01-duplicate-source-columns.sql`
- **现象**: 收集来源列时不做去重，同一列被多个子表达式引用时会重复出现。

```sql
SELECT SUM(sal) OVER (PARTITION BY sal) AS c FROM emp
```

实际输出：

```
OUT alias=c
    src table=emp name=sal
    src table=emp name=sal   <-- 重复
```

- **触发面**: 窗口聚合（参数列与 PARTITION/ORDER BY 同列）、二元运算 `a+a`、多分支 CASE 引用同列等。
- **影响**: 血缘边重复，下游按来源列计数或建图时产生重复边。
- **建议**: 在 `recordSource` 处按 `(tableName, name, constant)` 去重（常量可保留或单独处理）。

## P2-2 TreeNode 兄弟节点 id 相同 + subtreeSize 语义错误

- **位置**: `sqllineageparser/model/TreeNode.java#addChild`（id 行、subtreeSize 行）
- **验证 SQL**: `sql/issue-p2-02-treenode-id-subtreesize.sql`
- **现象**:
  - `childNode.id = this.id + 1` 使同一父节点下所有子节点 id 相同（均为 `parent.id + 1`）；
  - `this.subtreeSize++` 只统计直接子节点，孙子节点不向上传播，与「子树大小」命名/注释不符。

```sql
SELECT a.id, a.name, a.age FROM users a
```

实际输出：

```
OUT alias=null id=1
OUT alias=null id=1   <-- id 全部相同
OUT alias=null id=1
root.subtreeSize=3    <-- 扁平树恰好正确；嵌套树只统计直接子节点会偏小
```

- **影响**: 目前血缘树按下标消费，id / subtreeSize 未参与正确性，属**潜在**缺陷；一旦持久化/建图依赖 id 唯一性即会踩坑。
- **建议**: 用全局自增或路径生成唯一 id；`subtreeSize` 递归累加或在遍历时计算。

## P2-3 getChildren 注释称不可变但返回可变 list

- **位置**: `sqllineageparser/model/TreeNode.java#getChildren`
- **验证**: 无 SQL（API 契约问题，代码审查即可确认）
- **现象**: 注释写「不可变视图」，实际直接返回原始 `children` 引用；调用方可 `getChildren().add(...)` 绕过 `addChild`，破坏 `subtreeSize` 计数。
- **建议**: 返回 `Collections.unmodifiableList(children)`，或修正注释并保留 `getChildrenMutable()` 作为唯一可变入口。

---

## P3-1 源列表名含 schema，与目标表拆分表示不一致

- **位置**: `core/parser/sql/expr/ExprParseContext.java#collectTableAlias`（使用 `getExpr().toString()`）
- **验证 SQL**: `sql/issue-p3-01-table-name-inconsistency.sql`
- **现象**: 源列 `tableName` 取自 `SQLExprTableSource.getExpr().toString()`，对 `db2.src` 得到内嵌 schema 的字符串；而 INSERT/CTAS/CREATE VIEW 的目标表把 schema/table 拆开存。

```sql
INSERT INTO db.dst SELECT a.id FROM db2.src a
```

实际输出：

```
targetSchema=db  targetTable=dst      <-- 目标：拆分
target[0]=id
    src table=db2.src name=id          <-- 源：schema 内嵌
```

- **影响**: 同一模型中源、目标表标识形态不统一，消费方比对表身份时会错配。
- **建议**: 统一表标识表示（源列也拆分 schema/table，或目标表也用全限定字符串）。

## P3-2 SELECT * 目标列名回退为字面量 "*"

- **位置**: `sqllineageparser/model/DmlLineageInfo.java#getTargetColumnAt` / `SqlLineageParser.java#visibleColumnName`
- **验证 SQL**: `sql/issue-p3-02-select-star-target-name.sql`
- **现象**: `SELECT *` 无法展开为具体列，目标列名回退成字面量 `"*"`。

```sql
CREATE VIEW dw.v AS SELECT * FROM users
```

实际输出：

```
targetSchema=dw  targetTable=v
target[0]=*        <-- 把 "*" 当成了列名
```

- **影响**: `*` 需元数据才能展开；当前把 `*` 当列名，血缘不准确。另外含空格/括号的表达式列会返回 null 而被静默丢弃（不注册进虚拟表，外层下钻失败）。
- **建议**: 识别 `SQLAllColumnExpr` 并结合元数据展开，或至少标记为通配符占位而非列名。

## P3-3 单表裸列推断误归属（无别名子查询未登记）

- **位置**: `core/parser/sql/expr/ExprParseContext.java#inferSingleTableName` + `#collectTableAlias`
- **验证 SQL**: `sql/issue-p3-03-single-table-bare-column-misattribution.sql`
- **现象**: `collectTableAlias` 只在子查询**有别名**时登记；无别名子查询不进 `aliasToTableMap`，导致 `map.size()==1` 触发单表裸列推断，把裸列归给唯一登记的表。

```sql
SELECT x FROM t a JOIN (SELECT id FROM u) ON a.id = 1
```

实际输出：

```
OUT alias=null
    src table=t name=x    <-- x 被误归属到 t（实际来源不明确）
```

- **影响**: 多表场景下裸列错误归属，血缘不准确。
- **建议**: 存在多个表源（含未登记子查询）时不触发单表推断；或先补全所有表源（含无别名子查询）的登记再判断。

## P3-4 递归 CTE 未支持

- **位置**: `core/parser/SqlLineageParser.java#collectCteSources`
- **验证 SQL**: `sql/issue-p3-04-recursive-cte.sql`
- **现象**: `collectCteSources` 在把 CTE 放入结果 map **之前**解析其子查询，递归体内对自身的引用会被当作真实表。

```sql
WITH RECURSIVE r AS (
    SELECT id FROM base UNION ALL SELECT id FROM r
) SELECT id FROM r
```

实际输出：

```
OUT alias=null
    src table=base name=id
    src table=r name=id    <-- 自引用泄漏为虚拟名 r（血缘不完整）
```

- **影响**: 递归血缘不完整（当前不会死循环）。
- **建议**: 识别递归 CTE，将自引用视为占位并只保留锚点分支的真实来源。

## P3-5 CTE / 子查询重复解析开销（性能）

- **位置**: `core/parser/SqlLineageParser.java#extractColumnSources` / `#registerNestedSubqueries`
- **验证**: 无 SQL（代码级，深层嵌套时可观察耗时增长）
- **现象**: `extractColumnSources` 每层都重算 `collectCteSources(withClause)`，`registerNestedSubqueries` 又让同一子查询被解析多次，深层嵌套 CTE/子查询下解析次数近似指数增长。
- **影响**: 大型嵌套 SQL 解析变慢（非正确性问题）。
- **建议**: 缓存已解析的 CTE / 子查询列血缘（按 AST 节点或名称记忆化），避免重复解析。
