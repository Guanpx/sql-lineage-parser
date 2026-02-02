package com.magic.core.debug;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.statement.*;
import com.magic.core.util.SqlFileReader;

/**
 * TableSourceParser 调试类
 * 用于开发时调试各种表源的解析结果
 *
 * @author Debug
 */
public class TableSourceParserDebug {

    public static void main(String[] args) {
        debugSQLExprTableSource();
        debugSQLJoinTableSource();
        debugSQLSubqueryTableSource();
        debugSQLUnionQueryTableSource();
    }

    /**
     * 调试普通表源 SQLExprTableSource
     */
    public static void debugSQLExprTableSource() {
        DebugHelper.printTitle("调试 SQLExprTableSource (普通表源)");

        // 简单表
        debugTableSource("SELECT * FROM users", "简单表");

        // 带别名的表
        debugTableSource("SELECT * FROM users u", "带别名的表");

        // 带schema的表
        debugTableSource("SELECT * FROM db.schema.users", "带schema的表");

        // 带数据库名的表
        debugTableSource("SELECT * FROM mydb.users AS u", "带数据库名和别名");
    }

    /**
     * 调试JOIN表源 SQLJoinTableSource
     */
    public static void debugSQLJoinTableSource() {
        DebugHelper.printTitle("调试 SQLJoinTableSource (JOIN表源)");

        // 从文件读取简单JOIN
        String sql1 = SqlFileReader.readJoinSql("sqlJoin01.sql");
        DebugHelper.printSubTitle("sqlJoin01 - 简单LEFT JOIN");
        DebugHelper.printSql(sql1);
        debugJoinTableSource(sql1);

        // 从文件读取带子查询的JOIN
        String sql2 = SqlFileReader.readJoinSql("sqlJoin02.sql");
        DebugHelper.printSubTitle("sqlJoin02 - 带子查询的JOIN");
        DebugHelper.printSql(sql2);
        debugJoinTableSource(sql2);

        // 多表JOIN
        String sql3 = "SELECT * FROM t1 JOIN t2 ON t1.id = t2.id JOIN t3 ON t2.id = t3.id";
        DebugHelper.printSubTitle("多表JOIN");
        DebugHelper.printSql(sql3);
        debugJoinTableSource(sql3);
    }

    /**
     * 调试子查询表源 SQLSubqueryTableSource
     */
    public static void debugSQLSubqueryTableSource() {
        DebugHelper.printTitle("调试 SQLSubqueryTableSource (子查询表源)");

        // 简单子查询
        String sql1 = "SELECT * FROM (SELECT id, name FROM users) t";
        DebugHelper.printSubTitle("简单子查询");
        DebugHelper.printSql(sql1);
        debugSubqueryTableSource(sql1);

        // 嵌套子查询
        String sql2 = "SELECT * FROM (SELECT * FROM (SELECT id FROM users) a) b";
        DebugHelper.printSubTitle("嵌套子查询");
        DebugHelper.printSql(sql2);
        debugSubqueryTableSource(sql2);

        // 从文件读取带子查询的SQL
        String sql3 = SqlFileReader.readCaseSql("sqlCase02.sql");
        DebugHelper.printSubTitle("sqlCase02 - 带聚合的子查询");
        DebugHelper.printSql(sql3);
        debugSubqueryTableSource(sql3);
    }

    /**
     * 调试UNION查询表源 SQLUnionQueryTableSource
     */
    public static void debugSQLUnionQueryTableSource() {
        DebugHelper.printTitle("调试 SQLUnionQueryTableSource (UNION表源)");

        // UNION作为子查询
        String sql1 = "SELECT * FROM (SELECT id FROM t1 UNION SELECT id FROM t2) u";
        DebugHelper.printSubTitle("UNION作为子查询");
        DebugHelper.printSql(sql1);
        debugUnionTableSource(sql1);

        // UNION ALL
        String sql2 = "SELECT * FROM (SELECT id FROM t1 UNION ALL SELECT id FROM t2) u";
        DebugHelper.printSubTitle("UNION ALL");
        DebugHelper.printSql(sql2);
        debugUnionTableSource(sql2);
    }

    // ==================== 辅助方法 ====================

    private static void debugTableSource(String sql, String label) {
        DebugHelper.printSubTitle(label);
        DebugHelper.printSql(sql);

        SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
        SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) stmt.getSelect().getQuery();
        SQLTableSource tableSource = queryBlock.getFrom();

