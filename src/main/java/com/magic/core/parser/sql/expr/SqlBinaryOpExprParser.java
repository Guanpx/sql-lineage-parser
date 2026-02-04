package com.magic.core.parser.sql.expr;

import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;

import java.util.logging.Logger;

/**
 * 解析二元运算表达式
 *
 * @author Guan Peixiang
 * @since 2023/12/20
 */
public final class SqlBinaryOpExprParser implements BaseSqlExprParser {

    private static final Logger LOGGER = Logger.getLogger(SqlBinaryOpExprParser.class.getName());
    private static final SqlBinaryOpExprParser INSTANCE = new SqlBinaryOpExprParser();

    private SqlBinaryOpExprParser() {
    }

    public static SqlBinaryOpExprParser getInstance() {
        return INSTANCE;
    }

    /**
     * 解析二元运算表达式，收集结果到上下文
     *
     * @param expr    二元运算表达式
     * @param context 解析上下文
     */
    public static void parse(SQLBinaryOpExpr expr, ExprParseContext context) {
        LOGGER.fine(() -> "二元运算: " + expr.getOperator());

        // 递归解析左右操作数
        BaseSqlExprParser.parserSqlExpr(expr.getLeft(), context);
        BaseSqlExprParser.parserSqlExpr(expr.getRight(), context);
    }

    @Override
    public void process() {
        throw new UnsupportedOperationException("Use parse(SQLBinaryOpExpr, ExprParseContext) instead");
    }
}
