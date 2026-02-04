package com.magic.core.parser.sql.expr;

import com.alibaba.druid.sql.ast.expr.SQLCharExpr;

import java.util.logging.Logger;

/**
 * 解析字符常量表达式
 *
 * @author Guan Peixiang
 * @since 2023/12/20
 */
public final class SqlCharExprParser implements BaseSqlExprParser {

    private static final Logger LOGGER = Logger.getLogger(SqlCharExprParser.class.getName());
    private static final SqlCharExprParser INSTANCE = new SqlCharExprParser();

    private SqlCharExprParser() {
    }

    public static SqlCharExprParser getInstance() {
        return INSTANCE;
    }

    /**
     * 解析字符常量，收集结果到上下文
     *
     * @param expr    字符常量表达式
     * @param context 解析上下文
     */
    public static void parse(SQLCharExpr expr, ExprParseContext context) {
        String value = "'" + expr.getText() + "'";
        LOGGER.fine(() -> "字符常量: " + value);

        // 常量值
        context.addConstantSource(value);
    }

    @Override
    public void process() {
        throw new UnsupportedOperationException("Use parse(SQLCharExpr, ExprParseContext) instead");
    }
}
