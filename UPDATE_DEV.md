# UPDATE_DEV.md - 变更记录

## 2026-08-27 移除 TreeNode，血缘输出改为 List&lt;ColumnNode&gt;

### 背景

代码审查确认 TreeNode 未发挥树的作用：血缘"树"永远只有虚拟 root + 输出列两层，来源列实际存于 `ColumnNode.sourceColumns` 扁平列表；全代码库仅使用其 13 个公开 API 中的 `getValue`/`getChildren`；6 个表源解析器 `process()` 的 `parent` 参数无任何实现使用。经评估后实施移除。

### API 变更（breaking）

| 项 | 变更前 | 变更后 |
|----|--------|--------|
| `parserSingleSelectSql(sql)` | `TreeNode<ColumnNode>` | `List<ColumnNode>` |
| `parseSelect(SQLSelect)` | `TreeNode<ColumnNode>` | `List<ColumnNode>` |
| `DmlLineageInfo.sourceLineage` | `TreeNode<ColumnNode>`（getSourceLineage/setSourceLineage） | `List<ColumnNode> outputColumns`（getOutputColumns/setOutputColumns） |
| `BaseTableSourceParser.process` | `process(dbType, sequence, parent, tableSource)` | `process(dbType, sequence, tableSource)`（去掉伪 parent 参数） |

消费方式简化：`tree.getChildren().get(i).getValue()` → `list.get(i)`。

### 修改文件

删除：
```
src/main/java/com/magic/sqllineageparser/model/TreeNode.java
```

修改（main）：
```
core/parser/SqlLineageParser.java                    # parseSelect/parseQueryBlock/parseUnionQuery/extractColumnSources 全部改 List
sqllineageparser/model/DmlLineageInfo.java           # sourceLineage → outputColumns；getOutputColumnCount/getTargetColumnAt 适配
core/parser/sql/dml/SqlInsertParser.java             # setOutputColumns
core/parser/sql/dml/SqlCreateTableAsParser.java      # setOutputColumns；删除悬空重载 collectColumnDefinitions(DmlLineageInfo)
core/parser/sql/ddl/SqlCreateViewParser.java         # setOutputColumns
core/parser/sql/table/BaseTableSourceParser.java     # 接口去 parent 参数
core/parser/sql/table/Sql*TableSourceParser.java     # 6 个实现同步去 parent / 清理无用 import
persistence/repository/NebulaLineageRepository.java  # 清理残留 TreeNode import
```

修改（test/debug）：
```
parser/SqlLineageParserTest.java                     # 32 处机械替换（getChildren/getValue/getSourceLineage）
parser/sql/table/TableSourceParserTest.java          # 4 处调用适配新签名
debug/DebugHelper.java                               # printLineageTree → printLineageColumns(List<ColumnNode>)
debug/SqlLineageParserDebug.java / InsertSqlLineageParserDebug.java / CreateSqlLineageParserDebug.java
```

文档：
```
known-issues/KNOWN_ISSUES.md                         # P2-2 / P2-3 标记为已消除（随 TreeNode 删除）
known-issues/sql/issue-p2-02-treenode-id-subtreesize.sql  # 加历史记录注释
README.md                                            # API 表 / 示例代码 / 结构示意 / 数据模型表同步；测试数 89
```

### 验证

- `mvn test` BUILD SUCCESS，89 用例全绿（重构前后数量不变，无行为变化）
- 端到端探针比对：SELECT 三列血缘、INSERT 目标列对齐、UNION 合并输出与重构前完全一致
- 全库 `grep TreeNode` 无残留（仅 ColumnNode.tableTreeNodeId 字段名，属历史字段未动）

### 顺带收益

- 直接消除 KNOWN_ISSUES 的 P2-2（兄弟节点 id 相同 / subtreeSize 语义错）与 P2-3（getChildren 可变性）两个问题
- 删除 SqlCreateTableAsParser 中从未被调用的悬空重载方法
- 表源解析器接口去掉无用的 parent 参数，语义更诚实

