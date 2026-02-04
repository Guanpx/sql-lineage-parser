package com.magic.core.parser.sql.expr;

import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;

import java.util.logging.Logger;

/**
 * 解析整数常量表达式
 *
 * @author Guan Peixiang
 * @since 2023/12/20
 */
public final class SqlIntegerExprParser implements BaseSqlExprParser {

    private static final Logger LOGGER = Logger.getLogger(SqlIntegerExprParser.class.getName());
    private static final SqlIntegerExprParser INSTANCE = new SqlIntegerExprParser();

    private SqlIntegerExprParser() {
    }

    public static SqlIntegerExprParser getInstance() {
        return INSTANCE;
    }

    /**
     * 解析整数常量，收集结果到上下文
     *
     * @param expr    整数常量表达式
     * @param context 解析上下文
     */
    public static void parse(SQLIntegerExpr expr, ExprParseContext context) {
        String value = String.valueOf(expr.getNumber());
        LOGGER.fine(() -> "整数常量: " + value);

        // 常量值
        context.addConstantSource(value);
    }

    @Override
    public void process() {
        throw new UnsupportedOperationException("Use parse(SQLIntegerExpr, ExprParseContext) instead");
    }
}
