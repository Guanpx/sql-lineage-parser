package com.magic.core.parser.sql.expr;

import com.alibaba.druid.sql.ast.expr.SQLNumberExpr;

import java.util.logging.Logger;

/**
 * 解析数字常量表达式
 *
 * @author Guan Peixiang
 * @since 2023/12/20
 */
public final class SqlNumberExprParser implements BaseSqlExprParser {

    private static final Logger LOGGER = Logger.getLogger(SqlNumberExprParser.class.getName());
    private static final SqlNumberExprParser INSTANCE = new SqlNumberExprParser();

    private SqlNumberExprParser() {
    }

    public static SqlNumberExprParser getInstance() {
        return INSTANCE;
    }

    /**
     * 解析数字常量，收集结果到上下文
     *
     * @param expr    数字常量表达式
     * @param context 解析上下文
     */
    public static void parse(SQLNumberExpr expr, ExprParseContext context) {
        String value = String.valueOf(expr.getNumber());
        LOGGER.fine(() -> "数字常量: " + value);

        // 常量值
        context.addConstantSource(value);
    }

    @Override
    public void process() {
        throw new UnsupportedOperationException("Use parse(SQLNumberExpr, ExprParseContext) instead");
    }
}