---

## 2026-08-27 代码审查修复 P1 缺陷 + 归档 P2/P3

### 概述

对核心血缘解析链路做代码审查，发现并修复 2 个 P1 正确性缺陷；其余 P2/P3 问题归档到 `known-issues/` 并配套验证 SQL。测试从 87 增至 89（新增 2 个 P1 回归用例），`mvn test` 全绿。

### P1 修复（本次已改代码）

| 编号 | 缺陷 | 修复 | 验证 |
|------|------|------|------|
| P1-1 | CASE 表达式丢列：简单 CASE 操作数（`CASE dept WHEN...` 的 dept）与 WHEN 条件列（`WHEN status='X'` 的 status）从未被收集为来源 | `SqlCaseExprParser` 补充解析 `expr.getValueExpr()`（操作数）与 `item.getConditionExpr()`（条件） | `CaseWhenTest.testSimpleCaseOperandCollected` / `testSearchedCaseConditionCollected` |
| P1-2 | 虚拟表下钻未命中列时泄漏别名：`WITH t AS(...) SELECT t.nonexist` 把 CTE 名 t 当真实表写入血缘 | `ExprParseContext.drillDownVirtual` 命中虚拟表但列名无法下钻时记录为未解析列（tableName=null），不再回落到别名 | 探针验证 `t.nonexist` 来源变为 table=null |

### 修改文件

```
src/main/java/com/magic/core/parser/sql/expr/SqlCaseExprParser.java   # P1-1
src/main/java/com/magic/core/parser/sql/expr/ExprParseContext.java     # P1-2
src/test/java/com/magic/core/parser/SqlLineageParserTest.java          # 新增 2 个 CASE 回归用例
```

### 新增文件（P2/P3 问题归档）

```
known-issues/KNOWN_ISSUES.md                                          # 记录 8 个未修复问题（P2×3 + P3×5，含实际输出与建议）
known-issues/sql/issue-p2-01-duplicate-source-columns.sql
known-issues/sql/issue-p2-02-treenode-id-subtreesize.sql
known-issues/sql/issue-p3-01-table-name-inconsistency.sql
known-issues/sql/issue-p3-02-select-star-target-name.sql
known-issues/sql/issue-p3-03-single-table-bare-column-misattribution.sql
known-issues/sql/issue-p3-04-recursive-cte.sql
```

### 说明

- P2/P3 问题（含 `TreeNode.getChildren` 可变性、CTE 重复解析性能等无 SQL 复现项）均在 `KNOWN_ISSUES.md` 列出位置、现象、实际输出与修复建议，待后续排期。
- 本次为审查修复，未改变对外 API；`mvn test` BUILD SUCCESS，89 用例全绿。

---

## 2026-08-26 完成 v0.4.0 可开发余项（CREATE TABLE 纯 DDL / ALTER 增强）

### 操作背景

按用户本轮决策推进版本余项：

- v0.3.0 的 MERGE INTO 与多语句脚本解析不继续开发；
- v0.4.0 的 DROP TABLE / VIEW 识别不开发；
- 其余余项全部开发，即纯 CREATE TABLE 字段元信息解析与 ALTER TABLE MODIFY / CHANGE COLUMN。

### 新增能力

| 能力 | 关键实现 | 测试 |
|------|----------|------|
| 纯 CREATE TABLE 元信息 | 新增 `CreateTableInfo` / `TableColumnMeta` / `SqlCreateTableParser`，提取表名、表注释、字段、类型、注释、默认值、主键标记与分区字段 | `CreateTableDdlTest` 3 用例 |
| 纯建表入口 | `SqlLineageParser.parserCreateTableDdlSql(sql)`；CTAS 仍走 `parserCreateTableSql` 并保持非 CTAS 返回 null | `testCreateTableDdlRejectsCtas` |
| ALTER CHANGE COLUMN | Druid `SQLAlterTableAlterColumn` 映射为 `AlterColumnChange.Action.MODIFY`，保留原列名、新列名、类型与注释 | `testAlterChangeColumn` |
| ALTER MODIFY COLUMN | Druid Hive 方言无独立 MODIFY 分支；仅当 Hive 解析失败且 MySQL AST 明确产出 `MySqlAlterTableModifyColumn` 时窄范围回退解析 | `testAlterModifyColumn` |

