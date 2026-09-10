package com.magic.core.parser.sql.table;

import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * JOIN表源解析器
 * <p>
 * 处理 SQLJoinTableSource 类型的表源
 *
 * @author Guan Peixiang
 * @since 2023/9/12
 */
public final class SqlJoinTableSourceParser implements BaseTableSourceParser {

    private static final Logger LOGGER = Logger.getLogger(SqlJoinTableSourceParser.class.getName());
    private static final SqlJoinTableSourceParser INSTANCE = new SqlJoinTableSourceParser();

    private SqlJoinTableSourceParser() {
    }

    public static SqlJoinTableSourceParser getInstance() {
        return INSTANCE;
    }

    @Override
    public void process(String dbType, AtomicInteger sequence, SQLTableSource sqlTableSource) {
        if (!(sqlTableSource instanceof SQLJoinTableSource joinTableSource)) {
            return;
        }

        var joinType = joinTableSource.getJoinType();
        var left = joinTableSource.getLeft();
        var right = joinTableSource.getRight();
        var condition = joinTableSource.getCondition();

        LOGGER.fine(() -> "JOIN 类型: " + joinType);
        LOGGER.fine(() -> "左表: " + left);
        LOGGER.fine(() -> "右表: " + right);
        LOGGER.fine(() -> "JOIN 条件: " + condition);
    }
}
