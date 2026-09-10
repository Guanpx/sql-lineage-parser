package com.magic.core.parser;

import com.magic.core.util.SqlFileReader;
import com.magic.sqllineageparser.model.ColumnNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SqlLineageParser 单元测试
 *
 * @author Test
 */
@DisplayName("SqlLineageParser 测试")
class SqlLineageParserTest {

    @Nested
    @DisplayName("基础解析测试")
    class BasicParseTest {

        @Test
        @DisplayName("解析空SQL应返回null")
        void testParseEmptySql() {
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(null);
            assertNull(result);

            result = SqlLineageParser.parserSingleSelectSql("");
            assertNull(result);
        }

        @Test
        @DisplayName("解析简单SELECT语句")
        void testParseSimpleSelect() {
            String sql = "SELECT id, name FROM users";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("解析带别名的SELECT语句")
        void testParseSelectWithAlias() {
            String sql = "SELECT id AS user_id, name AS user_name FROM users u";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result);
            assertEquals(2, result.size());

            ColumnNode firstColumn = result.get(0);
            assertNotNull(firstColumn);
            assertEquals("user_id", firstColumn.getAlias());
        }
    }

    @Nested
    @DisplayName("CASE WHEN 表达式测试")
    class CaseWhenTest {

        @Test
        @DisplayName("解析CASE WHEN语句 - sqlcase1.sql")
        void testParseCaseWhenFromFile() {
            String sql = SqlFileReader.readCaseSql("sqlcase1.sql");
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result);
            // CASE WHEN + u.rere + u.* = 3个字段
            assertTrue(result.size() >= 1);
        }