### 关键文件

新增：
```
src/main/java/com/magic/sqllineageparser/model/CreateTableInfo.java
src/main/java/com/magic/sqllineageparser/model/TableColumnMeta.java
src/main/java/com/magic/core/parser/sql/ddl/SqlCreateTableParser.java
sqls/sqlCreateTable/sqlCreateTable01.sql
sqls/sqlAlter/sqlAlterChangeColumn01.sql
sqls/sqlAlter/sqlAlterModifyColumn01.sql
```

修改：
```
src/main/java/com/magic/core/parser/SqlLineageParser.java
src/main/java/com/magic/core/parser/sql/alter/SqlAlterTableParser.java
src/main/java/com/magic/sqllineageparser/model/AlterColumnChange.java
src/test/java/com/magic/core/parser/SqlLineageParserTest.java
src/test/java/com/magic/core/util/SqlFileReader.java
README.md
RELEASE.md
```

### 实现要点

- **模型边界**：纯 CREATE TABLE 是结构元信息，返回 `CreateTableInfo`；CTAS / CREATE VIEW 是带源 SELECT 的数据血缘，继续返回 `DmlLineageInfo`，不混用模型。
- **旧入口兼容**：`parserCreateTableSql` 继续只解析 CTAS，纯 DDL 返回 null，既有行为不变。
- **CHANGE 语义**：Hive `CHANGE COLUMN old new TYPE COMMENT` 在 Druid 中解析为 `SQLAlterTableAlterColumn`，`originColumn` 为 old，`column` 为新定义；输出统一为 `MODIFY` 动作。
- **MODIFY 兼容层**：先按 Hive 方言解析；仅当失败后 MySQL 方言产出 `MySqlAlterTableModifyColumn` 才回退，其他非法 SQL 仍抛出原 Hive 解析异常。

### 验证

- `mvn test` BUILD SUCCESS
- 87 个 @Test 全部通过（此前 82 个 + 新增 5 个）
- 既有 SELECT / DML / CTAS / CREATE VIEW 用例无回归

---

## 2026-07-07 新增 CREATE VIEW 解析（v0.4.0 DDL 首项）

### 概述

按用户选定的开发路线推进 v0.4.0 DDL 支持的首项能力：解析 `CREATE VIEW ... AS SELECT`，将视图定义（本质是一段 SELECT）的列血缘挂到视图目标表上。结构与 CTAS 高度同构，深度复用 `SqlLineageParser.parseSelect`，无新模型字段。所有 82 个单元测试通过（v0.3.0 的 78 + 新增 4）。

### 新增能力

| 能力 | 关键实现 | 测试 |
|------|----------|------|
| CREATE VIEW ... AS SELECT 解析 | 新增 `SqlCreateViewParser.parse(SQLCreateViewStatement)`，提取视图名 + 显式列覆盖 + 源 SELECT 血缘 | `CreateViewTest` 4 用例 |
| 统一 DML 入口扩展 | `parserDmlSql` 新增 `SQLCreateViewStatement` 分支 | `testUnifiedDmlEntryCreateView` |
| 独立入口 | `SqlLineageParser.parserCreateViewSql(sql)` | `testCreateViewBasic` / `testCreateViewExplicitColumns` |
| 视图显式列覆盖 | 从 `SQLCreateViewStatement.getColumns()`（List<SQLTableElement>）提取 SQLColumnDefinition 名 | `testCreateViewExplicitColumns` 验证 (uid, total) 按位置对齐 |

### 关键文件

新增：
```
src/main/java/com/magic/core/parser/sql/ddl/SqlCreateViewParser.java   # CREATE VIEW 解析器（ddl 子包）
sqls/sqlView/sqlCreateView01.sql                                       # JOIN + 聚合视图用例
sqls/sqlView/sqlCreateView02.sql                                       # 显式列覆盖用例
```

