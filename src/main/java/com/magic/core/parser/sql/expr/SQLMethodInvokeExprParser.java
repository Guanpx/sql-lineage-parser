package com.magic.core.parser.sql.expr;

/**
 * 解析SQL查询语句中的函数调用
 *
 * @author Guan Peixiang
 * @date 2023/12/20
 */
public class SQLMethodInvokeExprParser implements BaseSqlExprParser {

    private static final SQLMethodInvokeExprParser INSTANCE = new SQLMethodInvokeExprParser();

    private SQLMethodInvokeExprParser() {
    }

    public static SQLMethodInvokeExprParser getInstance() {
        return INSTANCE;
    }

    @Override
    public void process() {
        // TODO: 实现函数调用表达式的解析
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
