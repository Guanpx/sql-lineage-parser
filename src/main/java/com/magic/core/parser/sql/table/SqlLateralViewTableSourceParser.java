package com.magic.core.parser.sql.table;

import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.statement.SQLLateralViewTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * LATERAL VIEW 表源解析器
 * <p>
 * 处理 Hive 行转列语法 {@code LATERAL VIEW [OUTER] explode(col) tbl AS c1[, c2]}，
 * 内嵌方法表达式的参数是源列，输出列别名进入上层 SELECT。
 *
 * @author Guan Peixiang
 * @since 2026/05/19
 */
public final class SqlLateralViewTableSourceParser implements BaseTableSourceParser {

    private static final Logger LOGGER = Logger.getLogger(SqlLateralViewTableSourceParser.class.getName());
    private static final SqlLateralViewTableSourceParser INSTANCE = new SqlLateralViewTableSourceParser();

    private SqlLateralViewTableSourceParser() {
    }

    public static SqlLateralViewTableSourceParser getInstance() {
        return INSTANCE;
    }

    @Override
    public void process(String dbType, AtomicInteger sequence, SQLTableSource sqlTableSource) {
        if (!(sqlTableSource instanceof SQLLateralViewTableSource lateralView)) {
            return;
        }

        SQLMethodInvokeExpr method = lateralView.getMethod();
        String methodName = method != null ? method.getMethodName() : "(unknown)";
        String alias = lateralView.getAlias();
        List<SQLName> outputColumns = lateralView.getColumns();
        boolean outer = lateralView.isOuter();

        LOGGER.fine(() -> "LATERAL VIEW" + (outer ? " OUTER" : "") + " " + methodName
                + " " + (alias != null ? alias : "")
                + (outputColumns != null && !outputColumns.isEmpty()
                        ? " AS " + outputColumns
                        : ""));
        LOGGER.fine(() -> "内嵌表源: " + lateralView.getTableSource());
    }
}
