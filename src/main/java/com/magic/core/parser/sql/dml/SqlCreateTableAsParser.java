package com.magic.core.parser.sql.dml;

import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableElement;
import com.magic.core.parser.SqlLineageParser;
import com.magic.sqllineageparser.model.DmlLineageInfo;
import com.magic.sqllineageparser.model.DmlOperation;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CREATE TABLE AS SELECT (CTAS) 语句解析器
 * <p>
 * 复用 {@link SqlLineageParser#parseSelect} 解析 AS SELECT 子查询的列血缘，
 * 目标列从 CREATE TABLE 显式 column definitions 提取（若有），否则按 SELECT 输出别名兜底。
 *
 * @author Guan Peixiang
 * @since 2026/05/20
 */
public final class SqlCreateTableAsParser {

    private static final Logger LOGGER = Logger.getLogger(SqlCreateTableAsParser.class.getName());

    private SqlCreateTableAsParser() {
    }

    /**
     * 解析 CREATE TABLE AS SELECT 语句
     *
     * @param stmt Druid 解析得到的 SQLCreateTableStatement
     * @return DML 血缘信息；非 CTAS（无 AS SELECT 子句）返回 null
     */
    public static DmlLineageInfo parse(SQLCreateTableStatement stmt) {
        if (stmt == null || stmt.getSelect() == null) {
            return null;
        }

        DmlLineageInfo info = new DmlLineageInfo();
        info.setOperation(DmlOperation.CTAS);

        SQLExprTableSource ts = stmt.getTableSource();
        if (ts != null) {
            info.setTargetSchema(ts.getSchema());
            info.setTargetTable(ts.getTableName());
        }

        collectColumnDefinitions(stmt, info);
        info.setSourceLineage(SqlLineageParser.parseSelect(stmt.getSelect()));

        LOGGER.log(Level.FINE, () -> "CTAS 解析完成: " + info);
        return info;
    }

    private static void collectColumnDefinitions(SQLCreateTableStatement stmt, DmlLineageInfo info) {
        if (stmt.getTableElementList() == null) {
            return;
        }
        for (SQLTableElement element : stmt.getTableElementList()) {
            if (element instanceof SQLColumnDefinition column && column.getName() != null) {
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
