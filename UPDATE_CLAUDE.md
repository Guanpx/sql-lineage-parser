# UPDATE_CLAUDE.md - 变更记录

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
