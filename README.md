# SQL Lineage Parser

基于 Alibaba Druid 的 SQL 血缘解析工具，用于解析 SQL 语句中的表级和列级数据血缘关系。

## 项目简介

`sql-lineage-parser` 能够解析 SQL 语句（当前主要支持 Hive SQL），提取出字段级别的血缘关系，追踪数据从源表到目标表的流转路径。项目基于 Druid SQL Parser 进行 AST 解析。

### 核心特性

- **字段级血缘追踪**: 精确到列的血缘关系解析
- **解析上下文 (ExprParseContext)**: 解析过程中保存表别名映射、目标列引用，并自动收集来源列
- **表别名自动还原**: 自动将 `a.col1` 中的别名 `a` 还原为真实表名 `t1`
- **单表 FROM 裸列推断**: 唯一表场景下 `SELECT id FROM users` 自动归属 `id` 到 `users`
- **复杂表达式支持**: CASE WHEN、聚合函数、嵌套函数、二元运算、CAST、窗口函数 (OVER) 等
- **多种表源解析**: 普通表、JOIN、子查询、UNION、CTE (WITH)、LATERAL VIEW
- **虚拟表列下钻**: CTE / 子查询作为 FROM 表源时，外层 `alias.col` 自动下钻到真实底表列
- **DML 血缘**: INSERT INTO / INSERT OVERWRITE + PARTITION、CREATE TABLE AS SELECT、CREATE VIEW
- **DDL 解析**: 纯 CREATE TABLE、CREATE VIEW AS SELECT、ALTER TABLE ADD/DROP/RENAME/MODIFY/CHANGE COLUMN，提取字段、类型、注释、默认值和分区字段
- **树形结构输出**: 直观的血缘树结构，便于遍历和分析
- **类型安全的密封接口**: 基于 Java 17 `sealed interface` 限制并管理解析器实现类
- **模块化设计**: 表达式 / 表源 / DML / DDL 解析器按类型独立成文件，易于扩展新语法
- **图数据库持久化**: 支持将血缘关系写入 Neo4j / Nebula Graph（预留接入）

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 17+ | 运行环境 |
| Alibaba Druid | 1.2.20 | SQL 解析引擎 |
| Maven | 3.x | 构建工具 |
| JUnit 5 | 5.10.0 | 测试框架 |
| Lombok | 1.18.28 | 代码简化 |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.x

### 构建

```bash
# 编译打包
mvn clean package

# 运行测试
mvn test
```

### 基础使用

#### 1. SELECT 血缘解析

```java
import com.magic.core.parser.SqlLineageParser;
import com.magic.sqllineageparser.model.ColumnNode;
import com.magic.sqllineageparser.model.TreeNode;

String sql = """
    SELECT
        a.id,
        b.name,
        SUM(a.amount) as total
    FROM orders a
    JOIN users b ON a.user_id = b.id
    GROUP BY a.id, b.name
    """;

TreeNode<ColumnNode> lineageTree = SqlLineageParser.parserSingleSelectSql(sql);

lineageTree.getChildren().forEach(child -> {
    ColumnNode column = child.getValue();
    System.out.println("输出列: " + column.getAlias());
    System.out.println("来源: " + column.getSourceColumns());
    System.out.println("---");
});
```

#### 2. CTE / UNION / 窗口函数

