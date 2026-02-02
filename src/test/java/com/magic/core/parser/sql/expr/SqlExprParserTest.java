package com.magic.core.parser.sql.expr;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.magic.core.util.SqlFileReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL表达式解析器测试
 *
 * @author Test
 */
@DisplayName("SQL表达式解析器测试")
class SqlExprParserTest {

    private SQLExpr getFirstSelectExpr(String sql) {
        SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
        SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) stmt.getSelect().getQuery();
        return queryBlock.getSelectList().get(0).getExpr();
    }

    private List<SQLSelectItem> getAllSelectItems(String sql) {
        SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
        SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) stmt.getSelect().getQuery();
        return queryBlock.getSelectList();
    }

    @Nested
    @DisplayName("SQLPropertyExpr 测试 - 表.列表达式")
    class SQLPropertyExprTest {

        @Test
        @DisplayName("解析简单的表.列表达式")
        void testSimplePropertyExpr() {
            String sql = "SELECT t.id FROM users t";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLPropertyExpr.class, expr);
            SQLPropertyExpr propertyExpr = (SQLPropertyExpr) expr;
            assertEquals("id", propertyExpr.getName());
            assertEquals("t", propertyExpr.getOwnerName());
        }

        @Test
        @DisplayName("解析schema.table.column表达式")
        void testFullyQualifiedPropertyExpr() {
            String sql = "SELECT db.users.id FROM db.users";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLPropertyExpr.class, expr);
        }
    }

    @Nested
    @DisplayName("SQLIdentifierExpr 测试 - 列标识符")
    class SQLIdentifierExprTest {

        @Test
        @DisplayName("解析简单列标识符")
        void testSimpleIdentifier() {
            String sql = "SELECT id FROM users";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLIdentifierExpr.class, expr);
            SQLIdentifierExpr identifierExpr = (SQLIdentifierExpr) expr;
            assertEquals("id", identifierExpr.getName());
        }
    }

    @Nested
    @DisplayName("SQLCaseExpr 测试 - CASE WHEN表达式")
    class SQLCaseExprTest {

        @Test
        @DisplayName("解析CASE WHEN表达式 - sqlcase1.sql")
        void testCaseWhenFromFile() {
            String sql = SqlFileReader.readCaseSql("sqlcase1.sql");
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLCaseExpr.class, expr);
            SQLCaseExpr caseExpr = (SQLCaseExpr) expr;
            assertEquals(3, caseExpr.getItems().size());
            assertNotNull(caseExpr.getElseExpr());
        }

        @Test
        @DisplayName("解析简单CASE WHEN")
        void testSimpleCaseWhen() {
            String sql = "SELECT CASE WHEN status = 1 THEN 'active' ELSE 'inactive' END FROM users";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLCaseExpr.class, expr);
            SQLCaseExpr caseExpr = (SQLCaseExpr) expr;
            assertEquals(1, caseExpr.getItems().size());
        }

        @Test
        @DisplayName("解析多条件CASE WHEN")
        void testMultiConditionCaseWhen() {
            String sql = "SELECT CASE WHEN a = 1 THEN 'one' WHEN a = 2 THEN 'two' WHEN a = 3 THEN 'three' ELSE 'other' END FROM t";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLCaseExpr.class, expr);
            SQLCaseExpr caseExpr = (SQLCaseExpr) expr;
            assertEquals(3, caseExpr.getItems().size());
        }
    }

    @Nested
    @DisplayName("SQLAggregateExpr 测试 - 聚合函数")
    class SQLAggregateExprTest {

        @Test
        @DisplayName("解析COUNT函数")
        void testCountFunction() {
            String sql = "SELECT COUNT(*) FROM users";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLAggregateExpr.class, expr);
            SQLAggregateExpr aggregateExpr = (SQLAggregateExpr) expr;
            assertEquals("COUNT", aggregateExpr.getMethodName());
        }

        @Test
        @DisplayName("解析SUM函数")
        void testSumFunction() {
            String sql = "SELECT SUM(amount) FROM orders";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLAggregateExpr.class, expr);
            SQLAggregateExpr aggregateExpr = (SQLAggregateExpr) expr;
            assertEquals("SUM", aggregateExpr.getMethodName());
            assertEquals(1, aggregateExpr.getArguments().size());
        }

        @Test
        @DisplayName("解析AVG函数")
        void testAvgFunction() {
            String sql = "SELECT AVG(price) FROM products";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLAggregateExpr.class, expr);
            SQLAggregateExpr aggregateExpr = (SQLAggregateExpr) expr;
            assertEquals("AVG", aggregateExpr.getMethodName());
        }

        @Test
        @DisplayName("解析MAX函数")
        void testMaxFunction() {
            String sql = "SELECT MAX(score) FROM students";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLAggregateExpr.class, expr);
            SQLAggregateExpr aggregateExpr = (SQLAggregateExpr) expr;
            assertEquals("MAX", aggregateExpr.getMethodName());
        }

        @Test
        @DisplayName("解析MIN函数")
        void testMinFunction() {
            String sql = "SELECT MIN(score) FROM students";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLAggregateExpr.class, expr);
            SQLAggregateExpr aggregateExpr = (SQLAggregateExpr) expr;
            assertEquals("MIN", aggregateExpr.getMethodName());
        }
    }

    @Nested
    @DisplayName("SQLMethodInvokeExpr 测试 - 函数调用")
    class SQLMethodInvokeExprTest {

        @Test
        @DisplayName("解析IF函数 - sqlMaxIfNvlFunc01.sql")
        void testIfFunctionFromFile() {
            String sql = SqlFileReader.readFunctionSql("sqlMaxIfNvlFunc01.sql");
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLMethodInvokeExpr.class, expr);
            SQLMethodInvokeExpr methodExpr = (SQLMethodInvokeExpr) expr;
            assertEquals("if", methodExpr.getMethodName());
            assertEquals(3, methodExpr.getArguments().size());
        }

        @Test
        @DisplayName("解析NVL函数 - sqlMaxIfNvlFunc03.sql")
        void testNvlFunctionFromFile() {
            String sql = SqlFileReader.readFunctionSql("sqlMaxIfNvlFunc03.sql");
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLMethodInvokeExpr.class, expr);
            SQLMethodInvokeExpr methodExpr = (SQLMethodInvokeExpr) expr;
            assertEquals("nvl", methodExpr.getMethodName());
            assertEquals(2, methodExpr.getArguments().size());
        }

        @Test
        @DisplayName("解析CONCAT函数")
        void testConcatFunction() {
            String sql = "SELECT CONCAT(first_name, ' ', last_name) FROM users";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLMethodInvokeExpr.class, expr);
            SQLMethodInvokeExpr methodExpr = (SQLMethodInvokeExpr) expr;
            assertEquals("CONCAT", methodExpr.getMethodName());
            assertEquals(3, methodExpr.getArguments().size());
        }

        @Test
        @DisplayName("解析SUBSTR函数")
        void testSubstrFunction() {
            String sql = "SELECT SUBSTR(name, 1, 5) FROM users";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLMethodInvokeExpr.class, expr);
            SQLMethodInvokeExpr methodExpr = (SQLMethodInvokeExpr) expr;
            assertEquals("SUBSTR", methodExpr.getMethodName());
        }

        @Test
        @DisplayName("解析嵌套函数调用 - MAX(IF(...))")
        void testNestedFunctionFromFile() {
            String sql = SqlFileReader.readFunctionSql("sqlMaxIfNvlFunc02.sql");
            SQLExpr expr = getFirstSelectExpr(sql);

            // MAX是聚合函数
            assertInstanceOf(SQLAggregateExpr.class, expr);
            SQLAggregateExpr aggregateExpr = (SQLAggregateExpr) expr;
            // Druid解析后方法名为小写
            assertEquals("max", aggregateExpr.getMethodName());

            // 内部参数是IF函数
            SQLExpr innerExpr = aggregateExpr.getArguments().get(0);
            assertInstanceOf(SQLMethodInvokeExpr.class, innerExpr);
            SQLMethodInvokeExpr ifExpr = (SQLMethodInvokeExpr) innerExpr;
            assertEquals("if", ifExpr.getMethodName());
        }
    }

    @Nested
    @DisplayName("SQLBinaryOpExpr 测试 - 二元运算表达式")
    class SQLBinaryOpExprTest {

        @Test
        @DisplayName("解析加法表达式")
        void testAdditionExpr() {
            String sql = "SELECT a + b FROM t";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLBinaryOpExpr.class, expr);
            SQLBinaryOpExpr binaryExpr = (SQLBinaryOpExpr) expr;
            assertEquals(SQLBinaryOperator.Add, binaryExpr.getOperator());
        }

        @Test
        @DisplayName("解析比较表达式")
        void testComparisonExpr() {
            String sql = "SELECT a > b FROM t";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLBinaryOpExpr.class, expr);
            SQLBinaryOpExpr binaryExpr = (SQLBinaryOpExpr) expr;
            assertEquals(SQLBinaryOperator.GreaterThan, binaryExpr.getOperator());
        }

        @Test
        @DisplayName("解析AND表达式")
        void testAndExpr() {
            String sql = "SELECT a = 1 AND b = 2 FROM t";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLBinaryOpExpr.class, expr);
            SQLBinaryOpExpr binaryExpr = (SQLBinaryOpExpr) expr;
            assertEquals(SQLBinaryOperator.BooleanAnd, binaryExpr.getOperator());
        }

        @Test
        @DisplayName("解析OR表达式")
        void testOrExpr() {
            String sql = "SELECT a = 1 OR b = 2 FROM t";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLBinaryOpExpr.class, expr);
            SQLBinaryOpExpr binaryExpr = (SQLBinaryOpExpr) expr;
            assertEquals(SQLBinaryOperator.BooleanOr, binaryExpr.getOperator());
        }
    }

    @Nested
    @DisplayName("SQLNumberExpr 和 SQLIntegerExpr 测试 - 数值常量")
    class NumberExprTest {

        @Test
        @DisplayName("解析整数常量")
        void testIntegerExpr() {
            String sql = "SELECT 100 FROM dual";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLIntegerExpr.class, expr);
            SQLIntegerExpr integerExpr = (SQLIntegerExpr) expr;
            assertEquals(100, integerExpr.getNumber().intValue());
        }

        @Test
        @DisplayName("解析浮点数常量")
        void testNumberExpr() {
            String sql = "SELECT 3.14 FROM dual";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLNumberExpr.class, expr);
        }

        @Test
        @DisplayName("解析负数常量")
        void testNegativeNumber() {
            String sql = "SELECT -999999 FROM dual";
            SQLExpr expr = getFirstSelectExpr(sql);

            // 负数可能被解析为一元表达式或直接作为负数
            assertNotNull(expr);
        }
    }

    @Nested
    @DisplayName("SQLCharExpr 测试 - 字符常量")
    class CharExprTest {

        @Test
        @DisplayName("解析字符串常量")
        void testCharExpr() {
            String sql = "SELECT 'hello world' FROM dual";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLCharExpr.class, expr);
            SQLCharExpr charExpr = (SQLCharExpr) expr;
            assertEquals("hello world", charExpr.getText());
        }

        @Test
        @DisplayName("解析空字符串")
        void testEmptyCharExpr() {
            String sql = "SELECT '' FROM dual";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLCharExpr.class, expr);
            SQLCharExpr charExpr = (SQLCharExpr) expr;
            assertEquals("", charExpr.getText());
        }

        @Test
        @DisplayName("解析带单引号的字符串")
        void testCharExprWithQuote() {
            String sql = "SELECT 'it''s a test' FROM dual";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLCharExpr.class, expr);
        }
    }

    @Nested
    @DisplayName("复合表达式测试")
    class ComplexExprTest {

        @Test
        @DisplayName("解析CAST表达式")
        void testCastExpr() {
            String sql = "SELECT CAST(amount AS STRING) FROM orders";
            SQLExpr expr = getFirstSelectExpr(sql);

            assertInstanceOf(SQLCastExpr.class, expr);
        }

        @Test
        @DisplayName("解析复杂生产SQL的表达式 - sqlProd01.sql")
        void testProductionSqlExpressions() {
            String sql = SqlFileReader.readProdSql("sqlProd01.sql");
            List<SQLSelectItem> selectItems = getAllSelectItems(sql);

            assertFalse(selectItems.isEmpty());
            // 验证包含多种表达式类型
            boolean hasCaseExpr = false;
            boolean hasAggregateExpr = false;
            boolean hasMethodExpr = false;

            for (SQLSelectItem item : selectItems) {
                SQLExpr expr = item.getExpr();
                if (expr instanceof SQLCaseExpr) {
                    hasCaseExpr = true;
                } else if (expr instanceof SQLAggregateExpr) {
                    hasAggregateExpr = true;
                } else if (expr instanceof SQLMethodInvokeExpr) {
                    hasMethodExpr = true;
                }
            }

            assertTrue(hasCaseExpr, "应该包含CASE表达式");
            assertTrue(hasAggregateExpr, "应该包含聚合表达式");
            assertTrue(hasMethodExpr, "应该包含方法调用表达式");
        }
    }
}