修改：
```
src/main/java/com/magic/sqllineageparser/model/DmlOperation.java        # 新增 CREATE_VIEW 枚举值
src/main/java/com/magic/core/parser/SqlLineageParser.java               # 新增 parserCreateViewSql + parserDmlSql 扩展
src/test/java/com/magic/core/util/SqlFileReader.java                    # 新增 readViewSql
src/test/java/com/magic/core/parser/SqlLineageParserTest.java           # 新增 CreateViewTest 4 用例
README.md / RELEASE.md                                                  # 同步 v0.4.0 CREATE VIEW 状态
```

### 实现要点

- **AST 形态确认**：Druid 1.2.20 的 `SQLCreateViewStatement` 通过 `getTableSource()` 返回 `SQLExprTableSource`（视图名）、`getSubQuery()` 返回 `SQLSelect`（AS 子查询）、`getColumns()` 返回 `List<SQLTableElement>`（显式列覆盖，元素为 `SQLColumnDefinition`）。
- **DDL vs DML 包归属**：CREATE VIEW 语义上是 DDL，因此新解析器放在新 `ddl` 子包下；但返回类型复用 `DmlLineageInfo` + `DmlOperation.CREATE_VIEW`，避免引入字段重叠的新模型。
- **目标列对齐**：与 CTAS 同策略——显式列覆盖优先；缺失时通过 `DmlLineageInfo.getTargetColumnAt(i)` 三级兜底（显式列 → SELECT alias → expression 列名）。
- **统一入口语义扩展**：`parserDmlSql` 现成为「任何带目标表 + 源 SELECT 血缘」的统一入口（INSERT / CTAS / CREATE VIEW），方法 Javadoc 已同步更新。

### 验证

- `mvn test` BUILD SUCCESS，82 个 @Test 全绿
- 既有 INSERT / CTAS 用例回归无变化（4 + 3 全过）

---

## 2026-07-07 文档对齐（v0.3.0 当前状态）

### 操作背景

代码已推进到 v0.3.0（DML 已交付，78 个测试用例通过），但 README / RELEASE 文档存在滞后：

- README 「v0.2.0 功能详情」段写 71 用例，与「开发状态」段 78 用例不一致
- RELEASE 「二、当前项目状态评估」仍把 UNION / CTE / ALTER 标为开发中，把 INSERT / CTAS / 窗口函数 / LATERAL VIEW 标为缺失，与代码严重脱节
- RELEASE 「测试用例分类」表把 INSERT 标为 ❌ 待添加，已过时

本次仅做文档对齐，**不改代码**。

### 修改文件

```
README.md
  - v0.2.0 测试条目：71 → 78，用例范围补 INSERT / CTAS

RELEASE.md
  - 第二节「当前项目状态评估」整段重写：
      · 已完成项补充 UNION / CTE / 嵌套子查询 / 窗口函数 / LATERAL VIEW / CAST / ALTER / INSERT / CTAS / 统一 DML 入口
      · 开发中项改为 MERGE INTO / 多语句脚本
      · 缺失能力表移除已交付项，新增 CREATE VIEW / CREATE TABLE DDL / DROP / ALTER MODIFY
  - 测试用例分类表：INSERT 改 ✅，新增 CTAS / 窗口函数 / LATERAL VIEW / ALTER 行；总数标注 78
  - v0.2.0 段保留 71（历史准确）并加注 v0.3.0 后增至 78
  - 文档版本号 v1.4 → v1.5，更新时间 → 2026-07-07
```

### 验证

- `grep -rE "@Test" src/test` 实测 78 个用例，三处文档统一为 78
- 代码未变更，无需运行测试

---

## 2026-05-20 启动 v0.3.0 DML 支持

### 概述