```java
// CTE 链式引用 - 外层 alias.col 自动下钻到真实底表
String cteSql = """
    WITH active_users AS (SELECT id, name FROM users WHERE status='ACTIVE'),
         top_orders   AS (SELECT user_id, SUM(amount) AS total FROM orders GROUP BY user_id)
    SELECT au.name, t.total
    FROM active_users au LEFT JOIN top_orders t ON au.id = t.user_id
    """;
TreeNode<ColumnNode> tree = SqlLineageParser.parserSingleSelectSql(cteSql);
// au.name → users.name, t.total → orders.amount

// UNION 按列位置合并左右分支
String unionSql = "SELECT id FROM t1 UNION ALL SELECT id FROM t2";
TreeNode<ColumnNode> unionTree = SqlLineageParser.parserSingleSelectSql(unionSql);
// 第 1 列来源 = [t1.id, t2.id]

// 窗口函数 PARTITION BY / ORDER BY 列自动收集
String winSql = "SELECT ROW_NUMBER() OVER (PARTITION BY dept ORDER BY salary DESC) rn FROM emp";
TreeNode<ColumnNode> winTree = SqlLineageParser.parserSingleSelectSql(winSql);
// rn 来源 = [emp.dept, emp.salary]
```

#### 3. INSERT / CTAS 血缘解析

```java
import com.magic.sqllineageparser.model.DmlLineageInfo;

// INSERT INTO / INSERT OVERWRITE
String insertSql = """
    INSERT OVERWRITE TABLE dw.daily_order PARTITION (dt='2026-05-20')
    SELECT order_id, user_id, amount FROM ods.orders WHERE order_date='2026-05-20'
    """;
DmlLineageInfo info = SqlLineageParser.parserInsertSql(insertSql);
System.out.println(info.getOperation());            // INSERT_OVERWRITE
System.out.println(info.getQualifiedTargetTable()); // dw.daily_order
System.out.println(info.getPartitions());           // {dt='2026-05-20'}
for (int i = 0; i < info.getOutputColumnCount(); i++) {
    String targetCol = info.getTargetColumnAt(i);
    var sources = info.getSourceLineage().getChildren().get(i).getValue().getSourceColumns();
    System.out.println(targetCol + " <- " + sources);
}

// CREATE TABLE AS SELECT
DmlLineageInfo ctas = SqlLineageParser.parserCreateTableSql(
    "CREATE TABLE dw.top_users AS SELECT id AS user_id, name FROM users");

// CREATE VIEW ... AS SELECT (视图定义的列血缘)
DmlLineageInfo view = SqlLineageParser.parserCreateViewSql(
    "CREATE VIEW dw.v_active_user AS SELECT id AS user_id, name FROM users WHERE status='ACTIVE'");

// 统一 DML/DDL 入口（自动识别 INSERT / CTAS / CREATE VIEW）
DmlLineageInfo any = SqlLineageParser.parserDmlSql(insertSql);
```

#### 4. DDL 语句解析

```java
import com.magic.sqllineageparser.model.AlterTableInfo;
import com.magic.sqllineageparser.model.CreateTableInfo;

// 纯 CREATE TABLE：提取表注释、字段、类型、注释、默认值与分区字段
String createSql = """
    CREATE TABLE dw.user_profile (
      user_id BIGINT COMMENT '用户ID',
      amount DECIMAL(18, 2) DEFAULT 0
    ) COMMENT '用户画像表'
    PARTITIONED BY (dt STRING)
    """;
CreateTableInfo table = SqlLineageParser.parserCreateTableDdlSql(createSql);
System.out.println(table.getQualifiedTableName()); // dw.user_profile
System.out.println(table.getColumns().get(0).getComment()); // 用户ID

String alterSql = "ALTER TABLE warehouse.orders ADD COLUMNS (status string COMMENT '订单状态')";
AlterTableInfo info = SqlLineageParser.parserAlterTableSql(alterSql);

System.out.println(info.getQualifiedTableName()); // warehouse.orders
info.getChanges().forEach(c -> System.out.println(c));
// ADD status string COMMENT '订单状态'
```

### 血缘树结构示意

```
ROOT (虚拟根节点)
├── Column: id        (alias=null)
│   └── Source: orders.id
├── Column: name      (alias=null)
│   └── Source: users.name
└── Column: total     (alias=total, expr=SUM(a.amount))
    └── Source: orders.amount
```

DML 场景下 `DmlLineageInfo` 在血缘树之上额外携带：

