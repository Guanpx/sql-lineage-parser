# SQL Lineage Parser - Release 规划

> 本文档定义一个完整的 SQL 血缘解析工具应具备的能力，并规划当前项目的发展路线图。

## 一、完整的 SQL 血缘解析工具能力模型

### 1. 核心解析能力

| 能力 | 描述 | 重要性 |
|------|------|--------|
| SELECT 语句解析 | 解析查询语句，追踪字段来源 | P0 |
| INSERT 语句解析 | 识别目标表，建立完整的数据流向 | P0 |
| CTAS 解析 | CREATE TABLE AS SELECT，建表与数据流合一 | P0 |
| UNION/INTERSECT/EXCEPT | 集合操作的血缘合并 | P1 |
| CTE (WITH 子句) | 公共表表达式的递归解析 | P1 |
| 子查询解析 | 标量子查询、关联子查询、派生表 | P0 |
| JOIN 解析 | 多表关联的字段追踪 | P0 |

### 2. DDL 支持

| 能力 | 描述 | 重要性 |
|------|------|--------|
| CREATE TABLE | 表结构定义，字段元信息提取 | P1 |
| CREATE VIEW | 视图定义，透明化视图血缘 | P1 |
| ALTER TABLE | 字段变更追踪（ADD/DROP/RENAME/MODIFY/CHANGE COLUMN） | P2 |
| COMMENT | 字段注释提取 | P2 |

### 3. 表达式处理

| 表达式类型 | 描述 | 重要性 |
|------------|------|--------|
| 列引用 | table.column, column | P0 |
| 函数调用 | 内置函数、UDF | P0 |
| CASE WHEN | 条件表达式 | P0 |
| 聚合函数 | SUM, COUNT, AVG, MAX, MIN 等 | P0 |
| 窗口函数 | ROW_NUMBER, RANK, LAG, LEAD 等 | P1 |
| 算术运算 | +, -, *, /, % | P0 |
| 类型转换 | CAST, CONVERT | P0 |
| 字符串操作 | CONCAT, SUBSTR, TRIM 等 | P0 |
| 日期函数 | DATE_ADD, DATEDIFF, DATE_FORMAT 等 | P1 |
| NULL 处理 | NVL, COALESCE, IFNULL, NULLIF | P0 |
| 条件函数 | IF, IIF, DECODE | P0 |

### 4. 多 SQL 方言支持

| 方言 | 优先级 | 说明 |
|------|--------|------|
| Hive SQL | P0 | 当前主要支持 |
| Spark SQL | P1 | 与 Hive 语法相近 |
| MySQL | P1 | 广泛使用 |
| PostgreSQL | P2 | 标准 SQL 兼容性好 |
| Presto/Trino | P2 | 大数据查询引擎 |
| Oracle | P2 | 企业级数据库 |
| SQL Server | P3 | 企业级数据库 |
| ClickHouse | P3 | OLAP 场景 |

### 5. 高级特性

| 特性 | 描述 | 重要性 |
|------|------|--------|
| 多语句解析 | 批量 SQL 脚本解析 | P1 |
| 跨语句血缘 | 临时表、变量跨语句追踪 | P2 |
| 存储过程解析 | 解析 PL/SQL、HQL 存储过程 | P3 |
| 血缘可视化 | JSON/DOT/图数据库格式输出 | P1 |
| 影响分析 | 给定字段，分析上下游影响 | P1 |
| 血缘合并 | 多条 SQL 血缘图合并 | P2 |
| 增量解析 | 仅解析变更部分 | P3 |

### 6. 工程化能力

| 能力 | 描述 | 重要性 |
|------|------|--------|
| Java API | 程序化调用接口 | P0 |
| REST API | HTTP 服务接口 | P1 |
| CLI 工具 | 命令行工具 | P1 |
| Maven/Gradle 插件 | 构建集成 | P2 |
| IDE 插件 | IDEA/VSCode 插件 | P3 |
| Web UI | 可视化界面 | P2 |

---

## 二、当前项目状态评估

> 本节为「能力模型」与「当前代码」之间的快照对照，更细粒度的版本进度见第三节。
> 当前版本：**v0.4.0**（DDL 已按当前范围交付；DROP 与 v0.3.0 余项暂不开发）。

