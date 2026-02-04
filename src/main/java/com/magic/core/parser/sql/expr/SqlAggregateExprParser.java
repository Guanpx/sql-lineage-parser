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

    /**
     * 解析聚合函数表达式，收集结果到上下文
     *
     * @param expr    聚合函数表达式
     * @param context 解析上下文
     */
    public static void parse(SQLAggregateExpr expr, ExprParseContext context) {
        LOGGER.fine(() -> "聚合函数: " + expr.getMethodName());

        // 递归解析函数参数
        for (SQLExpr arg : expr.getArguments()) {
            BaseSqlExprParser.parserSqlExpr(arg, context);
        }
    }

    @Override
    public void process() {
        throw new UnsupportedOperationException("Use parse(SQLAggregateExpr, ExprParseContext) instead");
    }
}
