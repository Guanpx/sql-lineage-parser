# SQL Lineage Parser

基于 Alibaba Druid 的 SQL 血缘解析工具，用于解析 SQL 语句中的表级和列级数据血缘关系。

## 项目简介

`sql-lineage-parser` 能够解析 SQL 语句（当前主要支持 Hive SQL），提取出字段级别的血缘关系，追踪数据从源表到目标表的流转路径。项目基于 Druid SQL Parser 进行 AST 解析。

### 核心特性

- **字段级血缘追踪**: 精确到列的血缘关系解析
- **复杂表达式支持**: CASE WHEN、聚合函数、嵌套函数等
- **多种表源解析**: 普通表、JOIN、子查询、UNION、CTE
- **树形结构输出**: 直观的血缘树结构，便于遍历和分析
- **高度可扩展**: 模块化设计，易于扩展新语法支持
- **图数据库持久化**: 支持将血缘关系写入 Neo4j / Nebula Graph

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

```java
import com.magic.core.parser.SqlLineageParser;
import com.magic.sqllineageparser.model.ColumnNode;
import com.magic.sqllineageparser.model.TreeNode;

public class Example {
    public static void main(String[] args) {
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

        // 遍历血缘树
        lineageTree.getChildren().forEach(child -> {
            ColumnNode column = child.getValue();
            System.out.println("输出列: " + column.getAlias());
            System.out.println("来源: " + column.getSourceColumns());
            System.out.println("---");
        });
    }
}
```

### 血缘树结构说明

```
ROOT (虚拟根节点)
├── Column: id
│   └── Source: orders.id
├── Column: name
│   └── Source: users.name
└── Column: total (SUM)
    └── Source: orders.amount
```

## 项目结构

```
sql-lineage-parser/
├── src/main/java/com/magic/
│   ├── core/
│   │   ├── parser/
│   │   │   ├── SqlLineageParser.java           # 血缘解析核心入口
│   │   │   └── sql/
│   │   │       ├── table/                      # 表源解析器
│   │   │       │   ├── BaseTableSourceParser.java
│   │   │       │   ├── SqlExprTableSourceParser.java
│   │   │       │   ├── SqlJoinTableSourceParser.java
│   │   │       │   ├── SqlSubqueryTableSourceParser.java
│   │   │       │   ├── SqlUnionQueryTableSourceParser.java
│   │   │       │   └── SqlWithSubqueryTableSourceParser.java
│   │   │       └── expr/                       # 表达式解析器
│   │   │           ├── BaseSqlExprParser.java
│   │   │           ├── ExprParseContext.java   # 解析上下文
│   │   │           ├── SqlCaseExprParser.java
│   │   │           ├── SqlAggregateExprParser.java
│   │   │           ├── SqlMethodInvokeExprParser.java
│   │   │           ├── SqlBinaryOpExprParser.java
│   │   │           ├── SqlPropertyExprParser.java
│   │   │           ├── SqlIdentifierExprParser.java
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
│       └── ColumnNode.java                     # 列节点模型
├── src/test/                                   # 测试代码
├── sqls/                                       # SQL 测试用例库
│   ├── sqlCase/                                # CASE WHEN 语句
│   ├── sqlFunction/                            # 函数类语句
│   ├── sqlJoin/                                # JOIN 语句
│   ├── sqlProd/                                # 生产级复杂 SQL
│   └── sqlUnion/                               # UNION 语句
├── RELEASE.md                                  # 版本规划文档
├── UPDATE_CLAUDE.md                            # 变更记录
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

### 支持的表源类型

| 表源 | 示例 | 状态 |
|------|------|------|
| 普通表 | `FROM users u` | ✅ |
| JOIN | `LEFT JOIN orders o ON ...` | ✅ |
| 子查询 | `FROM (SELECT ...) t` | ✅ |
| UNION | `SELECT ... UNION SELECT ...` | 🚧 |
| CTE | `WITH t AS (...) SELECT ...` | 🚧 |

## 支持的 SQL 类型

| 类型 | 状态 | 说明 |
|------|------|------|
| 单表 SELECT | ✅ 已支持 | 完整支持 |
| JOIN 查询 | ✅ 已支持 | LEFT/RIGHT/INNER/FULL JOIN |
| 子查询 | ✅ 已支持 | 派生表、标量子查询 |
| CASE WHEN | ✅ 已支持 | 简单和搜索型 CASE |
| 聚合函数 | ✅ 已支持 | SUM/COUNT/AVG/MAX/MIN |
| 内置函数 | ✅ 已支持 | IF/NVL/CONCAT/SUBSTR 等 |
| UNION 查询 | 🚧 开发中 | 框架已建立 |
| CTE (WITH) | 🚧 开发中 | 框架已建立 |
| INSERT 语句 | 📋 计划中 | 目标表血缘 |
| CREATE 语句 | 📋 计划中 | DDL 支持 |

## 版本规划

详见 [RELEASE.md](./RELEASE.md)

| 版本 | 目标 | 状态 |
|------|------|------|
| v0.1.0 | 基础 SELECT 解析 | ✅ 当前 |
| v0.2.0 | UNION/CTE/窗口函数 | 📋 计划中 |
| v0.3.0 | INSERT/CTAS 支持 | 📋 计划中 |
| v0.4.0 | DDL 支持 | 📋 计划中 |
| v0.5.0 | 血缘输出增强 | 📋 计划中 |
| v1.0.0 | 生产就绪版本 | 📋 计划中 |

## 开发状态

项目处于早期开发阶段。当前已具备：

- ✅ 完整的核心解析框架
- ✅ 50+ 单元测试用例
- ✅ 生产级复杂 SQL 验证
- ✅ 模块化可扩展架构

待完善：

- UNION 和 CTE 完整实现
- INSERT/DDL 语句支持
- 多 SQL 方言支持
- REST API / CLI 工具

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