### 已完成 ✅

- [x] 项目基础架构搭建
- [x] 数据模型定义（TreeNode, ColumnNode, TableNode, AlterTableInfo, CreateTableInfo, DmlLineageInfo）
- [x] 单表 SELECT 解析
- [x] JOIN 查询解析
- [x] 子查询解析（SQLSubqueryTableSource，支持外层 alias.col 下钻）
- [x] CASE WHEN 表达式解析
- [x] 聚合函数解析（SUM, COUNT, AVG, MAX, MIN）
- [x] 函数调用解析（IF, NVL, CONCAT, SUBSTR 等）
- [x] 二元运算表达式解析
- [x] 常量表达式处理
- [x] 单元测试框架
- [x] 测试用例库（87 个 @Test 用例）
- [x] **表达式解析器模块化** - `BaseSqlExprParser` 密封接口 + 11 个独立 ExprParser 文件
- [x] **表源解析器模块化** - `BaseTableSourceParser` 密封接口 + 6 个 TableSourceParser 文件
- [x] **解析上下文** - ExprParseContext（别名映射、目标列、来源列收集、虚拟表下钻）
- [x] **表别名解析** - 自动解析 `a.col1` 到真实表 `t1.col1`
- [x] **单表 FROM 裸列推断** - `SELECT id FROM users` 自动归属为 `users.id`
- [x] **UNION / INTERSECT / EXCEPT** - 按列位置合并左右分支血缘
- [x] **WITH (CTE) 查询** - 预解析 CTE 列血缘并注册为虚拟表，支持链式 CTE
- [x] **嵌套子查询血缘下钻** - FROM 子查询逐层解析到真实底表
- [x] **窗口函数 (OVER)** - PARTITION BY / ORDER BY / SORT BY / DISTRIBUTE BY / CLUSTER BY 列引用全收集
- [x] **LATERAL VIEW** - 行转列输出列映射到 UDTF 输入列
- [x] **CAST 表达式** - 递归下钻到内部表达式
- [x] **ALTER TABLE 解析** - ADD COLUMNS / DROP COLUMN / RENAME COLUMN / MODIFY COLUMN / CHANGE COLUMN
- [x] **INSERT INTO / OVERWRITE 解析** - 目标表 + 显式目标列 + PARTITION + 源 SELECT 血缘
- [x] **CREATE TABLE AS SELECT (CTAS) 解析**
- [x] **CREATE TABLE 纯 DDL 解析** - 表注释、字段、类型、注释、默认值、主键标记与分区字段元信息
- [x] **CREATE VIEW 解析** - 视图定义的列血缘 + 显式列覆盖
- [x] **统一 DML 入口** - `parserDmlSql` 自动识别 INSERT / CTAS / CREATE VIEW
- [x] **血缘持久化模块** - persistence 包（实体、Repository 接口）
- [x] **Neo4j 持久化** - Neo4jLineageRepository（Cypher 预留）
- [x] **Nebula 持久化** - NebulaLineageRepository（nGQL 预留）

### 暂缓 🚫

- [ ] MERGE INTO 语句解析（UPSERT 场景；用户确认不继续开发）
- [ ] 多语句脚本解析（批量 SQL 拆分与依赖；用户确认不继续开发）
- [ ] DROP TABLE / VIEW 识别（用户确认不开发）
- [ ] Java 17 升级后特性的持续应用（pattern matching / sealed interface）

### 缺失功能 ❌

#### 核心能力缺失

| 功能 | 重要性 | 说明 |
|------|--------|------|
| SELECT * 展开 | P1 | 通配符需要元数据支持才能展开 |

#### 输出能力缺失

| 功能 | 重要性 | 说明 |
|------|--------|------|
| 血缘 JSON 导出 | P1 | 标准化输出格式（v0.5.0） |
| DOT 格式导出 | P2 | Graphviz 可视化（v0.5.0） |
| ~~图数据库持久化~~ | ~~P1~~ | ✅ 已完成（Neo4j/Nebula 预留） |
| 影响分析 | P1 | 字段变更影响范围 |