```
DmlLineageInfo
├── operation        : INSERT_INTO | INSERT_OVERWRITE | CTAS | CREATE_VIEW
├── targetSchema     : dw
├── targetTable      : daily_order
├── targetColumns    : [order_id, user_id, amount]  (显式或回退到 SELECT alias)
├── partitions       : {dt='2026-05-20'}
└── sourceLineage    : TreeNode<ColumnNode>  (同 SELECT 血缘树)
```

## 入口 API 一览

| 入口 | 输入 | 输出 | 适用场景 |
|------|------|------|---------|
| `parserSingleSelectSql(sql)` | SELECT / UNION / WITH | `TreeNode<ColumnNode>` | 查询血缘 |
| `parserInsertSql(sql)` | INSERT INTO / OVERWRITE | `DmlLineageInfo` | 数据写入血缘 |
| `parserCreateTableSql(sql)` | CREATE TABLE [AS SELECT] | `DmlLineageInfo` (非 CTAS 返回 null) | 建表 + 数据流 |
| `parserCreateTableDdlSql(sql)` | 不带 AS SELECT 的 CREATE TABLE | `CreateTableInfo` | 表结构元信息 |
| `parserDmlSql(sql)` | INSERT / CTAS / CREATE VIEW | `DmlLineageInfo` | DML/DDL 统一入口 |
| `parserCreateViewSql(sql)` | CREATE VIEW [AS SELECT] | `DmlLineageInfo` | 视图血缘 |
| `parserAlterTableSql(sql)` | ALTER TABLE | `AlterTableInfo` | 表结构变更 |
| `parseSelect(SQLSelect)` | Druid AST SQLSelect | `TreeNode<ColumnNode>` | 内部 / 高级用户复用 |

## 项目结构

```
sql-lineage-parser/
├── src/main/java/com/magic/
│   ├── core/
│   │   ├── parser/
│   │   │   ├── SqlLineageParser.java           # 血缘解析核心入口（SELECT / UNION / CTE / DML / DDL）
│   │   │   └── sql/
│   │   │       ├── alter/                      # DDL 解析器
│   │   │       │   └── SqlAlterTableParser.java
│   │   │       ├── ddl/                        # DDL 解析器（纯建表 / 视图等）
│   │   │       │   └── SqlCreateViewParser.java
│   │   │       │   └── SqlCreateTableParser.java
│   │   │       ├── dml/                        # DML 解析器
│   │   │       │   ├── SqlInsertParser.java
│   │   │       │   └── SqlCreateTableAsParser.java
│   │   │       ├── table/                      # 表源解析器
│   │   │       │   ├── BaseTableSourceParser.java
│   │   │       │   ├── SqlExprTableSourceParser.java
│   │   │       │   ├── SqlJoinTableSourceParser.java
│   │   │       │   ├── SqlSubqueryTableSourceParser.java
│   │   │       │   ├── SqlUnionQueryTableSourceParser.java
│   │   │       │   ├── SqlWithSubqueryTableSourceParser.java
│   │   │       │   └── SqlLateralViewTableSourceParser.java
│   │   │       └── expr/                       # 表达式解析器
│   │   │           ├── BaseSqlExprParser.java
│   │   │           ├── ExprParseContext.java   # 解析上下文 (含虚拟表下钻)
│   │   │           ├── SqlCaseExprParser.java
│   │   │           ├── SqlAggregateExprParser.java
│   │   │           ├── SqlMethodInvokeExprParser.java
│   │   │           ├── SqlBinaryOpExprParser.java
│   │   │           ├── SqlPropertyExprParser.java
│   │   │           ├── SqlIdentifierExprParser.java
│   │   │           ├── SqlCastExprParser.java
│   │   │           ├── SqlOverExprParser.java  # 窗口函数 OVER 子句
│   │   │           └── Sql*ExprParser.java     # 其他表达式解析器
│   │   └── utils/
│   │       └── StringUtils.java
│   ├── persistence/                            # 持久化模块
│   │   ├── config/
│   │   │   └── GraphDbConfig.java              # 图数据库配置
│   │   ├── entity/
│   │   │   ├── LineageNode.java                # 血缘节点
│   │   │   ├── LineageEdge.java                # 血缘边
│   │   │   └── LineageGraph.java               # 血缘图
│   │   └── repository/
│   │       ├── LineageRepository.java          # 持久化接口
│   │       ├── Neo4jLineageRepository.java     # Neo4j 实现
│   │       └── NebulaLineageRepository.java    # Nebula 实现
│   └── sqllineageparser/model/
│       ├── TreeNode.java                       # 通用树结构
│       ├── TableNode.java                      # 表节点模型
│       ├── ColumnNode.java                     # 列节点模型
│       ├── AlterTableInfo.java                 # ALTER 解析结果
│       ├── AlterColumnChange.java              # 单列变更项
│       ├── CreateTableInfo.java                # 纯建表解析结果
│       ├── TableColumnMeta.java                # 建表字段元信息
│       ├── DmlLineageInfo.java                 # DML/DDL 血缘解析结果（INSERT / CTAS / CREATE VIEW）
│       └── DmlOperation.java                   # DML 操作类型枚举
├── src/test/                                   # 测试代码
├── sqls/                                       # SQL 测试用例库
│   ├── sqlAlter/                               # ALTER 语句
│   ├── sqlCase/                                # CASE WHEN 语句
│   ├── sqlCtas/                                # CTAS 语句
│   ├── sqlCreateTable/                         # 纯 CREATE TABLE 语句
│   ├── sqlFunction/                            # 函数 / 窗口函数语句
│   ├── sqlInsert/                              # INSERT 语句
│   ├── sqlJoin/                                # JOIN 语句
│   ├── sqlProd/                                # 生产级复杂 SQL
│   ├── sqlSelect/                              # CTE 等 SELECT 用例
│   ├── sqlUnion/                               # UNION 语句
│   └── sqlView/                                # CREATE VIEW 语句
├── RELEASE.md                                  # 版本规划文档
├── UPDATE_DEV.md                               # 开发操作记录
└── pom.xml
```