按 v0.3.0 版本规划新增 DML 解析能力：INSERT INTO / INSERT OVERWRITE / CREATE TABLE AS SELECT (CTAS)，建立从源 SELECT 到目标表/目标列的完整血缘。共新增 6 个 Java 源文件与 3 个 SQL 用例。所有 78 个单元测试通过（v0.2.0 的 71 + 新增 7）。

### 新增能力

| 能力 | 关键实现 | 测试 |
|------|----------|------|
| INSERT INTO / OVERWRITE | `SqlInsertParser.parse(SQLInsertStatement)`，提取目标表/显式列/分区，复用 `SqlLineageParser.parseSelect` | `InsertStatementTest` 4 用例 |
| PARTITION 子句 | `SQLInsertInto.getPartitions()` → `DmlLineageInfo.partitions` (列名 → 值) | 覆盖于 INSERT OVERWRITE 测试 |
| CTAS | `SqlCreateTableAsParser.parse(SQLCreateTableStatement)`，从 column definitions 提取目标列 | `CtasTest` 3 用例 |
| 统一 DML 入口 | `SqlLineageParser.parserDmlSql(sql)` 自动分发 INSERT / CTAS | 2 用例 |
| 目标列兜底推断 | `DmlLineageInfo.getTargetColumnAt(i)`：显式列 → SELECT alias → expression 列名 | 覆盖于 INSERT 测试 |

### 关键文件

新增：
```
src/main/java/com/magic/core/parser/sql/dml/SqlInsertParser.java
src/main/java/com/magic/core/parser/sql/dml/SqlCreateTableAsParser.java
src/main/java/com/magic/sqllineageparser/model/DmlLineageInfo.java
src/main/java/com/magic/sqllineageparser/model/DmlOperation.java
sqls/sqlInsert/sqlInsert01.sql
sqls/sqlInsert/sqlInsertOverwrite01.sql
sqls/sqlCtas/sqlCtas01.sql
```

修改：
```
src/main/java/com/magic/core/parser/SqlLineageParser.java   # 暴露 parseSelect 为 public；新增 parserInsertSql / parserCreateTableSql / parserDmlSql
src/test/java/com/magic/core/util/SqlFileReader.java        # 新增 readInsertSql / readCtasSql
src/test/java/com/magic/core/parser/SqlLineageParserTest.java  # 新增 InsertStatementTest / CtasTest
README.md / RELEASE.md                                       # 同步 v0.3.0 状态
```

### 实现要点

- **复用 SELECT 解析**：将原 `SqlLineageParser.parseSelect(SQLSelect)` 由 private 提升为 public，让 dml 包的 INSERT / CTAS 解析器无需重复实现，并自动继承 v0.2.0 已有的 UNION / CTE / 嵌套子查询 / 窗口函数 / LATERAL VIEW 等能力。
- **INSERT/OVERWRITE 统一入口**：Druid 的 `SQLInsertStatement.isOverwrite()` 是区分两类语句的唯一信号，无需独立解析器。
- **目标列对齐**：当 `INSERT INTO t (c1, c2) SELECT ...` 显式给出目标列时直接对齐；省略时按位置回退到 SELECT 列 alias，再回退到从 expression 中提取列名（处理 `t.col` 这种 SQLPropertyExpr 不带 alias 的情况）。
- **CTAS 列名**：优先取 `CREATE TABLE t (c1, c2) AS SELECT ...` 中的显式 column definitions；缺失时回退到 SELECT 输出名。

---

## 2026-05-19 完成 v0.2.0 查询增强功能

### 概述

按 v0.2.0 版本规划完善 SQL 血缘解析能力，新增 UNION / CTE / 窗口函数 / LATERAL VIEW / CAST / ALTER 解析，并补充嵌套子查询血缘下钻与单表 FROM 裸列推断。共新增 12 个 Java 源文件，扩展 5 个核心解析器/上下文/接口。所有 71 个单元测试通过。

### 新增能力

