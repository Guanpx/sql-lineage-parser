package com.magic.core.debug;

import com.magic.core.parser.SqlLineageParser;
import com.magic.core.util.SqlFileReader;
import com.magic.sqllineageparser.model.ColumnNode;
import com.magic.sqllineageparser.model.DmlLineageInfo;
import com.magic.sqllineageparser.model.TreeNode;

/**
 * CreateSqlLineageParserDebug 调试类
 * 用于开发时调试核心解析器的解析结果
 *
 * @author Debug
 */
public class CreateSqlLineageParserDebug {


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

//        TreeNode<ColumnNode> result = SqlLineageParser.parserSingleSelectSql(sql);
        DmlLineageInfo dmlLineageInfo = SqlLineageParser.parserCreateTableSql(sql);

        DebugHelper.printSubTitle("解析结果 - 血缘树");
        DebugHelper.printCreateLineageTree(dmlLineageInfo);
    }

    public static void main(String[] args) {

//        debugComplexSql();

        String sql = """
               create table sss(aaa, ddd) AS
               select ad, bc
               from
               (select ad, xx AS bc from table_2)
               x
               """;
        debugCustomSql(sql);
    }


}
