package com.magic.core.parser;

import com.magic.core.util.SqlFileReader;
import com.magic.sqllineageparser.model.ColumnNode;
import com.magic.sqllineageparser.model.TreeNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(null);
            assertNull(result);

            result = SqlLineageParser.parserSingleSelectSql("");
            assertNull(result);
        }

        @Test
        @DisplayName("解析简单SELECT语句")
        void testParseSimpleSelect() {
            String sql = "SELECT id, name FROM users";
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result.getChildren());
            assertEquals(2, result.getChildren().size());
        }

        @Test
        @DisplayName("解析带别名的SELECT语句")
        void testParseSelectWithAlias() {
            String sql = "SELECT id AS user_id, name AS user_name FROM users u";
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result.getChildren());
            assertEquals(2, result.getChildren().size());

            ColumnNode firstColumn = result.getChildren().get(0).getValue();
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
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result.getChildren());
            // CASE WHEN + u.rere + u.* = 3个字段
            assertTrue(result.getChildren().size() >= 1);
        }

        @Test
        @DisplayName("解析带子查询的SELECT - sqlCase02.sql")
        void testParseCaseWithSubquery() {
            String sql = SqlFileReader.readCaseSql("sqlCase02.sql");
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result.getChildren());
            assertEquals(5, result.getChildren().size());
        }

        @Test
        @DisplayName("解析带反引号别名的SELECT - sqlCase03.sql")
        void testParseCaseWithBacktickAlias() {
            String sql = SqlFileReader.readCaseSql("sqlCase03.sql");
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result.getChildren());
        }
    }

    @Nested
    @DisplayName("JOIN 查询测试")
    class JoinQueryTest {

        @Test
        @DisplayName("解析简单LEFT JOIN - sqlJoin01.sql")
        void testParseSimpleLeftJoin() {
            String sql = SqlFileReader.readJoinSql("sqlJoin01.sql");
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result.getChildren());
            assertEquals(6, result.getChildren().size());
        }

        @Test
        @DisplayName("解析带子查询的LEFT JOIN - sqlJoin02.sql")
        void testParseJoinWithSubquery() {
            String sql = SqlFileReader.readJoinSql("sqlJoin02.sql");
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result.getChildren());
            assertEquals(8, result.getChildren().size());
        }
    }

    @Nested
    @DisplayName("函数表达式测试")
    class FunctionExprTest {

        @Test
        @DisplayName("解析IF函数 - sqlMaxIfNvlFunc01.sql")
        void testParseIfFunction() {
            String sql = SqlFileReader.readFunctionSql("sqlMaxIfNvlFunc01.sql");
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result.getChildren());
            assertEquals(1, result.getChildren().size());
        }

        @Test
        @DisplayName("解析MAX+IF嵌套函数 - sqlMaxIfNvlFunc02.sql")
        void testParseMaxIfFunction() {
            String sql = SqlFileReader.readFunctionSql("sqlMaxIfNvlFunc02.sql");
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result.getChildren());
            assertEquals(1, result.getChildren().size());
        }

        @Test
        @DisplayName("解析NVL函数 - sqlMaxIfNvlFunc03.sql")
        void testParseNvlFunction() {
            String sql = SqlFileReader.readFunctionSql("sqlMaxIfNvlFunc03.sql");
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result.getChildren());
            assertEquals(1, result.getChildren().size());
        }
    }

    @Nested
    @DisplayName("UNION 查询测试")
    class UnionQueryTest {

        @Test
        @DisplayName("解析UNION查询 - sqlUnion01.sql (当前不支持)")
        void testParseUnionQuery() {
            String sql = SqlFileReader.readUnionSql("sqlUnion01.sql");
            // UNION 查询当前返回 null (不支持)
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);
            assertNull(result, "UNION查询当前不支持，应返回null");
        }
    }

    @Nested
    @DisplayName("复杂生产SQL测试")
    class ProductionSqlTest {

        @Test
        @DisplayName("解析复杂生产SQL - sqlProd01.sql")
        void testParseComplexProductionSql() {
            String sql = SqlFileReader.readProdSql("sqlProd01.sql");
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertNotNull(result.getChildren());
            // 复杂SQL有多个输出字段
            assertTrue(result.getChildren().size() > 10);
        }
    }

    @Nested
    @DisplayName("表达式类型测试")
    class ExpressionTypeTest {

        @Test
        @DisplayName("解析SQLPropertyExpr - 表.列表达式")
        void testParseSQLPropertyExpr() {
            String sql = "SELECT t.id, t.name FROM table_a t";
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertEquals(2, result.getChildren().size());
        }

        @Test
        @DisplayName("解析SQLIdentifierExpr - 单列标识符")
        void testParseSQLIdentifierExpr() {
            String sql = "SELECT id, name FROM users";
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertEquals(2, result.getChildren().size());
        }

        @Test
        @DisplayName("解析SQLIntegerExpr - 整数常量")
        void testParseSQLIntegerExpr() {
            String sql = "SELECT 1 AS one, 100 AS hundred FROM dual";
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertEquals(2, result.getChildren().size());
        }

        @Test
        @DisplayName("解析SQLCharExpr - 字符常量")
        void testParseSQLCharExpr() {
            String sql = "SELECT 'hello' AS greeting, 'world' AS target FROM dual";
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertEquals(2, result.getChildren().size());
        }

        @Test
        @DisplayName("解析SQLBinaryOpExpr - 二元运算表达式")
        void testParseSQLBinaryOpExpr() {
            String sql = "SELECT a + b AS sum, a > b AS compare FROM table_x";
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertEquals(2, result.getChildren().size());
        }

        @Test
        @DisplayName("解析SQLAggregateExpr - 聚合函数")
        void testParseSQLAggregateExpr() {
            String sql = "SELECT COUNT(*) AS cnt, SUM(amount) AS total, AVG(price) AS avg_price FROM orders";
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertEquals(3, result.getChildren().size());
        }

        @Test
        @DisplayName("解析SQLMethodInvokeExpr - 方法调用")
        void testParseSQLMethodInvokeExpr() {
            String sql = "SELECT CONCAT(first_name, last_name) AS full_name, UPPER(name) AS upper_name FROM users";
            TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

            assertNotNull(result);
            assertEquals(2, result.getChildren().size());
        }
    }
}