| 能力 | 关键实现 | 测试 |
|------|----------|------|
| UNION / INTERSECT / EXCEPT 血缘合并 | `SqlLineageParser.parseUnionQuery` 按列位置合并左右分支 | `UnionQueryTest` 2 用例 |
| WITH (CTE) 解析 | `SqlLineageParser.collectCteSources` 预解析 CTE 列血缘并注册为虚拟表，支持链式 CTE | `CteQueryTest` 3 用例 |
| 嵌套子查询血缘下钻 | `SqlLineageParser.registerNestedSubqueries` + `ExprParseContext.registerVirtualTable` | `NestedSubqueryTest` 1 用例 |
| 窗口函数 (OVER) | 新增 `SqlOverExprParser`，处理 PARTITION/ORDER/DISTRIBUTE/SORT/CLUSTER BY；`SqlAggregateExprParser` 递归处理 `getOver()` | `WindowFunctionTest` 3 用例 |
| LATERAL VIEW | 新增 `SqlLateralViewTableSourceParser`；`SqlLineageParser.registerLateralViewOutputs` 将输出列映射到 UDTF 输入列 | `LateralViewTest` 1 用例 |
| CAST 表达式 | 新增 `SqlCastExprParser`，递归下钻内部表达式 | 既有 CAST 测试已覆盖 |
| ALTER TABLE 解析 | 新增 `SqlAlterTableParser` + `AlterTableInfo` / `AlterColumnChange`；新入口 `SqlLineageParser.parserAlterTableSql` | `AlterTableTest` 2 用例 |
| 单表 FROM 裸列推断 | `ExprParseContext.inferSingleTableName` 在唯一表 FROM 下自动归属裸列 | 覆盖于 CTE / 嵌套子查询测试 |

### 关键文件

新增：
```
src/main/java/com/magic/core/parser/sql/alter/SqlAlterTableParser.java
src/main/java/com/magic/core/parser/sql/expr/SqlCastExprParser.java
src/main/java/com/magic/core/parser/sql/expr/SqlOverExprParser.java
src/main/java/com/magic/core/parser/sql/table/SqlLateralViewTableSourceParser.java
src/main/java/com/magic/sqllineageparser/model/AlterTableInfo.java
src/main/java/com/magic/sqllineageparser/model/AlterColumnChange.java
sqls/sqlFunction/sqlWindow01.sql
sqls/sqlSelect/sqlCte01.sql
```

修改：
```
src/main/java/com/magic/core/parser/SqlLineageParser.java         # 重写：支持 UNION / CTE / ALTER / 虚拟表下钻
src/main/java/com/magic/core/parser/sql/expr/BaseSqlExprParser.java  # 扩展 permits 与分发，含 SQLOver / SQLCastExpr
src/main/java/com/magic/core/parser/sql/expr/ExprParseContext.java  # 新增 virtualTableColumns / 单表 FROM 推断
src/main/java/com/magic/core/parser/sql/expr/SqlAggregateExprParser.java  # 递归处理 getOver()
src/main/java/com/magic/core/parser/sql/table/BaseTableSourceParser.java  # permits 新增 LateralView 解析器
src/test/java/com/magic/core/parser/SqlLineageParserTest.java   # 新增 6 个 @Nested 测试组
src/test/java/com/magic/core/util/SqlFileReader.java            # 新增 readSelectSql
README.md / RELEASE.md                                          # 同步 v0.2.0 状态
```

### 实现要点

- **虚拟表下钻**：`ExprParseContext` 引入 `virtualTableColumns` 映射，记录 CTE / 子查询暴露的可见列 -> 真实来源列。当外层引用 `alias.col` 时先解析别名（alias → CTE 名），再下钻到真实表的真实列。
- **UNION 合并策略**：按列位置（position-based）合并，第 i 列输出的来源 = 所有分支第 i 列来源的并集。沿用左分支的列名 / 别名 / 表达式作为模板。
- **CTE 链式引用**：解析顺序按 WITH 子句中的 entry 顺序，前面 CTE 注册结果作为后面 CTE 的上下文，支持 `WITH a AS (...), b AS (SELECT FROM a) SELECT FROM b`。
- **LATERAL VIEW 列映射**：从 `SQLMethodInvokeExpr.getArguments()` 中提取列引用，将所有输出列（`AS c1, c2, ...`）映射到这些来源列。
- **裸列推断**：仅在 FROM 仅含唯一表（含虚拟表）时启用，避免 JOIN 多表场景下的歧义。

