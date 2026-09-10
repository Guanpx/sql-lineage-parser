package com.magic.core.parser.sql.table;

import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUnionQueryTableSource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Union查询表源解析器
 * <p>
 * 处理 SQLUnionQueryTableSource 类型的表源
 *
 * @author Guan Peixiang
 * @since 2023/9/12
 */
public final class SqlUnionQueryTableSourceParser implements BaseTableSourceParser {

    private static final Logger LOGGER = Logger.getLogger(SqlUnionQueryTableSourceParser.class.getName());
    private static final SqlUnionQueryTableSourceParser INSTANCE = new SqlUnionQueryTableSourceParser();

    private SqlUnionQueryTableSourceParser() {
    }

    public static SqlUnionQueryTableSourceParser getInstance() {
        return INSTANCE;
    }

    @Override
    public void process(String dbType, AtomicInteger sequence, SQLTableSource sqlTableSource) {
        if (!(sqlTableSource instanceof SQLUnionQueryTableSource unionQueryTableSource)) {
            return;
        }

        var unionQuery = unionQueryTableSource.getUnion();
        var alias = unionQueryTableSource.getAlias();

        LOGGER.fine(() -> "UNION 查询别名: " + alias);
        LOGGER.fine(() -> "UNION 查询: " + unionQuery);
    }
}
