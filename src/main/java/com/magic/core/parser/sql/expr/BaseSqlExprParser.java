package com.magic.core.parser.sql.expr;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLOver;
import com.alibaba.druid.sql.ast.expr.*;

import java.util.logging.Logger;

/**
 * SQL表达式解析器基础接口 (密封接口)
 * <p>
 * 使用 Java 17 sealed interface 限制实现类
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

    /**
     * 解析 SQL 表达式（分发到具体解析器，带上下文）
     * <p>
     * 解析结果会被收集到 context 中
     *
     * @param sqlExpr SQL 表达式
     * @param context 解析上下文
     */
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
     * 解析 SQL 表达式（无上下文版本，仅日志输出）
     *
     * @param sqlExpr SQL 表达式
     * @deprecated 建议使用带 context 的版本以收集解析结果
     */
    @Deprecated
    static void parserSqlExpr(SQLExpr sqlExpr) {
        parserSqlExpr(sqlExpr, new ExprParseContext());
    }
}