        System.out.println("\n【表源解析结果】");
        DebugHelper.printKeyValue("类型", tableSource.getClass().getSimpleName());
        DebugHelper.printKeyValue("别名", tableSource.getAlias());

        if (tableSource instanceof SQLExprTableSource exprTableSource) {
            DebugHelper.printKeyValue("表名", exprTableSource.getTableName());
            DebugHelper.printKeyValue("Schema", exprTableSource.getSchema());
            DebugHelper.printKeyValue("表达式", exprTableSource.getExpr());
            DebugHelper.printKeyValue("表达式类型", exprTableSource.getExpr().getClass().getSimpleName());
        }
    }

    private static void debugJoinTableSource(String sql) {
        SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
        SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) stmt.getSelect().getQuery();
        SQLTableSource tableSource = queryBlock.getFrom();

        System.out.println("\n【JOIN表源解析结果】");
        DebugHelper.printKeyValue("类型", tableSource.getClass().getSimpleName());

        if (tableSource instanceof SQLJoinTableSource joinTableSource) {
            printJoinDetails(joinTableSource, 0);
        }
    }

    private static void printJoinDetails(SQLJoinTableSource joinTableSource, int depth) {
        String indent = "  ".repeat(depth);
        System.out.println(indent + "【JOIN详情】");
        System.out.println(indent + "  JOIN类型: " + joinTableSource.getJoinType());
        System.out.println(indent + "  JOIN条件: " + joinTableSource.getCondition());

        // 左表
        SQLTableSource left = joinTableSource.getLeft();
        System.out.println(indent + "  左表类型: " + left.getClass().getSimpleName());
        System.out.println(indent + "  左表内容: " + left);
        if (left instanceof SQLJoinTableSource leftJoin) {
            printJoinDetails(leftJoin, depth + 1);
        }

        // 右表
        SQLTableSource right = joinTableSource.getRight();
        System.out.println(indent + "  右表类型: " + right.getClass().getSimpleName());
        System.out.println(indent + "  右表内容: " + right);
        if (right instanceof SQLJoinTableSource rightJoin) {
            printJoinDetails(rightJoin, depth + 1);
        }
    }

    private static void debugSubqueryTableSource(String sql) {
        SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
        SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) stmt.getSelect().getQuery();
        SQLTableSource tableSource = queryBlock.getFrom();

        System.out.println("\n【子查询表源解析结果】");
        DebugHelper.printKeyValue("类型", tableSource.getClass().getSimpleName());
        DebugHelper.printKeyValue("别名", tableSource.getAlias());

        if (tableSource instanceof SQLSubqueryTableSource subqueryTableSource) {
            SQLSelect select = subqueryTableSource.getSelect();
            SQLSelectQuery query = select.getQuery();
            System.out.println("  子查询类型: " + query.getClass().getSimpleName());
            System.out.println("  子查询内容: " + select);

            // 如果子查询还是SQLSelectQueryBlock，递归打印
            if (query instanceof SQLSelectQueryBlock innerBlock) {
                System.out.println("  子查询FROM: " + innerBlock.getFrom());
                System.out.println("  子查询SELECT列数: " + innerBlock.getSelectList().size());
            }
        }
    }

    private static void debugUnionTableSource(String sql) {
        SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
        SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) stmt.getSelect().getQuery();
        SQLTableSource tableSource = queryBlock.getFrom();

        System.out.println("\n【UNION表源解析结果】");
        DebugHelper.printKeyValue("类型", tableSource.getClass().getSimpleName());
        DebugHelper.printKeyValue("别名", tableSource.getAlias());

        if (tableSource instanceof SQLSubqueryTableSource subqueryTableSource) {
            SQLSelectQuery query = subqueryTableSource.getSelect().getQuery();
            if (query instanceof SQLUnionQuery unionQuery) {
                System.out.println("  UNION类型: " + unionQuery.getOperator());
                System.out.println("  左查询: " + unionQuery.getLeft());
                System.out.println("  右查询: " + unionQuery.getRight());
            }
        } else if (tableSource instanceof SQLUnionQueryTableSource unionTableSource) {
            SQLUnionQuery unionQuery = unionTableSource.getUnion();
            System.out.println("  UNION类型: " + unionQuery.getOperator());
            System.out.println("  左查询: " + unionQuery.getLeft());
            System.out.println("  右查询: " + unionQuery.getRight());
        }
    }
}