## 核心模块

### 持久化模块

血缘持久化模块支持将解析结果写入图数据库。

#### 实体类

| 类 | 说明 |
|---|------|
| `LineageNode` | 血缘节点（表/列），提供 `ofTable()` / `ofColumn()` / `ofConstant()` 工厂方法 |
| `LineageEdge` | 血缘边，表示节点间的数据流转关系 |
| `LineageGraph` | 血缘图，封装节点和边的集合 |

#### Repository 接口

```java
LineageRepository repo = new Neo4jLineageRepository(config);
repo.connect();

// 保存血缘图
LineageGraph graph = new LineageGraph(sql);
graph.addLineage(sourceNode, targetNode, "DERIVES_FROM");
repo.saveGraph(graph);

// 查询血缘
List<LineageNode> upstream = repo.findUpstream(nodeId);
List<LineageNode> downstream = repo.findDownstream(nodeId);

repo.close();
```

#### 支持的图数据库

| 数据库 | 实现类 | 状态 |
|--------|--------|------|
| Neo4j | `Neo4jLineageRepository` | 预留（Cypher 已实现） |
| Nebula Graph | `NebulaLineageRepository` | 预留（nGQL 已实现） |

### 数据模型

| 模型 | 说明 |
|------|------|
| **TreeNode\<T>** | 通用泛型树结构，支持父子关系、层高计算、子树遍历 |
| **TableNode** | 表节点：schema、表名、别名、是否虚拟表、字段列表 |
| **ColumnNode** | 列节点：列名、别名、来源列、表达式、是否常量 |
| **AlterTableInfo** | ALTER 解析结果：目标表 + 列变更列表 |
| **AlterColumnChange** | 单列变更项：ADD / DROP / RENAME / MODIFY |
| **CreateTableInfo** | 纯 CREATE TABLE 解析结果：目标表 + 表注释 + 字段与分区字段元信息 |
| **TableColumnMeta** | 建表字段元信息：字段名、类型、注释、默认值、主键标记 |
| **DmlLineageInfo** | DML 解析结果：操作类型 / 目标表 / 目标列 / 分区 / 源 SELECT 血缘 |
| **DmlOperation** | DML 操作枚举：INSERT_INTO / INSERT_OVERWRITE / CTAS / CREATE_VIEW |

