package com.magic.core.parser.sql.table;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.statement.*;
import com.magic.core.util.SqlFileReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 表源解析器测试
 *
 * @author Test
 */
@DisplayName("表源解析器测试")
class TableSourceParserTest {

    @Nested
    @DisplayName("SQLExprTableSource 测试")
    class SQLExprTableSourceTest {

        @Test
        @DisplayName("解析简单表源")
        void testParseSimpleTableSource() {
            String sql = "SELECT * FROM users";
            SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) stmt.getSelect().getQuery();
            SQLTableSource tableSource = queryBlock.getFrom();

            assertInstanceOf(SQLExprTableSource.class, tableSource);
            SQLExprTableSource exprTableSource = (SQLExprTableSource) tableSource;
            assertEquals("users", exprTableSource.getTableName());
        }

        @Test
        @DisplayName("解析带别名的表源")
        void testParseTableSourceWithAlias() {
            String sql = "SELECT * FROM users u";
            SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) stmt.getSelect().getQuery();
            SQLTableSource tableSource = queryBlock.getFrom();

            assertInstanceOf(SQLExprTableSource.class, tableSource);
            SQLExprTableSource exprTableSource = (SQLExprTableSource) tableSource;
            assertEquals("users", exprTableSource.getTableName());
            assertEquals("u", exprTableSource.getAlias());
        }

        @Test
        @DisplayName("解析带schema的表源")
        void testParseTableSourceWithSchema() {
            String sql = "SELECT * FROM db.users";
            SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) stmt.getSelect().getQuery();
            SQLTableSource tableSource = queryBlock.getFrom();

            assertInstanceOf(SQLExprTableSource.class, tableSource);
            SQLExprTableSource exprTableSource = (SQLExprTableSource) tableSource;
            // getTableName()只返回表名，schema需要通过getSchema()获取
            assertEquals("users", exprTableSource.getTableName());
            assertEquals("db", exprTableSource.getSchema());
        }

        @Test
        @DisplayName("SQLExprTableSourceParser处理表源")
        void testSQLExprTableSourceParser() {
            String sql = "SELECT id FROM users";
            SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) stmt.getSelect().getQuery();
            SQLTableSource tableSource = queryBlock.getFrom();

            SqlExprTableSourceParser.getInstance().process("hive", new AtomicInteger(0), tableSource);
            // 验证解析器不抛出异常
            assertNotNull(tableSource);
        }
    }

    @Nested
    @DisplayName("SQLJoinTableSource 测试")
    class SQLJoinTableSourceTest {

        @Test
        @DisplayName("解析简单JOIN - sqlJoin01.sql")
        void testParseSimpleJoin() {
            String sql = SqlFileReader.readJoinSql("sqlJoin01.sql");
            SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) stmt.getSelect().getQuery();
            SQLTableSource tableSource = queryBlock.getFrom();

            assertInstanceOf(SQLJoinTableSource.class, tableSource);
            SQLJoinTableSource joinTableSource = (SQLJoinTableSource) tableSource;

            assertEquals(SQLJoinTableSource.JoinType.LEFT_OUTER_JOIN, joinTableSource.getJoinType());
            assertNotNull(joinTableSource.getLeft());
            assertNotNull(joinTableSource.getRight());
            assertNotNull(joinTableSource.getCondition());
        }

        @Test
        @DisplayName("解析带子查询的JOIN - sqlJoin02.sql")
        void testParseJoinWithSubquery() {
            String sql = SqlFileReader.readJoinSql("sqlJoin02.sql");
            SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) stmt.getSelect().getQuery();
            SQLTableSource tableSource = queryBlock.getFrom();

            assertInstanceOf(SQLJoinTableSource.class, tableSource);
            SQLJoinTableSource joinTableSource = (SQLJoinTableSource) tableSource;

            // 左表是普通表
            assertInstanceOf(SQLExprTableSource.class, joinTableSource.getLeft());
            // 右表是子查询
            assertInstanceOf(SQLSubqueryTableSource.class, joinTableSource.getRight());
        }

        @Test
        @DisplayName("SQLJoinTableSourceParser处理JOIN表源")
        void testSQLJoinTableSourceParser() {
            String sql = SqlFileReader.readJoinSql("sqlJoin01.sql");
            SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) stmt.getSelect().getQuery();
            SQLTableSource tableSource = queryBlock.getFrom();

            SqlJoinTableSourceParser.getInstance().process("hive", new AtomicInteger(0), tableSource);
            // 验证解析器不抛出异常
            assertNotNull(tableSource);
        }
    }

    @Nested
    @DisplayName("SQLSubqueryTableSource 测试")
    class SQLSubqueryTableSourceTest {

        @Test
        @DisplayName("解析子查询表源")
        void testParseSubqueryTableSource() {
            String sql = "SELECT * FROM (SELECT id, name FROM users) t";
            SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) stmt.getSelect().getQuery();
            SQLTableSource tableSource = queryBlock.getFrom();

            assertInstanceOf(SQLSubqueryTableSource.class, tableSource);
            SQLSubqueryTableSource subqueryTableSource = (SQLSubqueryTableSource) tableSource;
            assertEquals("t", subqueryTableSource.getAlias());
            assertNotNull(subqueryTableSource.getSelect());
        }

        @Test
        @DisplayName("解析嵌套子查询 - sqlCase02.sql")
        void testParseNestedSubquery() {
            String sql = SqlFileReader.readCaseSql("sqlCase02.sql");
            SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) stmt.getSelect().getQuery();
            SQLTableSource tableSource = queryBlock.getFrom();

            assertInstanceOf(SQLSubqueryTableSource.class, tableSource);
        }

        @Test
        @DisplayName("SQLSubqueryTableSourceParser处理子查询表源")
        void testSQLSubqueryTableSourceParser() {
            String sql = "SELECT * FROM (SELECT id FROM users) t";
            SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) stmt.getSelect().getQuery();
            SQLTableSource tableSource = queryBlock.getFrom();

            SqlSubqueryTableSourceParser.getInstance().process("hive", new AtomicInteger(0), tableSource);
            // 验证解析器不抛出异常
            assertNotNull(tableSource);
        }
    }

    @Nested
    @DisplayName("SQLUnionQueryTableSource 测试")
    class SQLUnionQueryTableSourceTest {

        @Test
        @DisplayName("解析UNION作为表源")
        void testParseUnionAsTableSource() {
            String sql = "SELECT * FROM (SELECT id FROM t1 UNION SELECT id FROM t2) u";
            SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) stmt.getSelect().getQuery();
            SQLTableSource tableSource = queryBlock.getFrom();

            // Druid可能直接解析为SQLUnionQueryTableSource或SQLSubqueryTableSource
            // 验证是表源类型即可
            assertNotNull(tableSource);
            assertTrue(tableSource instanceof SQLSubqueryTableSource
                    || tableSource instanceof SQLUnionQueryTableSource);
        }

        @Test
        @DisplayName("SQLUnionQueryTableSourceParser处理")
        void testSQLUnionQueryTableSourceParser() {
            // 直接测试解析器不抛出异常
            SqlUnionQueryTableSourceParser.getInstance().process("hive", new AtomicInteger(0), null);
        }
    }
}
