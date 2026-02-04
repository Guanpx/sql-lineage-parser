package com.magic.core.parser.sql.expr;

import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;

import java.util.logging.Logger;

/**
 * 解析 table.column 属性表达式
 *
 * @author Guan Peixiang
 * @since 2023/12/20
 */
public final class SqlPropertyExprParser implements BaseSqlExprParser {

    private static final Logger LOGGER = Logger.getLogger(SqlPropertyExprParser.class.getName());
    private static final SqlPropertyExprParser INSTANCE = new SqlPropertyExprParser();

    private SqlPropertyExprParser() {
    }

    public static SqlPropertyExprParser getInstance() {
        return INSTANCE;
    }

    /**
     * 解析属性表达式，收集结果到上下文
     *
     * @param expr    属性表达式
     * @param context 解析上下文
     */
    public static void parse(SQLPropertyExpr expr, ExprParseContext context) {
        String ownerName = expr.getOwnerName();
        String columnName = expr.getName();

        LOGGER.fine(() -> "属性表达式: " + ownerName + "." + columnName);

        // 将列引用添加到上下文，会自动解析别名为真实表名
        context.addSourceColumn(ownerName, columnName);
    }

    @Override
    public void process() {
        throw new UnsupportedOperationException("Use parse(SQLPropertyExpr, ExprParseContext) instead");
    }
}