---

## 2026-05-19 完善 v0.2.0 文档

### 操作内容

基于现有 Java 代码实际实现，重新梳理 v0.2.0 版本的功能边界，并同步更新 README 与 RELEASE 文档：

- 在 `README.md` 「版本规划」表中将 v0.2.0 标记为 🚧 当前版本，并新增 **v0.2.0 功能详情** 章节
- 在 `README.md` 「核心特性」中补充 ExprParseContext、表别名自动还原、密封接口架构等条目
- 在 `README.md` 「开发状态」中刷新为 v0.2.0 当前进度
- 在 `README.md` 「支持的表源类型」中将 UNION / CTE 状态细化为「框架已建立」
- 在 `RELEASE.md` 中将 v0.2.0 章节重写为 已交付 / 开发中 / 计划中 三段
- 文档版本号更新至 v1.2

### v0.2.0 已交付特性归类

| 类别 | 特性 | 关键文件 |
|------|------|----------|
| 运行环境 | Java 17 升级（sealed interface / pattern matching / var） | `pom.xml`, `BaseSqlExprParser.java`, `BaseTableSourceParser.java` |
| 解析核心 | ExprParseContext 解析上下文 | `core/parser/sql/expr/ExprParseContext.java` |
| 解析核心 | 表别名 → 真实表名自动还原 | `ExprParseContext#collectTableAlias`, `resolveTableName` |
| 架构 | Expr 解析器模块化（9 个 Sql\*ExprParser） | `core/parser/sql/expr/Sql*ExprParser.java` |
| 架构 | TableSource 解析器模块化（5 个 Sql\*TableSourceParser） | `core/parser/sql/table/Sql*TableSourceParser.java` |
| 持久化 | LineageNode / LineageEdge / LineageGraph 实体模型 | `persistence/entity/*` |
| 持久化 | LineageRepository 接口 + Neo4j / Nebula 预留实现 | `persistence/repository/*` |
| 工程化 | 调试工具集 | `test/java/com/magic/core/debug/*` |
| 测试 | JUnit 5 测试套件 | `test/java/com/magic/core/parser/**/*Test.java` |

### v0.2.0 开发中条目

- UNION / INTERSECT / EXCEPT 完整解析（`SqlUnionQueryTableSourceParser` 仅记录日志，`SqlLineageParser` 中 UNION 入口返回 `null`）
- WITH 子查询（CTE）完整解析（`SqlWithSubqueryTableSourceParser` 仅记录日志）
- 窗口函数 / LATERAL VIEW / ALTER 语句解析（暂未纳入 sealed permits）

---

## 2026-02-04 血缘持久化模块

### 新增 `com.magic.persistence` 包

新增持久化模块，用于将 SQL 血缘解析结果持久化到图数据库（Neo4j / Nebula Graph）。

#### 包结构

```
com.magic.persistence
├── config
│   └── GraphDbConfig.java         # 图数据库连接配置
├── entity
│   ├── LineageNode.java           # 血缘节点实体（表/列）
│   ├── LineageEdge.java           # 血缘边实体（关系）
│   └── LineageGraph.java          # 血缘图实体（节点+边的集合）
└── repository
    ├── LineageRepository.java     # 持久化接口
    ├── Neo4jLineageRepository.java    # Neo4j 实现
    └── NebulaLineageRepository.java   # Nebula Graph 实现
```

#### 实体说明

| 类 | 说明 |
|---|------|
| `LineageNode` | 图中的节点，类型为 `TABLE` 或 `COLUMN`。提供 `ofTable()` / `ofColumn()` / `ofConstant()` 工厂方法 |
| `LineageEdge` | 图中的边，表示两个节点之间的血缘关系（如 `DERIVES_FROM`） |
| `LineageGraph` | 封装一组节点和边，代表一条 SQL 解析出的完整血缘图 |

