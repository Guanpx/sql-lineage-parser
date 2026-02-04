package com.magic.core.parser;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.statement.*;
import com.magic.core.parser.sql.expr.BaseSqlExprParser;
import com.magic.core.parser.sql.expr.ExprParseContext;
import com.magic.core.utils.StringUtils;
import com.magic.sqllineageparser.model.ColumnNode;
import com.magic.sqllineageparser.model.TreeNode;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQL血缘解析入口
 *
 * @author Guan Peixiang
 * @since 2023/12/19
 */
public final class SqlLineageParser {

    private static final Logger LOGGER = Logger.getLogger(SqlLineageParser.class.getName());

    private SqlLineageParser() {
    }

    /**
     * 解析单个 SELECT SQL 语句
     * <p>支持: SELECT 类型、UNION 类型(开发中)
     *
     * @param sql SQL语句
     * @return 血缘树根节点，解析失败返回 null
     */
    public static TreeNode<ColumnNode> parserSingleSelectSql(String sql) {
        if (StringUtils.isEmpty(sql)) {
            return null;
        }

        var stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
        var sqlSelectQuery = stmt.getSelect().getQuery();
        var root = new TreeNode<ColumnNode>();

        if (sqlSelectQuery instanceof SQLSelectQueryBlock queryBlock) {
            parserSelectStmt(queryBlock, root);
            LOGGER.log(Level.FINE, () -> "解析完成, 列数: " + root.getChildren().size());
            return root;
        } else if (sqlSelectQuery instanceof SQLUnionQuery unionQuery) {
            LOGGER.warning(() -> "UNION 查询暂不支持: " + unionQuery.getClass().getSimpleName());
            return null;
        } else {
            LOGGER.warning(() -> "不支持的查询类型: " +
                    (sqlSelectQuery == null ? "null" : sqlSelectQuery.getClass().getSimpleName()));
            return null;
        }
    }

    /**
     * 解析 SELECT 语句（非 UNION）
     *
     * @param queryBlock SELECT 查询块
     * @param root       根节点
     */
    private static void parserSelectStmt(SQLSelectQueryBlock queryBlock, TreeNode<ColumnNode> root) {
        LOGGER.fine("解析 SELECT 语句");

        // 从 FROM 子句创建解析上下文（包含表别名映射）
        ExprParseContext context = ExprParseContext.fromTableSource(queryBlock.getFrom());
        LOGGER.log(Level.FINE, () -> "表别名映射: " + context.getAliasToTableMap());

        List<SQLSelectItem> selectList = queryBlock.getSelectList();
        LOGGER.log(Level.FINE, () -> "SELECT 列数: " + selectList.size());

        for (SQLSelectItem item : selectList) {
            var expr = item.getExpr();
            var alias = item.getAlias();
            var itemName = item.toString();

            // 创建列节点
            var node = new ColumnNode();
            node.setName(itemName);
            node.setAlias(alias);
            node.setExpression(expr.toString());

            var child = TreeNode.of(node);
            root.addChild(child);

            var displayName = StringUtils.isEmpty(alias) ? itemName : alias;
            LOGGER.log(Level.FINE, () -> "解析列: " + displayName);

            // 设置当前列并解析表达式，来源列会自动收集到 node 中
            context.setCurrentColumn(node);
            BaseSqlExprParser.parserSqlExpr(expr, context);

            LOGGER.log(Level.FINE, () -> "列 " + displayName + " 的来源列数: " + node.getSourceColumns().size());
        }

        handleTableSource(queryBlock.getFrom());
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

        if (table instanceof SQLExprTableSource exprTable) {
            LOGGER.fine(() -> "普通表: " + exprTable.getExpr());
        } else if (table instanceof SQLJoinTableSource joinTable) {
            LOGGER.fine("JOIN 表");
            handleJoinTableSource(joinTable);
        } else if (table instanceof SQLSubqueryTableSource subquery) {
            LOGGER.fine(() -> "子查询表: " + subquery.getAlias());
        } else if (table instanceof SQLUnionQueryTableSource) {
            LOGGER.fine("UNION 表源");
        } else {
            LOGGER.fine(() -> "其他表源类型: " + table.getClass().getSimpleName());
        }
    }

    /**
     * 处理 JOIN 表源
     */
    private static void handleJoinTableSource(SQLJoinTableSource joinTable) {
        LOGGER.fine(() -> "JOIN 类型: " + joinTable.getJoinType());
        LOGGER.fine(() -> "左表: " + joinTable.getLeft());
        LOGGER.fine(() -> "右表: " + joinTable.getRight());
        LOGGER.fine(() -> "JOIN 条件: " + joinTable.getCondition());
    }
}