### 支持的表达式类型

| 表达式 | 示例 | 状态 |
|--------|------|------|
| 列引用 | `t.id`, `name` | ✅ |
| CASE WHEN | `CASE WHEN a=1 THEN 'x' ELSE 'y' END` | ✅ |
| 聚合函数 | `SUM(amount)`, `COUNT(*)`, `MAX(score)` | ✅ |
| 函数调用 | `CONCAT(a, b)`, `NVL(x, 0)`, `IF(...)` | ✅ |
| 二元运算 | `a + b`, `a > b`, `a AND b` | ✅ |
| 常量 | `'hello'`, `123`, `3.14` | ✅ |
| CAST | `CAST(x AS STRING)` | ✅ |
| 窗口函数 (OVER) | `ROW_NUMBER() OVER (PARTITION BY x ORDER BY y)` | ✅ |

### 支持的表源类型

| 表源 | 示例 | 状态 |
|------|------|------|
| 普通表 | `FROM users u` | ✅ |
| JOIN | `LEFT JOIN orders o ON ...` | ✅ |
| 子查询 | `FROM (SELECT ...) t` | ✅ 支持外层 alias.col 下钻 |
| UNION | `SELECT ... UNION SELECT ...` | ✅ 按列位置合并左右分支 |
| CTE | `WITH t AS (...) SELECT ...` | ✅ 支持多 CTE 链式引用 |
| LATERAL VIEW | `LATERAL VIEW explode(arr) v AS item` | ✅ 输出列下钻到 UDTF 输入列 |

## 支持的 SQL 类型

| 类型 | 状态 | 说明 |
|------|------|------|
| 单表 SELECT | ✅ 已支持 | 完整支持 |
| JOIN 查询 | ✅ 已支持 | LEFT/RIGHT/INNER/FULL JOIN |
| 子查询 | ✅ 已支持 | 派生表、嵌套子查询血缘下钻 |
| CASE WHEN | ✅ 已支持 | 简单和搜索型 CASE |
| 聚合函数 | ✅ 已支持 | SUM/COUNT/AVG/MAX/MIN |
| 内置函数 | ✅ 已支持 | IF/NVL/CONCAT/SUBSTR 等 |
| UNION 查询 | ✅ 已支持 | UNION / UNION ALL / INTERSECT / EXCEPT，按列位置合并 |
| CTE (WITH) | ✅ 已支持 | 单 CTE 与多 CTE 链式引用 |
| 窗口函数 | ✅ 已支持 | ROW_NUMBER/RANK/LAG/LEAD + PARTITION BY/ORDER BY |
| LATERAL VIEW | ✅ 已支持 | Hive 行转列，输出列下钻到 UDTF 输入列 |
| CAST 类型转换 | ✅ 已支持 | 递归下钻到内部表达式 |
| ALTER 语句 | ✅ 已支持 | ADD COLUMNS / DROP COLUMN / RENAME COLUMN / CHANGE COLUMN / MODIFY COLUMN |
| INSERT 语句 | ✅ 已支持 | INSERT INTO / OVERWRITE + PARTITION，含目标列对齐 |
| CTAS 语句 | ✅ 已支持 | CREATE TABLE AS SELECT |
| CREATE VIEW 语句 | ✅ 已支持 | 视图定义列血缘，支持显式列覆盖 |
| CREATE TABLE 语句 | ✅ 已支持 | 纯 DDL 字段元信息；CTAS 另走数据血缘入口 |