#### 接口说明 (`LineageRepository`)

| 方法 | 说明 |
|------|------|
| `connect()` / `close()` | 连接/关闭图数据库 |
| `saveNode()` / `saveNodes()` | 保存节点（单个/批量） |
| `saveEdge()` / `saveEdges()` | 保存边（单个/批量） |
| `saveGraph()` | 保存完整血缘图 |
| `findUpstream()` / `findDownstream()` | 查询上/下游血缘 |
| `findNodeById()` / `findColumnsByTable()` | 按ID/表名查询节点 |
| `clearBySqlId()` | 按SQL ID清除血缘数据 |

#### 实现状态

| 实现类 | 状态 | 说明 |
|--------|------|------|
| `Neo4jLineageRepository` | **预留** | Cypher 语句已生成，`executeCypher()` 待接入 `neo4j-java-driver` |
| `NebulaLineageRepository` | **预留** | nGQL 语句已生成，`executeNgql()` 待接入 `nebula-java` |

#### 待接入依赖

Neo4j:
```xml
<dependency>
    <groupId>org.neo4j.driver</groupId>
    <artifactId>neo4j-java-driver</artifactId>
    <version>5.x</version>
</dependency>
```

Nebula Graph:
```xml
<dependency>
    <groupId>com.vesoft</groupId>
    <artifactId>client</artifactId>
    <version>3.x</version>
</dependency>
```

#### 使用示例

```java
// 1. 配置连接
GraphDbConfig config = new GraphDbConfig("localhost", 7687, "neo4j", "password");
config.setDatabase("lineage");

// 2. 创建 Repository
LineageRepository repo = new Neo4jLineageRepository(config);
repo.connect();

// 3. 构建血缘图
LineageGraph graph = new LineageGraph(sql);
LineageNode sourceCol = LineageNode.ofColumn("db", "t1", "col1");
LineageNode targetCol = LineageNode.ofColumn("db", "result", "out_col");
graph.addLineage(sourceCol, targetCol, "DERIVES_FROM");

// 4. 持久化
repo.saveGraph(graph);

// 5. 查询血缘
List<LineageNode> upstream = repo.findUpstream(targetCol.getId());

// 6. 关闭
repo.close();
```

---

## 2026-02-04 ExprParser 解析结果保存

### 新增 `ExprParseContext`

新增 `com.magic.core.parser.sql.expr.ExprParseContext`，用于在表达式解析过程中保存上下文状态和收集结果。

- 保存表别名 → 真实表名映射
- 跟踪当前解析的目标列
- 解析结果自动收集到 `ColumnNode.sourceColumns`

### 修改 `BaseSqlExprParser`

- 新增 `parserSqlExpr(SQLExpr, ExprParseContext)` 方法
- 旧无参版本标记为 `@Deprecated`

### 修改所有 ExprParser 实现类

所有 `parse()` 方法新增 `ExprParseContext` 参数，解析时将来源列自动写入上下文。

### 修改 `SqlLineageParser`

- 从 FROM 子句创建 `ExprParseContext`
- 解析每列时通过 `context.setCurrentColumn()` 设定目标列
- 来源列自动收集到 `ColumnNode.sourceColumns`

---

## 2026-02-04 Expr 解析器独立文件

将 `SqlLineageParser` 中的 expr 解析方法拆分到 `parser/sql/expr/` 下独立文件：

- `SqlCaseExprParser` / `SqlAggregateExprParser` / `SqlBinaryOpExprParser`
- `SqlPropertyExprParser` / `SqlIdentifierExprParser`
- `SqlNumberExprParser` / `SqlIntegerExprParser` / `SqlCharExprParser`
- `SqlMethodInvokeExprParser`（已有，补充实际逻辑）

`BaseSqlExprParser` 作为 sealed interface 统一管理 permits 和分发。