        @Test
        @DisplayName("解析带子查询的SELECT - sqlCase02.sql")
        void testParseCaseWithSubquery() {
            String sql = SqlFileReader.readCaseSql("sqlCase02.sql");
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result);
            assertEquals(5, result.size());
        }

        @Test
        @DisplayName("解析带反引号别名的SELECT - sqlCase03.sql")
        void testParseCaseWithBacktickAlias() {
            String sql = SqlFileReader.readCaseSql("sqlCase03.sql");
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result);
        }

        @Test
        @DisplayName("简单 CASE 的操作数列纳入血缘 (P1 回归)")
        void testSimpleCaseOperandCollected() {
            String sql = "SELECT CASE dept WHEN 1 THEN 'a' ELSE 'b' END AS c FROM emp";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);
            assertNotNull(result);
            ColumnNode col = result.get(0);
            // CASE 操作数 dept 必须作为来源列被收集
            assertTrue(col.getSourceColumns().stream()
                            .anyMatch(s -> "emp".equals(s.getTableName()) && "dept".equals(s.getName())),
                    "简单 CASE 操作数 dept 应作为来源列");
        }

        @Test
        @DisplayName("搜索型 CASE 的 WHEN 条件列纳入血缘 (P1 回归)")
        void testSearchedCaseConditionCollected() {
            String sql = "SELECT CASE WHEN status='X' THEN amount ELSE 0 END AS c FROM orders";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);
            assertNotNull(result);
            ColumnNode col = result.get(0);
            // WHEN 条件列 status 与 THEN 取值列 amount 都应被收集
            assertTrue(col.getSourceColumns().stream()
                            .anyMatch(s -> "orders".equals(s.getTableName()) && "status".equals(s.getName())),
                    "WHEN 条件列 status 应作为来源列");
            assertTrue(col.getSourceColumns().stream()
                            .anyMatch(s -> "orders".equals(s.getTableName()) && "amount".equals(s.getName())),
                    "THEN 取值列 amount 应作为来源列");
        }
    }

    @Nested
    @DisplayName("JOIN 查询测试")
    class JoinQueryTest {

        @Test
        @DisplayName("解析简单LEFT JOIN - sqlJoin01.sql")
        void testParseSimpleLeftJoin() {
            String sql = SqlFileReader.readJoinSql("sqlJoin01.sql");
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result);
            assertEquals(6, result.size());
        }

        @Test
        @DisplayName("解析带子查询的LEFT JOIN - sqlJoin02.sql")
        void testParseJoinWithSubquery() {
            String sql = SqlFileReader.readJoinSql("sqlJoin02.sql");
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result);
            assertEquals(8, result.size());
        }
    }

    @Nested
    @DisplayName("函数表达式测试")
    class FunctionExprTest {

        @Test
        @DisplayName("解析IF函数 - sqlMaxIfNvlFunc01.sql")
        void testParseIfFunction() {
            String sql = SqlFileReader.readFunctionSql("sqlMaxIfNvlFunc01.sql");
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("解析MAX+IF嵌套函数 - sqlMaxIfNvlFunc02.sql")
        void testParseMaxIfFunction() {
            String sql = SqlFileReader.readFunctionSql("sqlMaxIfNvlFunc02.sql");
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("解析NVL函数 - sqlMaxIfNvlFunc03.sql")
        void testParseNvlFunction() {
            String sql = SqlFileReader.readFunctionSql("sqlMaxIfNvlFunc03.sql");
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("UNION 查询测试")
    class UnionQueryTest {

        @Test
        @DisplayName("解析UNION查询 - sqlUnion01.sql")
        void testParseUnionQuery() {
            String sql = SqlFileReader.readUnionSql("sqlUnion01.sql");
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);
            assertNotNull(result, "UNION 查询应返回血缘树");
            // 左右分支各 5 列, 按位置合并后输出 5 列
            assertEquals(5, result.size());
            // 第二列 order_id / invoice_id 的来源应同时包含两个分支
            ColumnNode secondCol = result.get(1);
            assertTrue(secondCol.getSourceColumns().size() >= 2,
                    "UNION 第二列应合并来自 orders 和 invoices 的来源");
        }

        @Test
        @DisplayName("解析内联 UNION ALL")
        void testParseUnionAll() {
            String sql = "SELECT a FROM t1 UNION ALL SELECT b FROM t2 UNION ALL SELECT c FROM t3";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);
            assertNotNull(result);
            assertEquals(1, result.size());
            ColumnNode col = result.get(0);
            // 3 个分支的来源都应被合并
            assertEquals(3, col.getSourceColumns().size());
        }
    }

    @Nested
    @DisplayName("CTE / WITH 查询测试")
    class CteQueryTest {

        @Test
        @DisplayName("解析单个 CTE - 列血缘下钻到真实表")
        void testParseSingleCte() {
            String sql = "WITH cte1 AS (SELECT id AS uid, name FROM users) " +
                    "SELECT cte1.uid, cte1.name FROM cte1";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);
            assertNotNull(result);
            assertEquals(2, result.size());
            ColumnNode firstCol = result.get(0);
            // 应下钻到 users.id（不再是 cte1.uid）
            assertEquals(1, firstCol.getSourceColumns().size());
            assertEquals("users", firstCol.getSourceColumns().get(0).getTableName());
            assertEquals("id", firstCol.getSourceColumns().get(0).getName());
        }

        @Test
        @DisplayName("解析多个 CTE 链式引用")
        void testParseChainedCte() {
            String sql = "WITH a AS (SELECT id FROM t1), " +
                    "     b AS (SELECT id FROM a) " +
                    "SELECT b.id FROM b";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);
            assertNotNull(result);
            ColumnNode col = result.get(0);
            assertEquals(1, col.getSourceColumns().size());
            assertEquals("t1", col.getSourceColumns().get(0).getTableName());
        }

        @Test
        @DisplayName("解析 CTE SQL 文件 - sqlCte01.sql")
        void testCteFromFile() {
            String sql = SqlFileReader.readSelectSql("sqlCte01.sql");
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);
            assertNotNull(result);
            // 输出 4 个字段
            assertEquals(4, result.size());
            // user_name 来源应包含 users.name
            ColumnNode userNameCol = result.get(1);
            assertTrue(userNameCol.getSourceColumns().stream()
                    .anyMatch(s -> "users".equals(s.getTableName()) && "name".equals(s.getName())));
        }
    }

    @Nested
    @DisplayName("嵌套子查询血缘下钻测试")
    class NestedSubqueryTest {

        @Test
        @DisplayName("子查询作为表源时, 外层引用应下钻到真实表")
        void testNestedSubqueryDrillDown() {
            String sql = "SELECT t.uid FROM (SELECT id AS uid FROM users) t";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);
            assertNotNull(result);
            ColumnNode col = result.get(0);
            assertEquals(1, col.getSourceColumns().size());
            assertEquals("users", col.getSourceColumns().get(0).getTableName());
            assertEquals("id", col.getSourceColumns().get(0).getName());
        }
    }

    @Nested
    @DisplayName("窗口函数测试")
    class WindowFunctionTest {

        @Test
        @DisplayName("解析 ROW_NUMBER OVER")
        void testRowNumberOver() {
            String sql = "SELECT ROW_NUMBER() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rn FROM emp";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);
            assertNotNull(result);
            ColumnNode col = result.get(0);
            // PARTITION BY dept_id, ORDER BY salary 都应作为来源列
            assertTrue(col.getSourceColumns().size() >= 2);
        }

        @Test
        @DisplayName("解析 SUM OVER 窗口聚合")
        void testSumOver() {
            String sql = "SELECT user_id, SUM(amount) OVER (PARTITION BY user_id) AS total FROM orders";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);
            assertNotNull(result);
            ColumnNode totalCol = result.get(1);
            // amount 和 user_id 都应被收集
            assertTrue(totalCol.getSourceColumns().size() >= 2);
        }

        @Test
        @DisplayName("解析窗口函数 SQL 文件 - sqlWindow01.sql")
        void testWindowSqlFromFile() {
            String sql = SqlFileReader.readFunctionSql("sqlWindow01.sql");
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);
            assertNotNull(result);
            // 应解析出 7 个输出列
            assertEquals(7, result.size());
        }
    }

    @Nested
    @DisplayName("LATERAL VIEW 测试")
    class LateralViewTest {

        @Test
        @DisplayName("LATERAL VIEW explode 输出列下钻到方法参数")
        void testLateralViewExplode() {
            String sql = "SELECT v.item FROM mytable t LATERAL VIEW explode(t.arr) v AS item";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);
            assertNotNull(result);
            ColumnNode col = result.get(0);
            assertEquals(1, col.getSourceColumns().size());
            assertEquals("arr", col.getSourceColumns().get(0).getName());
        }
    }

    @Nested
    @DisplayName("ALTER TABLE 测试")
    class AlterTableTest {

        @Test
        @DisplayName("ADD COLUMNS 解析")
        void testAlterAddColumns() {
            String sql = "ALTER TABLE fce_stock.dest_test ADD COLUMNS (first_code string COMMENT '首code')";
            var info = SqlLineageParser.parserAlterTableSql(sql);
            assertNotNull(info);
            assertEquals("fce_stock", info.getSchema());
            assertEquals("dest_test", info.getTableName());
            assertEquals(1, info.getChanges().size());
            var change = info.getChanges().get(0);
            assertEquals(com.magic.sqllineageparser.model.AlterColumnChange.Action.ADD, change.getAction());
            assertEquals("first_code", change.getColumnName());
            assertEquals("首code", change.getComment());
        }

        @Test
        @DisplayName("从用例文件解析 sqlAlter01.sql")
        void testAlterFromFile() {
            String sql = SqlFileReader.readAlterSql("sqlAlter01.sql");
            var info = SqlLineageParser.parserAlterTableSql(sql);
            assertNotNull(info);
            assertNotNull(info.getTableName());
            assertFalse(info.getChanges().isEmpty());
        }

        @Test
        @DisplayName("CHANGE COLUMN 映射为 MODIFY 并保留新旧列名")
        void testAlterChangeColumn() {
            String sql = SqlFileReader.readAlterSql("sqlAlterChangeColumn01.sql");
            var info = SqlLineageParser.parserAlterTableSql(sql);

            assertNotNull(info);
            assertEquals("dw.orders", info.getQualifiedTableName());
            assertEquals(1, info.getChanges().size());

            var change = info.getChanges().get(0);
            assertEquals(com.magic.sqllineageparser.model.AlterColumnChange.Action.MODIFY,
                    change.getAction());
            assertEquals("amount", change.getColumnName());
            assertEquals("order_amount", change.getNewColumnName());
            assertEquals("DECIMAL(18, 2)", change.getDataType());
            assertEquals("订单金额", change.getComment());
        }

        @Test
        @DisplayName("MODIFY COLUMN 解析为 MODIFY 动作")
        void testAlterModifyColumn() {
            String sql = SqlFileReader.readAlterSql("sqlAlterModifyColumn01.sql");
            var info = SqlLineageParser.parserAlterTableSql(sql);

            assertNotNull(info);
            assertEquals("dw.orders", info.getQualifiedTableName());
            assertEquals(1, info.getChanges().size());

            var change = info.getChanges().get(0);
            assertEquals(com.magic.sqllineageparser.model.AlterColumnChange.Action.MODIFY,
                    change.getAction());
            assertEquals("amount", change.getColumnName());
            assertNull(change.getNewColumnName());
            assertEquals("DECIMAL(18, 2)", change.getDataType());
            assertEquals("订单金额", change.getComment());
        }
    }

    @Nested
    @DisplayName("CREATE TABLE 纯 DDL 测试")
    class CreateTableDdlTest {

        @Test
        @DisplayName("解析 CREATE TABLE 表字段元信息")
        void testCreateTableColumnMetadata() {
            String sql = SqlFileReader.readCreateTableSql("sqlCreateTable01.sql");
            var info = SqlLineageParser.parserCreateTableDdlSql(sql);

            assertNotNull(info);
            assertEquals("dw", info.getSchema());
            assertEquals("user_profile", info.getTableName());
            assertEquals("dw.user_profile", info.getQualifiedTableName());
            assertEquals("用户画像表", info.getComment());
            assertEquals(3, info.getColumns().size());

            var userId = info.getColumns().get(0);
            assertEquals("user_id", userId.getColumnName());
            assertEquals("BIGINT", userId.getDataType());
            assertEquals("用户ID", userId.getComment());

            var amount = info.getColumns().get(2);
            assertEquals("DECIMAL(18, 2)", amount.getDataType());
            assertEquals("0", amount.getDefaultValue());
        }

        @Test
        @DisplayName("解析 CREATE TABLE 分区字段")
        void testCreateTablePartitionColumns() {
            String sql = SqlFileReader.readCreateTableSql("sqlCreateTable01.sql");
            var info = SqlLineageParser.parserCreateTableDdlSql(sql);

            assertNotNull(info);
            assertEquals(2, info.getPartitionColumns().size());
            assertEquals("dt", info.getPartitionColumns().get(0).getColumnName());
            assertEquals("STRING", info.getPartitionColumns().get(0).getDataType());
            assertEquals("业务日期", info.getPartitionColumns().get(0).getComment());
        }

        @Test
        @DisplayName("CTAS 不走纯 DDL 元信息入口")
        void testCreateTableDdlRejectsCtas() {
            String sql = "CREATE TABLE t AS SELECT id FROM users";
            assertNull(SqlLineageParser.parserCreateTableDdlSql(sql));
        }
    }

    @Nested
    @DisplayName("INSERT 语句测试")
    class InsertStatementTest {

        @Test
        @DisplayName("INSERT INTO ... SELECT 提取目标表与目标列")
        void testInsertIntoSelect() {
            String sql = SqlFileReader.readInsertSql("sqlInsert01.sql");
            var info = SqlLineageParser.parserInsertSql(sql);
            assertNotNull(info);
            assertEquals(com.magic.sqllineageparser.model.DmlOperation.INSERT_INTO, info.getOperation());
            assertEquals("dw", info.getTargetSchema());
            assertEquals("user_summary", info.getTargetTable());
            assertEquals(List.of("user_id", "user_name", "total_amount"), info.getTargetColumns());
            assertNotNull(info.getOutputColumns());
            assertEquals(3, info.getOutputColumnCount());
            // 第 1 列 user_id 应来自 users.id
            ColumnNode firstSource = info.getOutputColumns().get(0);
            assertTrue(firstSource.getSourceColumns().stream()
                    .anyMatch(s -> "users".equals(s.getTableName()) && "id".equals(s.getName())));
        }

        @Test
        @DisplayName("INSERT OVERWRITE 带 PARTITION 解析")
        void testInsertOverwriteWithPartition() {
            String sql = SqlFileReader.readInsertSql("sqlInsertOverwrite01.sql");
            var info = SqlLineageParser.parserInsertSql(sql);
            assertNotNull(info);
            assertEquals(com.magic.sqllineageparser.model.DmlOperation.INSERT_OVERWRITE, info.getOperation());
            assertEquals("dw", info.getTargetSchema());
            assertEquals("daily_order", info.getTargetTable());
            assertEquals(1, info.getPartitions().size());
            assertEquals("'2026-05-20'", info.getPartitions().get("dt"));
            assertEquals(4, info.getOutputColumnCount());
        }

        @Test
        @DisplayName("INSERT INTO 无显式列时 targetColumns 为空, 回退到 alias")
        void testInsertWithoutExplicitColumns() {
            String sql = "INSERT INTO dw.t SELECT a.id AS uid, a.name FROM users a";
            var info = SqlLineageParser.parserInsertSql(sql);
            assertNotNull(info);
            assertTrue(info.getTargetColumns().isEmpty());
            assertEquals("uid", info.getTargetColumnAt(0));
            assertEquals("name", info.getTargetColumnAt(1));
        }

        @Test
        @DisplayName("统一 DML 入口识别 INSERT 语句")
        void testUnifiedDmlEntryInsert() {
            String sql = "INSERT INTO t SELECT id FROM users";
            var info = SqlLineageParser.parserDmlSql(sql);
            assertNotNull(info);
            assertEquals(com.magic.sqllineageparser.model.DmlOperation.INSERT_INTO, info.getOperation());
        }
    }

    @Nested
    @DisplayName("CREATE TABLE AS SELECT 测试")
    class CtasTest {

        @Test
        @DisplayName("解析 CTAS SQL 文件")
        void testCtasFromFile() {
            String sql = SqlFileReader.readCtasSql("sqlCtas01.sql");
            var info = SqlLineageParser.parserCreateTableSql(sql);
            assertNotNull(info);
            assertEquals(com.magic.sqllineageparser.model.DmlOperation.CTAS, info.getOperation());
            assertEquals("dw", info.getTargetSchema());
            assertEquals("top_users", info.getTargetTable());
            assertEquals(3, info.getOutputColumnCount());
            // 无显式列定义时, getTargetColumnAt 应回退到 alias
            assertEquals("user_id", info.getTargetColumnAt(0));
            assertEquals("user_name", info.getTargetColumnAt(1));
            assertEquals("total_amount", info.getTargetColumnAt(2));
            // total_amount 来源应包含 orders.amount
            ColumnNode totalCol = info.getOutputColumns().get(2);
            assertTrue(totalCol.getSourceColumns().stream()
                    .anyMatch(s -> "orders".equals(s.getTableName()) && "amount".equals(s.getName())));
        }

        @Test
        @DisplayName("非 CTAS 的 CREATE TABLE 返回 null")
        void testNonCtasReturnsNull() {
            String sql = "CREATE TABLE t (id int, name string)";
            var info = SqlLineageParser.parserCreateTableSql(sql);
            assertNull(info);
        }

        @Test
        @DisplayName("统一 DML 入口识别 CTAS 语句")
        void testUnifiedDmlEntryCtas() {
            String sql = "CREATE TABLE t AS SELECT id FROM users";
            var info = SqlLineageParser.parserDmlSql(sql);
            assertNotNull(info);
            assertEquals(com.magic.sqllineageparser.model.DmlOperation.CTAS, info.getOperation());
        }
    }

    @Nested
    @DisplayName("CREATE VIEW 测试")
    class CreateViewTest {

        @Test
        @DisplayName("解析 CREATE VIEW ... AS SELECT 提取目标表与目标列")
        void testCreateViewBasic() {
            String sql = SqlFileReader.readViewSql("sqlCreateView01.sql");
            var info = SqlLineageParser.parserCreateViewSql(sql);
            assertNotNull(info);
            assertEquals(com.magic.sqllineageparser.model.DmlOperation.CREATE_VIEW, info.getOperation());
            assertEquals("dw", info.getTargetSchema());
            assertEquals("v_active_user", info.getTargetTable());
            // 无显式列覆盖时 targetColumns 为空, 回退到 SELECT alias
            assertTrue(info.getTargetColumns().isEmpty());
            assertEquals(3, info.getOutputColumnCount());
            assertEquals("user_id", info.getTargetColumnAt(0));
            assertEquals("user_name", info.getTargetColumnAt(1));
            assertEquals("total_amount", info.getTargetColumnAt(2));
            // total_amount 应来自 orders.amount
            ColumnNode totalCol = info.getOutputColumns().get(2);
            assertTrue(totalCol.getSourceColumns().stream()
                    .anyMatch(s -> "orders".equals(s.getTableName()) && "amount".equals(s.getName())));
        }

        @Test
        @DisplayName("CREATE VIEW 显式列覆盖时按位置对齐 SELECT")
        void testCreateViewExplicitColumns() {
            String sql = SqlFileReader.readViewSql("sqlCreateView02.sql");
            var info = SqlLineageParser.parserCreateViewSql(sql);
            assertNotNull(info);
            assertEquals(com.magic.sqllineageparser.model.DmlOperation.CREATE_VIEW, info.getOperation());
            assertEquals("dw", info.getTargetSchema());
            assertEquals("v_order_summary", info.getTargetTable());
            assertEquals(List.of("uid", "total"), info.getTargetColumns());
            assertEquals(2, info.getOutputColumnCount());
            // 第 2 列 total 应来自 orders.amount
            ColumnNode totalCol = info.getOutputColumns().get(1);
            assertTrue(totalCol.getSourceColumns().stream()
                    .anyMatch(s -> "orders".equals(s.getTableName()) && "amount".equals(s.getName())));
        }

        @Test
        @DisplayName("统一 DML 入口识别 CREATE VIEW 语句")
        void testUnifiedDmlEntryCreateView() {
            String sql = "CREATE VIEW dw.v1 AS SELECT id FROM users";
            var info = SqlLineageParser.parserDmlSql(sql);
            assertNotNull(info);
            assertEquals(com.magic.sqllineageparser.model.DmlOperation.CREATE_VIEW, info.getOperation());
            assertEquals("v1", info.getTargetTable());
        }

        @Test
        @DisplayName("非 CREATE VIEW 语句返回 null")
        void testNonCreateViewReturnsNull() {
            String sql = "SELECT id, name FROM users";
            var info = SqlLineageParser.parserCreateViewSql(sql);
            assertNull(info);
        }
    }

    @Nested
    @DisplayName("复杂生产SQL测试")
    class ProductionSqlTest {

        @Test
        @DisplayName("解析复杂生产SQL - sqlProd01.sql")
        void testParseComplexProductionSql() {
            String sql = SqlFileReader.readProdSql("sqlProd01.sql");
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result);
            // 复杂SQL有多个输出字段
            assertTrue(result.size() > 10);
        }
    }

    @Nested
    @DisplayName("表达式类型测试")
    class ExpressionTypeTest {

        @Test
        @DisplayName("解析SQLPropertyExpr - 表.列表达式")
        void testParseSQLPropertyExpr() {
            String sql = "SELECT t.id, t.name FROM table_a t";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("解析SQLIdentifierExpr - 单列标识符")
        void testParseSQLIdentifierExpr() {
            String sql = "SELECT id, name FROM users";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("解析SQLIntegerExpr - 整数常量")
        void testParseSQLIntegerExpr() {
            String sql = "SELECT 1 AS one, 100 AS hundred FROM dual";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("解析SQLCharExpr - 字符常量")
        void testParseSQLCharExpr() {
            String sql = "SELECT 'hello' AS greeting, 'world' AS target FROM dual";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("解析SQLBinaryOpExpr - 二元运算表达式")
        void testParseSQLBinaryOpExpr() {
            String sql = "SELECT a + b AS sum, a > b AS compare FROM table_x";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("解析SQLAggregateExpr - 聚合函数")
        void testParseSQLAggregateExpr() {
            String sql = "SELECT COUNT(*) AS cnt, SUM(amount) AS total, AVG(price) AS avg_price FROM orders";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("解析SQLMethodInvokeExpr - 方法调用")
        void testParseSQLMethodInvokeExpr() {
            String sql = "SELECT CONCAT(first_name, last_name) AS full_name, UPPER(name) AS upper_name FROM users";
            List<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertEquals(2, result.size());
        }
    }
}
