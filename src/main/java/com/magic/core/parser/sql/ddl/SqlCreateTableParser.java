package com.magic.core.parser.sql.ddl;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.magic.sqllineageparser.model.CreateTableInfo;
import com.magic.sqllineageparser.model.TableColumnMeta;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 纯 CREATE TABLE 语句解析器
 * <p>
 * 提取表名、表注释、字段名、字段类型、默认值、主键标记与分区字段。
 * CREATE TABLE AS SELECT 的数据血缘由 SqlCreateTableAsParser 处理。
 *
 * @author Guan Peixiang
 * @since 2026/08/26
 */
public final class SqlCreateTableParser {

    private static final Logger LOGGER = Logger.getLogger(SqlCreateTableParser.class.getName());

    private SqlCreateTableParser() {
    }

    /**
     * 解析不带 AS SELECT 的 CREATE TABLE 语句
     *
     * @param stmt Druid 解析得到的 SQLCreateTableStatement
     * @return 建表元信息；CTAS 返回 null
     */
    public static CreateTableInfo parse(SQLCreateTableStatement stmt) {
        if (stmt == null || stmt.getSelect() != null) {
            return null;
        }

        CreateTableInfo info = new CreateTableInfo();
        SQLExprTableSource tableSource = stmt.getTableSource();
        if (tableSource != null) {
            info.setSchema(tableSource.getSchema());
            info.setTableName(tableSource.getTableName());
        }
        info.setComment(extractText(stmt.getComment()));

        if (stmt.getColumnDefinitions() != null) {
            for (SQLColumnDefinition column : stmt.getColumnDefinitions()) {
                TableColumnMeta meta = parseColumn(column);
                if (meta != null) {
                    info.addColumn(meta);
                }
            }
        }
        if (stmt.getPartitionColumns() != null) {
            for (SQLColumnDefinition column : stmt.getPartitionColumns()) {
                TableColumnMeta meta = parseColumn(column);
                if (meta != null) {
                    info.addPartitionColumn(meta);
                }
            }
        }

        LOGGER.log(Level.FINE, () -> "CREATE TABLE 解析完成: " + info);
        return info;
    }

    private static TableColumnMeta parseColumn(SQLColumnDefinition column) {
        if (column == null || column.getName() == null) {
            return null;
        }
        String dataType = column.getDataType() == null ? null : column.getDataType().toString();
        return TableColumnMeta.of(
                stripIdentifier(column.getName().getSimpleName()),
                dataType,
                extractText(column.getComment()),
                column.getDefaultExpr() == null ? null : column.getDefaultExpr().toString(),
                column.isPrimaryKey()
        );
    }

    private static String extractText(SQLExpr expr) {
        if (expr == null) {
            return null;
        }
        if (expr instanceof SQLCharExpr charExpr) {
            return charExpr.getText();
        }
        return expr.toString();
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
