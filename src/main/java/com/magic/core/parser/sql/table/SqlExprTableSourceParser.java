package com.magic.core.parser.sql.table;

import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * 普通表源解析器
 * <p>
 * 处理 SQLExprTableSource 类型的表源
 *
 * @author Guan Peixiang
 * @since 2023/9/12
 */
public final class SqlExprTableSourceParser implements BaseTableSourceParser {

    private static final Logger LOGGER = Logger.getLogger(SqlExprTableSourceParser.class.getName());
    private static final SqlExprTableSourceParser INSTANCE = new SqlExprTableSourceParser();

    private SqlExprTableSourceParser() {
    }

    public static SqlExprTableSourceParser getInstance() {
        return INSTANCE;
    }

    @Override
    public void process(String dbType, AtomicInteger sequence, SQLTableSource sqlTableSource) {
        if (!(sqlTableSource instanceof SQLExprTableSource exprTableSource)) {
            return;
        }

        var expr = exprTableSource.getExpr();
        var alias = exprTableSource.getAlias();

        LOGGER.fine(() -> "解析普通表: " + expr + (alias != null ? " AS " + alias : ""));
    }
}
