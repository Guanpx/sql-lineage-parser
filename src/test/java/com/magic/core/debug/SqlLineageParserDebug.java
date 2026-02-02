package com.magic.core.debug;

import com.magic.core.parser.SqlLineageParser;
import com.magic.core.util.SqlFileReader;
import com.magic.sqllineageparser.model.ColumnNode;
import com.magic.sqllineageparser.model.TreeNode;

/**
 * SqlLineageParser 调试类
 * 用于开发时调试核心解析器的解析结果
 *
 * @author Debug
 */
public class SqlLineageParserDebug {

    public static void main(String[] args) {
        // 选择要调试的场景
        debugSimpleSelect();
        debugCaseWhen();
        debugJoinQuery();
        debugFunctionExpr();
        debugComplexSql();
    }

    /**
     * 调试简单SELECT语句
     */
    public static void debugSimpleSelect() {
        DebugHelper.printTitle("调试简单SELECT语句");

        String sql = "SELECT id, name, age FROM users WHERE id > 10";
        DebugHelper.printSql(sql);

        TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

        DebugHelper.printSubTitle("解析结果 - 血缘树");
        DebugHelper.printLineageTree(result);
    }

    /**
     * 调试带别名的SELECT
     */
    public static void debugSelectWithAlias() {
        DebugHelper.printTitle("调试带别名的SELECT");

        String sql = "SELECT t.id AS user_id, t.name AS user_name, CONCAT(t.first, t.last) AS full_name FROM users t";
        DebugHelper.printSql(sql);

        TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

        DebugHelper.printSubTitle("解析结果 - 血缘树");
        DebugHelper.printLineageTree(result);
    }

    /**
     * 调试CASE WHEN语句
     */
    public static void debugCaseWhen() {
        DebugHelper.printTitle("调试CASE WHEN语句");

        // 从文件读取
        String sql = SqlFileReader.readCaseSql("sqlcase1.sql");
        DebugHelper.printSql(sql);

        TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

        DebugHelper.printSubTitle("解析结果 - 血缘树");
        DebugHelper.printLineageTree(result);
    }

    /**
     * 调试JOIN查询
     */
    public static void debugJoinQuery() {
        DebugHelper.printTitle("调试JOIN查询");

        // 简单JOIN
        String sql1 = SqlFileReader.readJoinSql("sqlJoin01.sql");
        DebugHelper.printSql(sql1);
        DebugHelper.printSubTitle("sqlJoin01 解析结果");
        TreeNode<ColumnNode> result1 = SqlLineageParser.parserSingleSelectSql(sql1);
        DebugHelper.printLineageTree(result1);

        // 带子查询的JOIN
        String sql2 = SqlFileReader.readJoinSql("sqlJoin02.sql");
        DebugHelper.printSql(sql2);
        DebugHelper.printSubTitle("sqlJoin02 解析结果");
        TreeNode<ColumnNode> result2 = SqlLineageParser.parserSingleSelectSql(sql2);
        DebugHelper.printLineageTree(result2);
    }

    /**
     * 调试函数表达式
     */
    public static void debugFunctionExpr() {
        DebugHelper.printTitle("调试函数表达式");

        // IF函数
        DebugHelper.printSubTitle("IF函数");
        String sql1 = SqlFileReader.readFunctionSql("sqlMaxIfNvlFunc01.sql");
        DebugHelper.printSql(sql1);
        TreeNode<ColumnNode> result1 = SqlLineageParser.parserSingleSelectSql(sql1);
        DebugHelper.printLineageTree(result1);

        // MAX+IF嵌套
        DebugHelper.printSubTitle("MAX+IF嵌套函数");
        String sql2 = SqlFileReader.readFunctionSql("sqlMaxIfNvlFunc02.sql");
        DebugHelper.printSql(sql2);
        TreeNode<ColumnNode> result2 = SqlLineageParser.parserSingleSelectSql(sql2);
        DebugHelper.printLineageTree(result2);

        // NVL函数
        DebugHelper.printSubTitle("NVL函数");
        String sql3 = SqlFileReader.readFunctionSql("sqlMaxIfNvlFunc03.sql");
        DebugHelper.printSql(sql3);
        TreeNode<ColumnNode> result3 = SqlLineageParser.parserSingleSelectSql(sql3);
        DebugHelper.printLineageTree(result3);
    }

    /**
     * 调试复杂生产SQL
     */
    public static void debugComplexSql() {
        DebugHelper.printTitle("调试复杂生产SQL");

        String sql = SqlFileReader.readProdSql("sqlProd01.sql");
        DebugHelper.printSql(sql);

        TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

        DebugHelper.printSubTitle("解析结果 - 血缘树");
        DebugHelper.printLineageTree(result);

        // 统计信息
        if (result != null && result.getChildren() != null) {
            DebugHelper.printSubTitle("统计信息");
            DebugHelper.printKeyValue("输出列数量", result.getChildren().size());
        }
    }

    /**
     * 调试UNION查询 (当前不支持)
     */
    public static void debugUnionQuery() {
        DebugHelper.printTitle("调试UNION查询");

        String sql = SqlFileReader.readUnionSql("sqlUnion01.sql");
        DebugHelper.printSql(sql);

        TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

        if (result == null) {
            DebugHelper.printError("UNION查询当前不支持");
        } else {
            DebugHelper.printLineageTree(result);
        }
    }

    /**
     * 调试自定义SQL - 便于开发时快速测试
     */
    public static void debugCustomSql(String sql) {
        DebugHelper.printTitle("调试自定义SQL");
        DebugHelper.printSql(sql);

        TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);

        DebugHelper.printSubTitle("解析结果 - 血缘树");
        DebugHelper.printLineageTree(result);
    }
}
