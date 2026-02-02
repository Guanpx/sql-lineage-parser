# SQL Lineage Parser

基于 Alibaba Druid 的 SQL 血缘解析工具，用于解析 SQL 语句中的表级和列级数据血缘关系。

## 项目简介

`sql-lineage-parser` 能够解析 SQL 语句（当前主要支持 Hive SQL），提取出字段级别的血缘关系，追踪数据从源表到目标表的流转路径。项目采用 Scala + Java 混合编程，基于 Druid SQL Parser 进行 AST 解析。

## 技术栈

- **语言**: Scala 2.12 + Java 8
- **SQL 解析引擎**: Alibaba Druid 1.2.20
- **构建工具**: Maven
- **辅助工具**: Lombok

## 项目结构

```
sql-lineage-parser/
├── src/
│   ├── main/
│   │   ├── java/com/magic/sqllineageparser/model/
│   │   │   ├── TreeNode.java          # 通用树结构节点
│   │   │   ├── TableNode.java         # 表节点模型
│   │   │   └── ColumnNode.java        # 列节点模型
│   │   └── scala/com/magic/core/
│   │       ├── parser/
│   │       │   ├── SqlLineageParser.scala       # 血缘解析核心入口
│   │       │   └── sql/
│   │       │       ├── table/                   # 表源解析器
│   │       │       │   ├── BaseTableSourceParser.scala
│   │       │       │   ├── SQLExprTableSourceParser.java
│   │       │       │   ├── SQLJoinTableSourceParser.java
│   │       │       │   ├── SQLSubqueryTableSourceParser.java
│   │       │       │   ├── SQLUnionQueryTableSourceParser.scala
│   │       │       │   └── SQLWithSubqueryTableSourceParser.scala
│   │       │       └── expr/                    # 表达式解析器
│   │       │           ├── BaseSqlExprParser.scala
│   │       │           └── SQLMethodInvokeExprParser.scala
│   │       └── utils/
│   │           └── StringUtils.scala
│   └── test/
│       ├── java/ut/TestAll.java
│       └── scala/ut/TestSingleSql01.scala
├── sqls/                              # SQL 测试用例
│   ├── sqlAlter/                      # ALTER 语句
│   ├── sqlCase/                       # CASE WHEN 语句
│   ├── sqlFunction/                   # 函数类语句
│   ├── sqlJoin/                       # JOIN 语句
│   ├── sqlProd/                       # 生产级复杂 SQL
│   ├── sqlSelect/                     # SELECT 语句
│   └── sqlUnion/                      # UNION 语句
├── pom.xml
└── CheckStyle.xml
```

## 核心模块

### 数据模型

- **TreeNode**: 通用泛型树结构，支持父子关系、层高计算、子树大小统计等操作
- **TableNode**: 表节点，包含 schema、表名、别名、是否虚拟表、字段列表等信息
- **ColumnNode**: 列节点，包含列名、别名、来源列、所属表、表达式、是否常量等信息

### 解析引擎

- **SqlLineageParser**: 解析入口，负责将 SQL 字符串解析为血缘树。支持解析以下 SQL 表达式类型：
  - `SQLPropertyExpr` - 表.列 表达式
  - `SQLCaseExpr` - CASE WHEN 表达式
  - `SQLAggregateExpr` - 聚合函数（SUM、COUNT 等）
  - `SQLMethodInvokeExpr` - 函数调用
  - `SQLBinaryOpExpr` - 二元运算/比较表达式
  - `SQLIdentifierExpr` - 列标识符
  - `SQLNumberExpr` / `SQLIntegerExpr` - 数值常量
  - `SQLCharExpr` - 字符常量

- **表源解析器（TableSourceParser）**: 处理不同的表来源类型：
  - 普通表（`SQLExprTableSource`）
  - JOIN 表（`SQLJoinTableSource`）
  - 子查询（`SQLSubqueryTableSource`）
  - UNION 查询（`SQLUnionQueryTableSource`）
  - WITH 子查询（`SQLWithSubqueryTableSource`）

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.x
- Scala 2.12

### 构建

```bash
mvn clean package
```

### 使用示例

**Scala 调用：**

```scala
import com.magic.core.parser.SqlLineageParser

val sql = "SELECT a.id, b.name FROM table_a a JOIN table_b b ON a.id = b.id"
val lineageTree = SqlLineageParser.parserSingleSelectSql(sql)
```

**Java 调用：**

```java
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;

String sql = "SELECT a.id, b.name FROM table_a a JOIN table_b b ON a.id = b.id";
SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, "mysql");
SQLStatement statement = parser.parseStatement();

SchemaStatVisitor visitor = new SchemaStatVisitor();
statement.accept(visitor);
```

## 支持的 SQL 类型

| 类型 | 状态 |
|------|------|
| 单表 SELECT | 已支持 |
| JOIN 查询 | 已支持 |
| 子查询 | 已支持 |
| CASE WHEN | 已支持 |
| 聚合函数 | 已支持 |
| 内置函数 | 已支持 |
| UNION 查询 | 开发中 |
| ALTER 语句 | 开发中 |
| CREATE 语句 | 计划中 |

## 开发状态

项目处于早期开发阶段，部分表源解析器和表达式解析器仍在完善中。

## License

未指定