## 版本规划

详见 [RELEASE.md](./RELEASE.md)

| 版本 | 目标 | 状态 |
|------|------|------|
| v0.1.0 | 基础 SELECT 解析 | ✅ 已完成 |
| v0.2.0 | 查询增强 / UNION / CTE / 窗口函数 / LATERAL VIEW / ALTER | ✅ 已完成 |
| v0.3.0 | DML 支持（INSERT INTO/OVERWRITE / CTAS） | ✅ 已完成 |
| v0.4.0 | DDL 支持（CREATE VIEW / CREATE TABLE / ALTER 增强） | ✅ 已完成（DROP 暂不开发） |
| v0.5.0 | 血缘输出增强（JSON/DOT） | 📋 计划中 |
| v1.0.0 | 生产就绪版本 | 📋 计划中 |

### v0.2.0 功能详情

v0.2.0 在 v0.1.0 基础 SELECT 解析能力之上，引入解析上下文、模块化架构、图数据库持久化预留，并完成 UNION / CTE / 窗口函数 / LATERAL VIEW / ALTER 等查询增强能力。

#### 已交付特性 ✅

| 类别 | 特性 | 说明 |
|------|------|------|
| 运行环境 | **Java 17 升级** | 全面升级到 JDK 17，启用 `sealed interface`、`pattern matching`、`var` 等新特性 |
| 解析核心 | **ExprParseContext 解析上下文** | 在表达式解析过程中保存别名映射、目标列引用，并自动收集来源列到 `ColumnNode.sourceColumns` |
| 解析核心 | **表别名自动解析** | 自动将 `a.col1` 中的别名 `a` 还原为真实表名 `t1`，写入血缘节点 |
| 解析核心 | **单表 FROM 裸列推断** | 唯一表 FROM 场景下，`SELECT id FROM users` 中的 `id` 自动归属到 `users.id` |
| 解析核心 | **虚拟表列下钻** | CTE / 子查询作为 FROM 表源时，外层 `alias.col` 自动下钻到真实来源表/列 |
| 查询增强 | **UNION / INTERSECT / EXCEPT** | 递归解析左右分支，按列位置合并血缘 |
| 查询增强 | **WITH (CTE) 查询** | 预解析每个 CTE 的列血缘并注册为虚拟表；支持 CTE 链式引用 |
| 查询增强 | **嵌套子查询血缘下钻** | FROM 子查询自动注册虚拟表，逐层解析到真实底表 |
| 查询增强 | **窗口函数 (OVER)** | 支持 `SUM/ROW_NUMBER/RANK/LAG/LEAD` 等，PARTITION BY / ORDER BY / SORT BY / DISTRIBUTE BY / CLUSTER BY 子句列引用全收集 |
| 查询增强 | **LATERAL VIEW** | 行转列输出列自动映射到 UDTF 输入列（如 `explode(t.arr) AS item` → `t.arr`） |
| 查询增强 | **CAST 表达式** | `CAST(expr AS TYPE)` 递归下钻到内部表达式 |
| DDL | **ALTER TABLE 语句** | 通过 `parserAlterTableSql` 解析 ADD COLUMNS / DROP COLUMN / RENAME COLUMN，提取字段、类型、注释 |
| 架构 | **表达式解析器模块化** | 拆分为 11 个独立 `Sql*ExprParser` 文件，统一通过 `BaseSqlExprParser` 密封接口分发 |
| 架构 | **表源解析器模块化** | `BaseTableSourceParser` 密封接口管理 6 种 `SQLTableSource` 实现（含 LATERAL VIEW） |
| 持久化 | **血缘实体模型** | `LineageNode` / `LineageEdge` / `LineageGraph`，含表/列/常量工厂方法与节点 ID 生成 |
| 持久化 | **Repository 接口** | `LineageRepository` 统一抽象连接、节点、边、图、上下游查询、SQL ID 清理 |
| 持久化 | **Neo4j 预留实现** | `Neo4jLineageRepository`，Cypher MERGE / 上下游查询语句已生成，待接入 `neo4j-java-driver` |
| 持久化 | **Nebula 预留实现** | `NebulaLineageRepository`，nGQL 语句已生成，待接入 `nebula-java` |
| 工程化 | **调试工具集** | `SqlLineageParserDebug` / `SqlExprParserDebug` / `TableSourceParserDebug` / `DebugHelper` 用于开发期可视化血缘树 |
| 测试 | **JUnit 5 测试套件** | 78 个用例覆盖 SELECT、UNION、CTE、嵌套子查询、窗口函数、LATERAL VIEW、ALTER、INSERT、CTAS 等场景 |
| 测试 | **生产 SQL 用例** | `sqlProd01.sql`、`sqlWindow01.sql`、`sqlCte01.sql` 等复杂场景回归 |