#### 工程能力缺失

| 功能 | 重要性 | 说明 |
|------|--------|------|
| REST API 服务 | P1 | HTTP 接口 |
| CLI 工具 | P1 | 命令行解析工具 |
| 元数据集成 | P2 | Hive Metastore / 自定义元数据 |
| 多方言切换 | P2 | 当前仅 Hive |

---

## 三、版本发布规划

### v0.1.0 - 基础版本

**目标**: 完成核心 SELECT 语句血缘解析

- [x] 单表查询解析
- [x] JOIN 查询解析
- [x] 子查询解析
- [x] 基础表达式解析
- [x] 单元测试覆盖

### v0.2.0 - 查询增强版本 (Current ✅)

**目标**: 在 v0.1.0 基础解析能力之上，引入解析上下文、模块化架构、图数据库持久化预留，完成 UNION / CTE / 窗口函数 / LATERAL VIEW / ALTER 等查询增强能力

#### 已交付 ✅

- [x] **Java 17 升级** - 启用 `sealed interface` / `pattern matching` / `var`
- [x] **ExprParseContext** - 表达式解析上下文（别名映射、当前列、来源列收集、虚拟表下钻）
- [x] **表别名自动解析** - `a.col1` → `t1.col1`
- [x] **单表 FROM 裸列推断** - `SELECT id FROM users` 自动归属为 `users.id`
- [x] **UNION / INTERSECT / EXCEPT** - 递归解析左右分支，按列位置合并血缘
- [x] **WITH (CTE) 查询** - 预解析每个 CTE 列血缘并注册为虚拟表；支持多 CTE 链式引用
- [x] **嵌套子查询血缘下钻** - FROM 子查询自动注册虚拟表，逐层解析到真实底表
- [x] **窗口函数 (OVER)** - SUM / ROW_NUMBER / RANK / LAG / LEAD 等，PARTITION BY / ORDER BY 子句列引用全收集
- [x] **LATERAL VIEW** - 行转列输出列映射到 UDTF 输入列
- [x] **CAST 表达式** - 递归下钻到内部表达式
- [x] **ALTER TABLE 解析** - 通过 `parserAlterTableSql` 解析 ADD/DROP/RENAME COLUMN
- [x] **Expr 解析器模块化** - `BaseSqlExprParser` 密封接口 + 11 个独立 `Sql*ExprParser` 文件
- [x] **TableSource 解析器模块化** - `BaseTableSourceParser` 密封接口 + 6 个 `Sql*TableSourceParser` 文件
- [x] **常量识别** - SQLCharExpr / SQLIntegerExpr / SQLNumberExpr 标记为常量来源
- [x] **持久化实体模型** - LineageNode / LineageEdge / LineageGraph + 工厂方法
- [x] **Repository 接口** - LineageRepository 统一抽象
- [x] **Neo4j / Nebula 预留实现** - Cypher / nGQL 语句已生成
- [x] **调试工具集** - SqlLineageParserDebug / SqlExprParserDebug / TableSourceParserDebug
- [x] **JUnit 5 测试套件** - 71 用例覆盖 SELECT / UNION / CTE / 嵌套子查询 / 窗口函数 / LATERAL VIEW / ALTER（v0.3.0 后增至 78）
- [x] **生产 SQL 回归** - sqlProd01.sql / sqlWindow01.sql / sqlCte01.sql 等复杂 SQL 验证

#### 计划中 📋

- [ ] 嵌套函数深度解析优化
- [ ] SELECT \* 字段展开（需元数据支持）
- [ ] 多语句批量解析

### v0.3.0 - DML 支持版本 (✅)

**目标**: 支持数据操作语句，建立完整数据流（源表 → 目标表）

#### 已交付 ✅

