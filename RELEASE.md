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
| ALTER TABLE | 字段变更追踪（ADD/DROP/RENAME COLUMN） | P2 |
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

### 已完成 ✅

- [x] 项目基础架构搭建
- [x] 数据模型定义（TreeNode, ColumnNode, TableNode）
- [x] 单表 SELECT 解析
- [x] JOIN 查询解析
- [x] 子查询解析（SQLSubqueryTableSource）
- [x] CASE WHEN 表达式解析
- [x] 聚合函数解析（SUM, COUNT, AVG, MAX, MIN）
- [x] 函数调用解析（IF, NVL, CONCAT, SUBSTR 等）
- [x] 二元运算表达式解析
- [x] 常量表达式处理
- [x] 单元测试框架
- [x] 测试用例库（50+ 用例）

### 开发中 🚧

- [ ] UNION 查询解析（框架已建立，实现返回 null）
- [ ] WITH 子查询/CTE 解析（框架已建立，待实现）
- [ ] ALTER 语句解析

### 缺失功能 ❌

#### 核心能力缺失

| 功能 | 重要性 | 说明 |
|------|--------|------|
| INSERT 语句解析 | P0 | 无法追踪目标表，血缘不完整 |
| CTAS 语句解析 | P0 | CREATE TABLE AS SELECT 不支持 |
| 窗口函数解析 | P1 | ROW_NUMBER, RANK, LAG 等 |
| LATERAL VIEW | P1 | Hive 行转列场景常用 |
| SELECT * 展开 | P1 | 通配符需要元数据支持才能展开 |
| 多语句解析 | P1 | 批量 SQL 脚本 |

#### 输出能力缺失

| 功能 | 重要性 | 说明 |
|------|--------|------|
| 血缘 JSON 导出 | P1 | 标准化输出格式 |
| DOT 格式导出 | P2 | Graphviz 可视化 |
| 血缘图遍历 API | P1 | 上下游查询接口 |
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

### v0.1.0 - 基础版本 (Current)

**目标**: 完成核心 SELECT 语句血缘解析

- [x] 单表查询解析
- [x] JOIN 查询解析
- [x] 子查询解析
- [x] 基础表达式解析
- [x] 单元测试覆盖

### v0.2.0 - 查询增强版本

**目标**: 完善 SELECT 语句的所有场景

- [ ] UNION/INTERSECT/EXCEPT 查询支持
- [ ] WITH 子查询（CTE）完整支持
- [ ] 窗口函数解析
- [ ] LATERAL VIEW 支持
- [ ] 嵌套函数深度解析优化
- [ ] 完善测试用例覆盖

### v0.3.0 - DML 支持版本

**目标**: 支持数据操作语句，建立完整数据流

- [ ] INSERT INTO ... SELECT 解析
- [ ] INSERT OVERWRITE 解析
- [ ] CREATE TABLE AS SELECT 解析
- [ ] MERGE INTO 语句解析
- [ ] 多语句批量解析

### v0.4.0 - DDL 支持版本

**目标**: 支持表结构定义语句

- [ ] CREATE TABLE 解析（提取字段元信息）
- [ ] CREATE VIEW 解析
- [ ] ALTER TABLE 字段变更追踪
- [ ] DROP 语句识别

### v0.5.0 - 输出增强版本

**目标**: 丰富血缘输出能力

- [ ] JSON 格式标准化输出
- [ ] DOT 格式输出（Graphviz）
- [ ] 血缘图遍历 API
- [ ] 上游追溯 / 下游影响分析
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
| UNION | 10+ | 🚧 部分 |
| CTE | 10+ | 🚧 部分 |
| INSERT | 15+ | ❌ 待添加 |
| DDL | 20+ | ❌ 待添加 |
| 生产复杂 SQL | 10+ | ✅ 已覆盖 |

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

*文档版本: v1.0*
*更新时间: 2026-02-03*