#### 计划中 📋

| 特性 | 说明 |
|------|------|
| 嵌套函数深度优化 | 深层嵌套函数的递归终止与表达式签名规范化 |
| SELECT \* 字段展开 | 需结合元数据将 `*` 展开为具体列 |
| 多语句解析 | 批量 SQL 脚本一次性解析 |

### v0.3.0 功能详情

v0.3.0 在 v0.2.0 查询能力之上新增 DML 解析，建立从源表到目标表的完整数据流。

#### 已交付特性 ✅

| 类别 | 特性 | 说明 |
|------|------|------|
| DML 入口 | **`parserInsertSql`** | 解析 INSERT INTO / INSERT OVERWRITE，提取目标表、显式列、分区、源 SELECT 血缘 |
| DML 入口 | **`parserCreateTableSql`** | 解析 CREATE TABLE [AS SELECT]，非 CTAS 返回 null |
| DML 入口 | **`parserDmlSql`** | 统一入口，根据 AST 自动分发 INSERT / CTAS |
| 解析复用 | **`parseSelect(SQLSelect)` 公开** | 原私有方法提升为 public，让 DML 解析器无重复地复用全部 SELECT 能力（含 UNION/CTE/嵌套子查询/窗口函数/LATERAL VIEW） |
| DML 解析 | **INSERT INTO ... SELECT** | 完整解析，含显式目标列对齐 |
| DML 解析 | **INSERT OVERWRITE ... SELECT** | 通过 `isOverwrite()` 区分，与 INSERT INTO 共享入口 |
| DML 解析 | **PARTITION 子句** | 静态分区（含值）/ 动态分区（值为 null）均归入 `DmlLineageInfo.partitions` |
| DML 解析 | **CREATE TABLE AS SELECT (CTAS)** | 从 column definitions 提取目标列，回退到 SELECT 输出名 |
| DML 模型 | **`DmlLineageInfo` / `DmlOperation`** | 携带操作类型、目标表、目标列、分区、源 SELECT 血缘树 |
| DML 模型 | **目标列三级兜底** | 显式列 → SELECT alias → 从 expression 提取列名（处理 `t.col` 无 alias 场景） |
| 测试 | **DML 测试** | `InsertStatementTest` 4 用例 + `CtasTest` 3 用例，覆盖 INSERT INTO/OVERWRITE/PARTITION/CTAS/统一入口 |

#### 暂缓 🚫

| 特性 | 说明 |
|------|------|
| MERGE INTO | UPSERT 场景，识别源表与目标表的字段匹配规则 |
| 多语句脚本解析 | 一次解析多条 DML 语句，输出语句序列的血缘集合 |

### v0.4.0 功能详情

v0.4.0 聚焦 DDL 元信息与结构变更能力。按当前开发决策，DROP TABLE / VIEW 识别暂不开发。

#### 已交付特性 ✅

