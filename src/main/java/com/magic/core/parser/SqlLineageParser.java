package com.magic.core.parser;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.magic.core.utils.StringUtils;
import com.magic.sqllineageparser.model.ColumnNode;
import com.magic.sqllineageparser.model.TreeNode;

import java.util.List;

/**
 * SQL血缘解析入口函数
 *
 * @author Guan Peixiang
 * @date 2023/12/19
 */
public final class SqlLineageParser {

    private SqlLineageParser() {
    }

    /**
     * 解析单个select sql
     * 包括:
     * 1.select 类型
     * 2.union 类型 (todo)
     *
     * @param sql SQL语句
     * @return 血缘树根节点
     */
    public static TreeNode<ColumnNode> parserSingleSelectSql(String sql) {
        if (StringUtils.isEmpty(sql)) {
            return null;
        }

        SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
        SQLSelectQuery sqlSelectQuery = stmt.getSelect().getQuery();
        TreeNode<ColumnNode> root = new TreeNode<>();

        // select语句
        if (sqlSelectQuery instanceof SQLSelectQueryBlock queryBlock) {
            parserSelectStmt(queryBlock, root);
            System.out.println(root.getChildren().size());
            return root;
        } else if (sqlSelectQuery instanceof SQLUnionQuery unionQuery) {
            System.out.println(unionQuery.getClass() + "类型不支持!!!");
            return null;
        } else {
            System.out.println(sqlSelectQuery.getClass() + "类型不支持!!!");
            return null;
        }
    }

    /**
     * 解析select语句 非union
     *
     * @param sqlSelectQueryBlock select查询块
     * @param root                根节点
     */
    private static void parserSelectStmt(SQLSelectQueryBlock sqlSelectQueryBlock, TreeNode<ColumnNode> root) {
        System.out.println("解析 select sqlSelectQueryBlock");

        List<SQLSelectItem> selectList = sqlSelectQueryBlock.getSelectList();
        System.out.println("select 语句大小：" + selectList.size());
        System.out.println("select 语句FROM ：" + sqlSelectQueryBlock);
        System.out.println(sqlSelectQueryBlock.getFrom().toString());

        for (SQLSelectItem item : selectList) {
            SQLExpr expr = item.getExpr();
            String alias = item.getAlias();
            String itemName = item.toString();

            TreeNode<ColumnNode> child = new TreeNode<>();
            ColumnNode node = new ColumnNode();
            node.setName(itemName);
            node.setAlias(alias);
            child.setValue(node);
            root.addChild(child);

            String getColumn = StringUtils.isEmpty(alias) ? itemName : alias;
            System.out.println("原字段: " + itemName);
            System.out.println("别名字段: " + getColumn);

            // 解析sql语句
            parserSqlExpr(expr);
        }

        SQLTableSource table = sqlSelectQueryBlock.getFrom();
        handleTableSource(table);
    }

    /**
     * 处理不同类型的表源
     *
     * @param table 表源
     */
    private static void handleTableSource(SQLTableSource table) {
        if (table == null) {
            return;
        }

        if (table instanceof SQLExprTableSource) {
            System.out.println("普通表：SQLExprTableSource");
            // TODO: 处理最终表
        } else if (table instanceof SQLJoinTableSource joinTableSource) {
            System.out.println("join表：SQLJoinTableSource");
            handlerSQLJoinTableSource(joinTableSource);
        } else if (table instanceof SQLSubqueryTableSource) {
            System.out.println("子查询表：SQLSubqueryTableSource");
            // TODO: 处理子查询
        } else if (table instanceof SQLUnionQueryTableSource) {
            System.out.println("union表：SQLUnionQueryTableSource");
            // TODO: 处理union
        }
        // 其他类型不处理
    }

    /**
     * 解析子节点sql表达式
     *
     * @param sqlExpr SQL表达式
     */
    private static void parserSqlExpr(SQLExpr sqlExpr) {
        if (sqlExpr == null) {
            System.out.println("！！！暂不支持未列出类型！！！");
            return;
        }

        // case when
        if (sqlExpr instanceof SQLCaseExpr expr) {
            parserSQLCaseExpr(expr);
        }
        // 聚合
        else if (sqlExpr instanceof SQLAggregateExpr expr) {
            parserSQLAggregateExpr(expr);
        }
        // 方法
        else if (sqlExpr instanceof SQLMethodInvokeExpr expr) {
            parserSqlMethodInvokeExpr(expr);
        }
        // 比较
        else if (sqlExpr instanceof SQLBinaryOpExpr expr) {
            parserSQLBinaryOpExpr(expr);
        }
        // 表达式
        else if (sqlExpr instanceof SQLPropertyExpr expr) {
            parserSQLPropertyExpr(expr);
        }
        // 列
        else if (sqlExpr instanceof SQLIdentifierExpr expr) {
            parserSqlIdentifierExpr(expr);
        }
        // 数字
        else if (sqlExpr instanceof SQLNumberExpr expr) {
            parserSQLNumberExpr(expr);
        }
        // 整数表达式
        else if (sqlExpr instanceof SQLIntegerExpr expr) {
            parserSqlIntegerExpr(expr);
        }
        // 字符
        else if (sqlExpr instanceof SQLCharExpr expr) {
            parseSqlCharExpr(expr);
        }
        // 其他未列出类型
        else {
            System.out.println("！！！暂不支持未列出类型！！！");
        }
    }

