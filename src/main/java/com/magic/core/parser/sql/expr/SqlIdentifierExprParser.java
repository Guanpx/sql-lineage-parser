package com.magic.core.parser.sql.expr;

import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;

import java.util.logging.Logger;

/**
 * 解析列标识符表达式
 *
 * @author Guan Peixiang
 * @since 2023/12/20
 */
public final class SqlIdentifierExprParser implements BaseSqlExprParser {

    private static final Logger LOGGER = Logger.getLogger(SqlIdentifierExprParser.class.getName());
    private static final SqlIdentifierExprParser INSTANCE = new SqlIdentifierExprParser();

    private SqlIdentifierExprParser() {
    }

    public static SqlIdentifierExprParser getInstance() {
        return INSTANCE;
    }

    public static void parse(SQLIdentifierExpr expr, ExprParseContext context) {
        String columnName = expr.getName();
        LOGGER.fine(() -> "列标识符: " + columnName);

        context.addColumnReference(columnName);
    }

    @Override
    public void process() {
        throw new UnsupportedOperationException("Use parse(SQLIdentifierExpr, ExprParseContext) instead");
    }
}