| 类别 | 特性 | 说明 |
|------|------|------|
| DDL 入口 | **`parserCreateTableDdlSql`** | 解析不带 AS SELECT 的 CREATE TABLE，返回 `CreateTableInfo` |
| DDL 解析 | **纯 CREATE TABLE** | 提取 schema、表名、表注释、字段名、类型、注释、默认值、主键标记与分区字段 |
| DDL 解析 | **CREATE VIEW** | 解析视图定义列血缘，支持显式列覆盖 |
| DDL 解析 | **ALTER CHANGE COLUMN** | 映射为 `MODIFY` 动作，保留原列名、新列名、类型与注释 |
| DDL 解析 | **ALTER MODIFY COLUMN** | Druid Hive 缺少独立分支时使用窄范围 MySQL AST 兼容解析，同样输出 `MODIFY` 动作 |
| DDL 模型 | **`CreateTableInfo` / `TableColumnMeta`** | 与 CTAS 的 `DmlLineageInfo` 分离，避免把元信息和数据血缘混在同一模型 |
| 测试 | **DDL 测试** | 新增 5 个用例，覆盖纯建表、分区字段、CTAS 边界、CHANGE COLUMN、MODIFY COLUMN |

#### 暂缓 🚫

| 特性 | 说明 |
|------|------|
| DROP TABLE / VIEW 识别 | 用户确认本轮不开发；后续如做需处理血缘失效与删除语义 |

## 开发状态

项目处于 v0.4.0 已交付状态（DROP 按当前决策暂不开发）。已交付能力：

- ✅ 完整的核心解析框架（表达式 + 表源 双密封接口）
- ✅ 解析上下文 ExprParseContext，列血缘自动收集与下钻
- ✅ 表别名 → 真实表名自动还原；单表 FROM 裸列推断
- ✅ UNION / CTE / 嵌套子查询 完整血缘解析
- ✅ 窗口函数（OVER 子句）/ LATERAL VIEW / CAST 表达式
- ✅ ALTER TABLE 解析（ADD/DROP/RENAME/MODIFY/CHANGE COLUMN）
- ✅ INSERT INTO / INSERT OVERWRITE + PARTITION 解析
- ✅ CREATE TABLE AS SELECT 解析；纯 CREATE TABLE 字段元信息解析；CREATE VIEW 解析
- ✅ 统一 DML/DDL 入口（INSERT / CTAS / CREATE VIEW）
- ✅ 持久化模块预留（Neo4j / Nebula 双实现）
- ✅ **87 个单元测试用例**，覆盖 SELECT / UNION / CTE / 子查询 / 窗口函数 / LATERAL VIEW / ALTER / INSERT / CTAS / CREATE VIEW / 纯 CREATE TABLE 等场景
- ✅ 生产级复杂 SQL 验证（sqlProd01.sql / sqlWindow01.sql / sqlCte01.sql / sqlInsert*.sql / sqlCtas01.sql / sqlView*.sql / sqlCreateTable01.sql）
- ✅ 调试工具集（SqlLineageParserDebug 等）

v0.4.0 暂缓项：

- DROP TABLE/VIEW 识别（用户确认不开发）

v0.3.0 暂缓项：

- MERGE INTO 语句解析
- 多语句批量脚本解析

后续版本规划：

- 血缘 JSON / DOT 标准化输出（v0.5.0）
- 多 SQL 方言支持、REST API / CLI 工具（v0.6.0+）

## 调试工具

项目提供调试工具类，便于开发调试：

```java
// 位于 src/test/java/com/magic/core/debug/
SqlLineageParserDebug.main(args);   // 主解析器调试
SqlExprParserDebug.main(args);      // 表达式解析调试
TableSourceParserDebug.main(args);  // 表源解析调试
```

## 贡献指南

欢迎贡献代码！

1. Fork 本仓库
2. 创建特性分支: `git checkout -b feature/your-feature`
3. 编写代码和测试
4. 确保测试通过: `mvn test`
5. 提交 PR

## License

未指定