    /**
     * 表达式
     * select table.column from table_name table
     *
     * @param expr 属性表达式
     */
    private static void parserSQLPropertyExpr(SQLPropertyExpr expr) {
        System.out.println("表达式 propertyExpr");
        String name = expr.toString();
        System.out.println(name);
        System.out.println(expr.getOwner());
        System.out.println(expr.getName());
        System.out.println("表达式 propertyExpr 解析完成！！！");
    }

    /**
     * SQL CASE WHEN
     * select case when then else end
     *
     * @param expr case表达式
     */
    private static void parserSQLCaseExpr(SQLCaseExpr expr) {
        System.out.println("\n\n解析case when ....");
        List<SQLCaseExpr.Item> items = expr.getItems();
        System.out.println("开始解析每个子条目...");
        for (SQLCaseExpr.Item item : items) {
            System.out.println("item: " + item);
            System.out.println("case条件：" + item.getConditionExpr());
            parserSqlExpr(item.getValueExpr());
        }
        System.out.println("开始解析else语句...");
        if (expr.getElseExpr() != null) {
            parserSqlExpr(expr.getElseExpr());
        }
        System.out.println("解析case 结束 !\n\n\n");
    }

    /**
     * SQL Method
     * select func(xxx) from table
     *
     * @param expr 方法调用表达式
     */
    private static void parserSqlMethodInvokeExpr(SQLMethodInvokeExpr expr) {
        System.out.println("方法 visitSQLMethodInvoke");
        String name = expr.getMethodName();
        for (SQLExpr argsExpr : expr.getArguments()) {
            System.out.println(argsExpr);
            parserSqlExpr(argsExpr);
        }
        System.out.println(name);
        System.out.println("方法 visitSQLMethodInvoke 解析完成！！！");
    }

    /**
     * 列标识符
     *
     * @param expr 标识符表达式
     */
    private static void parserSqlIdentifierExpr(SQLIdentifierExpr expr) {
        System.out.println("列 identifierExpr");
        String name = expr.getName();
        System.out.println(name);
        System.out.println("列 identifierExpr 解析完成！！！");
    }

    /**
     * 整数 sql
     * select 1 as col
     *
     * @param expr 整数表达式
     */
    private static void parserSqlIntegerExpr(SQLIntegerExpr expr) {
        System.out.println("整数常量");
        String name = expr.getNumber().toString();
        System.out.println(name);
        System.out.println("整数常量 解析完成！！！");
    }

    /**
     * 数字
     *
     * @param expr 数字表达式
     */
    private static void parserSQLNumberExpr(SQLNumberExpr expr) {
        System.out.println(" 数字 numberExpr");
        String name = expr.toString();
        System.out.println(name);
        System.out.println("数字 numberExpr 解析完成！！！");
    }

    /**
     * 字符 sql
     * select '1' as col
     *
     * @param expr 字符表达式
     */
    private static void parseSqlCharExpr(SQLCharExpr expr) {
        System.out.println("字符常量");
        String name = expr.toString();
        System.out.println(name);
        System.out.println("字符常量 解析完成！！！");
    }

    /**
     * 比较类 sql
     * select a>b
     *
     * @param expr 二元操作表达式
     */
    private static void parserSQLBinaryOpExpr(SQLBinaryOpExpr expr) {
        System.out.println("比较 BinaryOp");
        String name = expr.toString();
        System.out.println(name);
        System.out.println(expr.getLeft());
        parserSqlExpr(expr.getLeft());
        System.out.println(expr.getOperator());
        System.out.println(expr.getRight());
        parserSqlExpr(expr.getRight());
        System.out.println("比较 BinaryOp 解析完成！！！");
    }

    /**
     * 聚合函数
     *
     * @param expr 聚合表达式
     */
    private static void parserSQLAggregateExpr(SQLAggregateExpr expr) {
        System.out.println("聚合aggregate");
        System.out.println(expr.computeDataType());
        System.out.println(expr.getMethodName());
        System.out.println();
        for (SQLExpr arg : expr.getArguments()) {
            parserSqlExpr(arg);
        }
        System.out.println("聚合aggregate 解析完成！！！");
    }

    /**
     * sql join table source
     *
     * @param tableSource JOIN表源
     */
    private static void handlerSQLJoinTableSource(SQLJoinTableSource tableSource) {
        System.out.println(tableSource.getJoinType());
        System.out.println(tableSource);
        System.out.println(tableSource.getFlashback());
        System.out.println(tableSource.getAlias2());
        System.out.println(tableSource.getLeft());
        System.out.println(tableSource.getRight());
        System.out.println(tableSource.getCondition());
    }
}
