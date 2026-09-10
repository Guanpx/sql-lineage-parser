package com.magic.core.parser.sql.ddl;

import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLCreateViewStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableElement;
import com.magic.core.parser.SqlLineageParser;
import com.magic.sqllineageparser.model.DmlLineageInfo;
import com.magic.sqllineageparser.model.DmlOperation;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CREATE VIEW ... AS SELECT 语句解析器
 * <p>
 * 视图定义本质是一段 SELECT，因此复用 {@link SqlLineageParser#parseSelect}
 * 解析 AS 子查询的列血缘，并把视图名作为目标表挂到 {@link DmlLineageInfo} 上。
 * 显式列覆盖（{@code CREATE VIEW v (c1, c2) AS ...}）从 {@link SQLCreateViewStatement#getColumns()}
 * 提取（元素为 {@link SQLColumnDefinition}），缺失时按 SELECT 输出别名兜底。
 *
 * @author Guan Peixiang
 * @since 2026/07/07
 */
public final class SqlCreateViewParser {

    private static final Logger LOGGER = Logger.getLogger(SqlCreateViewParser.class.getName());

    private SqlCreateViewParser() {
    }

    /**
     * 解析 CREATE VIEW 语句
     *
     * @param stmt Druid 解析得到的 SQLCreateViewStatement
     * @return 视图血缘信息；缺少 AS 子查询时返回 null
     */
    public static DmlLineageInfo parse(SQLCreateViewStatement stmt) {
        if (stmt == null || stmt.getSubQuery() == null) {
            return null;
        }

        DmlLineageInfo info = new DmlLineageInfo();
        info.setOperation(DmlOperation.CREATE_VIEW);

        SQLExprTableSource ts = stmt.getTableSource();
        if (ts != null) {
            info.setTargetSchema(ts.getSchema());
            info.setTargetTable(ts.getTableName());
        }

        collectExplicitColumns(stmt.getColumns(), info);
        info.setOutputColumns(SqlLineageParser.parseSelect(stmt.getSubQuery()));

        LOGGER.log(Level.FINE, () -> "CREATE VIEW 解析完成: " + info);
        return info;
    }

    /**
     * 从 CREATE VIEW 的显式列定义中提取目标列名
     */
    private static void collectExplicitColumns(List<SQLTableElement> columns, DmlLineageInfo info) {
        if (columns == null) {
            return;
        }
        for (SQLTableElement element : columns) {
            if (element instanceof SQLColumnDefinition column
                    && column.getName() != null
                    && column.getName().getSimpleName() != null) {
                info.addTargetColumn(stripIdentifier(column.getName().getSimpleName()));
            }
        }
    }

    private static String stripIdentifier(String raw) {
        if (raw == null || raw.length() < 2) {
            return raw;
        }
        char first = raw.charAt(0);
        char last = raw.charAt(raw.length() - 1);
        if ((first == '`' && last == '`') || (first == '"' && last == '"')) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }
}

