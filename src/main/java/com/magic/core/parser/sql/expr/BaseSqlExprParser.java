package com.magic.core.parser.sql.expr;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLOver;
import com.alibaba.druid.sql.ast.expr.*;

import java.util.logging.Logger;

/**
 * SQL表达式解析器基础接口 (密封接口)
 *
 * @author Guan Peixiang
 * @since 2023/12/20
 */
public sealed interface BaseSqlExprParser
        permits SqlMethodInvokeExprParser,
        SqlCaseExprParser,
        SqlAggregateExprParser,
        SqlBinaryOpExprParser,
        SqlPropertyExprParser,
        SqlIdentifierExprParser,
        SqlNumberExprParser,
        SqlIntegerExprParser,
        SqlCharExprParser,
        SqlOverExprParser,
        SqlCastExprParser {

    Logger LOGGER = Logger.getLogger(BaseSqlExprParser.class.getName());

    /**
     * 处理表达式解析
     */
    void process();

    static void parserSqlExpr(SQLExpr sqlExpr, ExprParseContext context) {
        if (sqlExpr == null) {
            LOGGER.warning("表达式为空");
            return;
        }

        if (sqlExpr instanceof SQLCaseExpr expr) {
            SqlCaseExprParser.parse(expr, context);
        } else if (sqlExpr instanceof SQLAggregateExpr expr) {
            SqlAggregateExprParser.parse(expr, context);
        } else if (sqlExpr instanceof SQLMethodInvokeExpr expr) {
            SqlMethodInvokeExprParser.parse(expr, context);
        } else if (sqlExpr instanceof SQLBinaryOpExpr expr) {
            SqlBinaryOpExprParser.parse(expr, context);
        } else if (sqlExpr instanceof SQLPropertyExpr expr) {
            SqlPropertyExprParser.parse(expr, context);
        } else if (sqlExpr instanceof SQLIdentifierExpr expr) {
            SqlIdentifierExprParser.parse(expr, context);
        } else if (sqlExpr instanceof SQLNumberExpr expr) {
            SqlNumberExprParser.parse(expr, context);
        } else if (sqlExpr instanceof SQLIntegerExpr expr) {
            SqlIntegerExprParser.parse(expr, context);
        } else if (sqlExpr instanceof SQLCharExpr expr) {
            SqlCharExprParser.parse(expr, context);
        } else if (sqlExpr instanceof SQLCastExpr expr) {
            SqlCastExprParser.parse(expr, context);
        } else if (sqlExpr instanceof SQLOver expr) {
            SqlOverExprParser.parse(expr, context);
        } else {
            LOGGER.warning(() -> "不支持的表达式类型: " + sqlExpr.getClass().getSimpleName());
        }
    }

    /**
     * @deprecated 改用带 context 的版本，否则收集不到解析结果
     */
    @Deprecated
    static void parserSqlExpr(SQLExpr sqlExpr) {
        parserSqlExpr(sqlExpr, new ExprParseContext());
    }
}
