package com.magic.core.parser.sql.table;

import com.alibaba.druid.sql.ast.statement.SQLTableSource;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 表源解析器基础接口 (密封接口)
 *
 * @author Guan Peixiang
 * @since 2023/9/12
 */
public sealed interface BaseTableSourceParser
        permits SqlExprTableSourceParser,
        SqlJoinTableSourceParser,
        SqlSubqueryTableSourceParser,
        SqlUnionQueryTableSourceParser,
        SqlWithSubqueryTableSourceParser,
        SqlLateralViewTableSourceParser {

    void process(String dbType, AtomicInteger sequence, SQLTableSource sqlTableSource);
}
