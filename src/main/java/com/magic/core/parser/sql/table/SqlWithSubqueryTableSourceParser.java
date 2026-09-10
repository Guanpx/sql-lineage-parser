package com.magic.core.parser.sql.table;

import com.alibaba.druid.sql.ast.statement.SQLTableSource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * WITH子查询表源解析器
 * <p>
 * 处理 WITH (CTE) 子查询类型的表源
 *
 * @author Guan Peixiang
 * @since 2023/12/19
 */
public final class SqlWithSubqueryTableSourceParser implements BaseTableSourceParser {

    private static final Logger LOGGER = Logger.getLogger(SqlWithSubqueryTableSourceParser.class.getName());
    private static final SqlWithSubqueryTableSourceParser INSTANCE = new SqlWithSubqueryTableSourceParser();

    private SqlWithSubqueryTableSourceParser() {
    }

    public static SqlWithSubqueryTableSourceParser getInstance() {
        return INSTANCE;
    }

    @Override
    public void process(String dbType, AtomicInteger sequence, SQLTableSource sqlTableSource) {
        LOGGER.fine("处理 WITH 子查询表源");
    }
}
