package com.magic.core.parser.sql.expr;

import com.alibaba.druid.sql.ast.expr.SQLCastExpr;

import java.util.logging.Logger;

/**
 * 解析 CAST 表达式
 * <p>
 * CAST 的源列就是被强制类型转换的表达式，递归解析即可
 *
 * @author Guan Peixiang
 * @since 2026/05/19
 */
public final class SqlCastExprParser implements BaseSqlExprParser {

    private static final Logger LOGGER = Logger.getLogger(SqlCastExprParser.class.getName());
    private static final SqlCastExprParser INSTANCE = new SqlCastExprParser();

    private SqlCastExprParser() {
    }

    public static SqlCastExprParser getInstance() {
        return INSTANCE;
    }

    /**
     * 解析 CAST 表达式，递归解析其被转换的子表达式
     */
    public static void parse(SQLCastExpr expr, ExprParseContext context) {
        if (expr == null) {
            return;
        }
        LOGGER.fine(() -> "CAST 表达式，目标类型: "
                + (expr.getDataType() != null ? expr.getDataType().toString() : "(unknown)"));
        BaseSqlExprParser.parserSqlExpr(expr.getExpr(), context);
    }

    @Override
    public void process() {
        throw new UnsupportedOperationException("Use parse(SQLCastExpr, ExprParseContext) instead");
    }
}
