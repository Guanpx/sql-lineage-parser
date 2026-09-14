package com.magic.core.parser.sql.expr;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;

import java.util.logging.Logger;

/**
 * 解析聚合函数表达式
 *
 * @author Guan Peixiang
 * @since 2023/12/20
 */
public final class SqlAggregateExprParser implements BaseSqlExprParser {

    private static final Logger LOGGER = Logger.getLogger(SqlAggregateExprParser.class.getName());
    private static final SqlAggregateExprParser INSTANCE = new SqlAggregateExprParser();

    private SqlAggregateExprParser() {
    }

    public static SqlAggregateExprParser getInstance() {
        return INSTANCE;
    }

    public static void parse(SQLAggregateExpr expr, ExprParseContext context) {
        LOGGER.fine(() -> "聚合函数: " + expr.getMethodName());

        for (SQLExpr arg : expr.getArguments()) {
            BaseSqlExprParser.parserSqlExpr(arg, context);
        }

        if (expr.getOver() != null) {
            LOGGER.fine(() -> "聚合 " + expr.getMethodName() + " 带 OVER 窗口");
            SqlOverExprParser.parse(expr.getOver(), context);
        }
    }

    @Override
    public void process() {
        throw new UnsupportedOperationException("Use parse(SQLAggregateExpr, ExprParseContext) instead");
    }
}