- [x] **INSERT INTO ... SELECT 解析** - 通过 `parserInsertSql` 提取目标表 + 显式目标列 + 源 SELECT 血缘
- [x] **INSERT OVERWRITE 解析** - 复用 INSERT 入口，自动识别 `isOverwrite()`
- [x] **PARTITION 子句提取** - 静态/动态分区列与值收集到 `DmlLineageInfo.partitions`
- [x] **CREATE TABLE AS SELECT (CTAS) 解析** - 通过 `parserCreateTableSql` 解析
- [x] **CREATE VIEW 解析** - 通过 `parserCreateViewSql` 解析视图定义的列血缘，支持显式列覆盖
- [x] **统一 DML 入口** - `parserDmlSql` 自动识别 INSERT / CTAS / CREATE VIEW
- [x] **DML 血缘模型** - `DmlLineageInfo` / `DmlOperation`，含目标列名兜底（显式列 → SELECT alias → 表达式列名）
- [x] **DML 测试用例** - sqlInsert / sqlCtas / sqlView 目录 + 11 个 @Test 用例

#### 暂缓 🚫

- [ ] MERGE INTO 语句解析
- [ ] 多语句批量解析（脚本级 SQL 拆分与依赖）

### v0.4.0 - DDL 支持版本 (Current ✅)

**目标**: 支持表结构定义语句

#### 已交付 ✅

- [x] **CREATE VIEW 解析** - `parserCreateViewSql` 解析视图定义的列血缘，支持显式列覆盖；纳入统一 DML 入口 `parserDmlSql`
- [x] **CREATE TABLE 纯 DDL 解析** - `parserCreateTableDdlSql` 提取表与字段元信息，支持表注释、字段类型、字段注释、默认值、主键标记与分区字段
- [x] **ALTER TABLE 字段变更追踪** - `CHANGE COLUMN` / `MODIFY COLUMN` 统一映射为 `MODIFY` 动作，保留新旧列名、类型与注释
- [x] **DDL 元信息模型** - `CreateTableInfo` / `TableColumnMeta`，与 CTAS 数据血缘模型 `DmlLineageInfo` 分离
- [x] **DDL 测试** - sqlCreateTable / sqlAlter 新增 5 个用例，累计 87 个 @Test 全部通过

#### 暂缓 🚫

- [ ] DROP TABLE / VIEW 语句识别（用户确认不开发）

### v0.5.0 - 输出增强版本

**目标**: 丰富血缘输出能力

- [x] **图数据库持久化接口** - LineageRepository
- [x] **Neo4j 支持** - Neo4jLineageRepository (Cypher 预留)
- [x] **Nebula Graph 支持** - NebulaLineageRepository (nGQL 预留)
- [x] **血缘实体模型** - LineageNode / LineageEdge / LineageGraph
- [x] **上游/下游查询接口** - findUpstream() / findDownstream()
- [ ] JSON 格式标准化输出
- [ ] DOT 格式输出（Graphviz）
- [ ] 血缘合并能力

### v0.6.0 - 多方言版本

**目标**: 支持更多 SQL 方言

- [ ] MySQL 方言支持
- [ ] PostgreSQL 方言支持
- [ ] Spark SQL 方言优化
- [ ] 方言自动检测

### v0.7.0 - 元数据集成版本

**目标**: 与元数据系统集成

- [ ] 元数据接口抽象
- [ ] Hive Metastore 集成
- [ ] SELECT * 字段展开
- [ ] 表/字段不存在校验

### v1.0.0 - 正式版本

**目标**: 生产可用的完整版本

- [ ] REST API 服务
- [ ] CLI 命令行工具
- [ ] 完整文档
- [ ] 性能基准测试
- [ ] 稳定性保证

---

## 四、技术实现建议

### 1. UNION 查询实现

```java
// SQLUnionQueryTableSourceParser 改进
public class SQLUnionQueryTableSourceParser {
    public static List<TableNode> parse(SQLUnionQueryTableSource source) {
        List<TableNode> tables = new ArrayList<>();
        SQLUnionQuery unionQuery = source.getUnion();

        // 递归解析左右分支
        parseUnionBranch(unionQuery.getLeft(), tables);
        parseUnionBranch(unionQuery.getRight(), tables);

        return tables;
    }
}
```

### 2. INSERT 语句支持

