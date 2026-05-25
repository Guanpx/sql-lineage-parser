# UPDATE_CLAUDE.md - 变更记录

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
