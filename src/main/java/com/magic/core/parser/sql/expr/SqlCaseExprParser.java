package com.magic.core.parser.sql.expr;

import com.alibaba.druid.sql.ast.expr.SQLCaseExpr;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 解析 CASE WHEN 表达式
 *
 * @author Guan Peixiang
 * @since 2023/12/20
 */
public final class SqlCaseExprParser implements BaseSqlExprParser {

    private static final Logger LOGGER = Logger.getLogger(SqlCaseExprParser.class.getName());
    private static final SqlCaseExprParser INSTANCE = new SqlCaseExprParser();

    private SqlCaseExprParser() {
    }

    public static SqlCaseExprParser getInstance() {
        return INSTANCE;
    }

    /**
     * 解析 CASE WHEN 表达式，收集结果到上下文
     *
     * @param expr    CASE WHEN 表达式
     * @param context 解析上下文
     */
    public static void parse(SQLCaseExpr expr, ExprParseContext context) {
        LOGGER.fine("解析 CASE WHEN 表达式");

        // 解析各个 WHEN 分支的值表达式
        for (SQLCaseExpr.Item item : expr.getItems()) {
            LOGGER.log(Level.FINER, () -> "WHEN 条件: " + item.getConditionExpr());
            BaseSqlExprParser.parserSqlExpr(item.getValueExpr(), context);
        }

        // 解析 ELSE 分支
        if (expr.getElseExpr() != null) {
            LOGGER.finer("解析 ELSE 分支");
            BaseSqlExprParser.parserSqlExpr(expr.getElseExpr(), context);
        }
    }

    @Override
    public void process() {
        throw new UnsupportedOperationException("Use parse(SQLCaseExpr, ExprParseContext) instead");
    }
}