```java
// 新增 INSERT 语句解析入口
public static TreeNode<ColumnNode> parseInsertStatement(String sql) {
    SQLStatement stmt = SQLUtils.parseSingleStatement(sql, DbType.hive);
    if (stmt instanceof HiveInsertStatement) {
        HiveInsertStatement insert = (HiveInsertStatement) stmt;
        // 1. 获取目标表
        TableNode targetTable = parseTargetTable(insert.getTableSource());
        // 2. 解析 SELECT 部分血缘
        TreeNode<ColumnNode> selectLineage = parseSelectQuery(insert.getQuery());
        // 3. 建立目标表与源血缘的关联
        return buildInsertLineage(targetTable, selectLineage);
    }
    return null;
}
```

### 3. 血缘输出格式

```json
{
  "targetTable": "dw.user_summary",
  "targetColumns": [
    {
      "name": "user_id",
      "type": "BIGINT",
      "lineage": [
        {
          "sourceTable": "ods.user_info",
          "sourceColumn": "id",
          "transformations": []
        }
      ]
    },
    {
      "name": "total_amount",
      "type": "DECIMAL",
      "lineage": [
        {
          "sourceTable": "ods.order",
          "sourceColumn": "amount",
          "transformations": ["SUM"]
        }
      ]
    }
  ]
}
```

### 4. 窗口函数解析

```java
// 窗口函数表达式处理
case SQLOver over -> {
    // 解析窗口函数主体
    SQLExpr expr = over.getExpr();
    // 解析 PARTITION BY
    List<SQLExpr> partitionBy = over.getPartitionBy();
    // 解析 ORDER BY
    SQLOrderBy orderBy = over.getOrderBy();
    // 构建血缘节点，包含分区和排序字段
}
```

---

## 五、质量保证

### 测试策略

1. **单元测试**: 每个解析器组件独立测试
2. **集成测试**: 完整 SQL 语句端到端测试
3. **回归测试**: 生产 SQL 用例库持续验证
4. **性能测试**: 大 SQL (1000+ 行) 解析性能基准

### 测试用例分类

| 分类 | 数量目标 | 当前状态 |
|------|----------|----------|
| 基础查询 | 20+ | ✅ 已覆盖 |
| JOIN 场景 | 15+ | ✅ 已覆盖 |
| 子查询 | 15+ | ✅ 已覆盖 |
| 函数表达式 | 30+ | ✅ 已覆盖 |
| UNION | 10+ | ✅ 已覆盖 |
| CTE | 10+ | ✅ 已覆盖 |
| 窗口函数 | 10+ | ✅ 已覆盖 |
| LATERAL VIEW | 5+ | ✅ 已覆盖 |
| INSERT | 15+ | ✅ 已覆盖（INSERT INTO / OVERWRITE / PARTITION） |
| CTAS | 5+ | ✅ 已覆盖 |
| CREATE VIEW | 5+ | ✅ 已覆盖（基本 + 显式列覆盖 + 统一入口） |
| ALTER | 5+ | ✅ 已覆盖（ADD / DROP / RENAME / CHANGE / MODIFY） |
| DDL（CREATE TABLE） | 5+ | ✅ 已覆盖（字段元信息 / 分区字段 / CTAS 边界；DROP 暂缓） |
| 生产复杂 SQL | 10+ | ✅ 已覆盖 |

> 当前 @Test 总数：**87**（截至 v0.4.0 纯 CREATE TABLE 与 ALTER 增强）。

---

## 六、参考资源

### 相关项目

- [Apache Atlas](https://atlas.apache.org/) - 元数据管理与血缘
- [DataHub](https://datahubproject.io/) - 元数据平台
- [Sqllineage](https://github.com/reata/sqllineage) - Python SQL 血缘解析
- [JSqlParser](https://github.com/JSQLParser/JSqlParser) - Java SQL 解析器

### 技术文档

- [Druid SQL Parser 文档](https://github.com/alibaba/druid/wiki/SQL-Parser)
- [Hive SQL 语法规范](https://cwiki.apache.org/confluence/display/Hive/LanguageManual)

---

## 七、贡献指南

欢迎贡献代码，请遵循以下规范：

1. Fork 项目并创建特性分支
2. 编写单元测试覆盖新功能
3. 确保所有测试通过: `mvn test`
4. 遵循代码风格规范 (CheckStyle)
5. 提交 Pull Request

---

*文档版本: v1.7*
*更新时间: 2026-08-26*
