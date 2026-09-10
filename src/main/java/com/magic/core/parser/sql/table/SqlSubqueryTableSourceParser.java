package com.magic.core.parser.sql.table;

import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * 子查询表源解析器
 * <p>
 * 处理 SQLSubqueryTableSource 类型的表源
 *
 * @author Guan Peixiang
 * @since 2023/9/12
 */
public final class SqlSubqueryTableSourceParser implements BaseTableSourceParser {

    private static final Logger LOGGER = Logger.getLogger(SqlSubqueryTableSourceParser.class.getName());
    private static final SqlSubqueryTableSourceParser INSTANCE = new SqlSubqueryTableSourceParser();

    private SqlSubqueryTableSourceParser() {
    }

    public static SqlSubqueryTableSourceParser getInstance() {
        return INSTANCE;
    }

    @Override
    public void process(String dbType, AtomicInteger sequence, SQLTableSource sqlTableSource) {
        if (!(sqlTableSource instanceof SQLSubqueryTableSource subqueryTableSource)) {
            return;
        }

        var select = subqueryTableSource.getSelect();
        var alias = subqueryTableSource.getAlias();

        LOGGER.fine(() -> "子查询别名: " + alias);
        LOGGER.fine(() -> "子查询内容: " + select);
    }
}
