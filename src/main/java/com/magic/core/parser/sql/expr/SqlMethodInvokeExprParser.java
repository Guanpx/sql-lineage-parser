package com.magic.core.parser.sql.expr;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;

import java.util.logging.Logger;

/**
 * 解析SQL查询语句中的函数调用表达式
 *
 * @author Guan Peixiang
 * @since 2023/12/20
 */
public final class SqlMethodInvokeExprParser implements BaseSqlExprParser {

    private static final Logger LOGGER = Logger.getLogger(SqlMethodInvokeExprParser.class.getName());
    private static final SqlMethodInvokeExprParser INSTANCE = new SqlMethodInvokeExprParser();

    private SqlMethodInvokeExprParser() {
    }

    public static SqlMethodInvokeExprParser getInstance() {
        return INSTANCE;
    }

    public static void parse(SQLMethodInvokeExpr expr, ExprParseContext context) {
        LOGGER.fine(() -> "函数调用: " + expr.getMethodName());

        for (SQLExpr arg : expr.getArguments()) {
            BaseSqlExprParser.parserSqlExpr(arg, context);
        }
    }

    @Override
    public void process() {
        throw new UnsupportedOperationException("Use parse(SQLMethodInvokeExpr, ExprParseContext) instead");
    }
}
