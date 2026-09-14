package com.magic.core.parser.sql.expr;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLOver;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;

import java.util.List;
import java.util.logging.Logger;

/**
 * 解析窗口函数的 OVER 子句
 * <p>
 * 处理 PARTITION BY / ORDER BY / DISTRIBUTE BY / SORT BY / CLUSTER BY 子句中的列引用，
 * 将其作为来源列收集到上下文。
 *
 * @author Guan Peixiang
 * @since 2023/12/21
 */
public final class SqlOverExprParser implements BaseSqlExprParser {

    private static final Logger LOGGER = Logger.getLogger(SqlOverExprParser.class.getName());
    private static final SqlOverExprParser INSTANCE = new SqlOverExprParser();

    private SqlOverExprParser() {
    }

    public static SqlOverExprParser getInstance() {
        return INSTANCE;
    }

    public static void parse(SQLOver over, ExprParseContext context) {
        if (over == null) {
            return;
        }
        LOGGER.fine("解析 OVER 子句");

        List<SQLExpr> partitionBy = over.getPartitionBy();
        if (partitionBy != null) {
            for (SQLExpr expr : partitionBy) {
                BaseSqlExprParser.parserSqlExpr(expr, context);
            }
        }

        parseOrderBy(over.getOrderBy(), context);
        parseOrderBy(over.getDistributeBy(), context);
        parseOrderBy(over.getSortBy(), context);
        parseOrderBy(over.getClusterBy(), context);
    }

    private static void parseOrderBy(SQLOrderBy orderBy, ExprParseContext context) {
        if (orderBy == null || orderBy.getItems() == null) {
            return;
        }
        for (SQLSelectOrderByItem item : orderBy.getItems()) {
            BaseSqlExprParser.parserSqlExpr(item.getExpr(), context);
        }
    }

    @Override
    public void process() {
        throw new UnsupportedOperationException("Use parse(SQLOver, ExprParseContext) instead");
    }
}
