package com.magic.core.parser.sql.dml;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLAssignItem;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.magic.core.parser.SqlLineageParser;
import com.magic.sqllineageparser.model.DmlLineageInfo;
import com.magic.sqllineageparser.model.DmlOperation;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * INSERT INTO ... SELECT / INSERT OVERWRITE ... SELECT 语句解析器
 * <p>
 * 提取目标表、显式目标列、分区信息，并复用 {@link SqlLineageParser#parseSelect}
 * 解析源 SELECT 的列血缘。
 *
 * @author Guan Peixiang
 * @since 2026/05/20
 */
public final class SqlInsertParser {

    private static final Logger LOGGER = Logger.getLogger(SqlInsertParser.class.getName());

    private SqlInsertParser() {
    }

    /**
     * 解析 INSERT 语句
     *
     * @param stmt Druid 解析得到的 SQLInsertStatement（含 Hive 方言子类）
     * @return DML 血缘信息；非 INSERT ... SELECT 形式返回 null
     */
    public static DmlLineageInfo parse(SQLInsertStatement stmt) {
        if (stmt == null) {
            return null;
        }
        if (stmt.getQuery() == null) {
            LOGGER.warning("INSERT 语句缺少 SELECT 子查询，跳过血缘解析");
            return null;
        }

        DmlLineageInfo info = new DmlLineageInfo();
        info.setOperation(stmt.isOverwrite() ? DmlOperation.INSERT_OVERWRITE : DmlOperation.INSERT_INTO);

        SQLExprTableSource ts = stmt.getTableSource();
        if (ts != null) {
            info.setTargetSchema(ts.getSchema());
            info.setTargetTable(ts.getTableName());
        }

        collectExplicitColumns(stmt.getColumns(), info);
        collectPartitions(stmt.getPartitions(), info);

        info.setSourceLineage(SqlLineageParser.parseSelect(stmt.getQuery()));

        LOGGER.log(Level.FINE, () -> "INSERT 解析完成: " + info);
        return info;
    }

    private static void collectExplicitColumns(List<SQLExpr> columns, DmlLineageInfo info) {
        if (columns == null) {
            return;
        }
        for (SQLExpr expr : columns) {
            String name = identifierName(expr);
            if (name != null) {
                info.addTargetColumn(name);
            }
        }
    }

    private static void collectPartitions(List<SQLAssignItem> partitions, DmlLineageInfo info) {
        if (partitions == null) {
            return;
        }
        for (SQLAssignItem item : partitions) {
            String column = identifierName(item.getTarget());
            String value = item.getValue() != null ? item.getValue().toString() : null;
            info.addPartition(column, value);
        }
    }

    private static String identifierName(SQLExpr expr) {
        if (expr instanceof SQLIdentifierExpr id) {
            return stripIdentifier(id.getName());
        }
        if (expr instanceof SQLPropertyExpr p) {
            return stripIdentifier(p.getName());
        }
        return expr != null ? stripIdentifier(expr.toString()) : null;
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
