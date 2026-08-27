package com.magic.core.parser.sql.alter;

import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlAlterTableModifyColumn;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableAddColumn;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableAlterColumn;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableDropColumnItem;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableItem;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableRenameColumn;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.magic.sqllineageparser.model.AlterColumnChange;
import com.magic.sqllineageparser.model.AlterTableInfo;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ALTER TABLE 语句解析器
 * <p>
 * 解析 Hive / 标准 SQL 的 ALTER TABLE 语句，支持
 * ADD COLUMNS / DROP COLUMN / RENAME COLUMN / CHANGE COLUMN 等结构变更。
     * CHANGE COLUMN 在结果模型中统一表达为 MODIFY 动作。
 *
 * @author Guan Peixiang
 * @since 2026/05/19
 */
public final class SqlAlterTableParser {

    private static final Logger LOGGER = Logger.getLogger(SqlAlterTableParser.class.getName());

    private SqlAlterTableParser() {
    }

    /**
     * 解析 ALTER TABLE 语句
     *
     * @param stmt 已经由 Druid 解析得到的 SQLAlterTableStatement
     * @return 表变更信息
     */
    public static AlterTableInfo parse(SQLAlterTableStatement stmt) {
        if (stmt == null) {
            return null;
        }

        AlterTableInfo info = new AlterTableInfo();
        SQLExprTableSource ts = stmt.getTableSource();
        if (ts != null) {
            info.setSchema(ts.getSchema());
            info.setTableName(ts.getTableName());
        }

        LOGGER.log(Level.FINE, () -> "解析 ALTER TABLE: " + info.getQualifiedTableName());

        if (stmt.getItems() == null) {
            return info;
        }

        for (SQLAlterTableItem item : stmt.getItems()) {
            parseItem(item, info);
        }
        return info;
    }

    private static void parseItem(SQLAlterTableItem item, AlterTableInfo info) {
        if (item instanceof SQLAlterTableAddColumn addColumn) {
            for (SQLColumnDefinition column : addColumn.getColumns()) {
                String name = stripIdentifier(column.getName().getSimpleName());
                String type = column.getDataType() != null ? column.getDataType().toString() : null;
                String comment = extractComment(column);
                info.addChange(AlterColumnChange.ofAdd(name, type, comment));
            }
        } else if (item instanceof SQLAlterTableDropColumnItem dropColumn) {
            for (SQLName name : dropColumn.getColumns()) {
                info.addChange(AlterColumnChange.ofDrop(stripIdentifier(name.getSimpleName())));
            }
        } else if (item instanceof SQLAlterTableRenameColumn rename) {
            String oldName = stripIdentifier(rename.getColumn().getSimpleName());
            String newName = stripIdentifier(rename.getTo().getSimpleName());
            info.addChange(AlterColumnChange.ofRename(oldName, newName));
        } else if (item instanceof SQLAlterTableAlterColumn alterColumn
                && alterColumn.getColumn() != null) {
            SQLColumnDefinition column = alterColumn.getColumn();
            String oldName = alterColumn.getOriginColumn() == null
                    ? null
                    : stripIdentifier(alterColumn.getOriginColumn().getSimpleName());
            String newName = stripIdentifier(column.getName().getSimpleName());
            String type = column.getDataType() == null ? null : column.getDataType().toString();
            boolean renamed = oldName != null && !oldName.equals(newName);

            info.addChange(AlterColumnChange.ofModify(
                    renamed ? oldName : newName,
                    renamed ? newName : null,
                    type,
                    extractComment(column)
            ));
        } else if (item instanceof MySqlAlterTableModifyColumn modifyColumn
                && modifyColumn.getNewColumnDefinition() != null) {
            SQLColumnDefinition column = modifyColumn.getNewColumnDefinition();
            info.addChange(AlterColumnChange.ofModify(
                    stripIdentifier(column.getName().getSimpleName()),
                    null,
                    column.getDataType() == null ? null : column.getDataType().toString(),
                    extractComment(column)
            ));
        } else {
            LOGGER.log(Level.FINE, () -> "未识别的 ALTER 子项: " + item.getClass().getSimpleName());
        }
    }

    private static String extractComment(SQLColumnDefinition column) {
        if (column.getComment() instanceof SQLCharExpr charExpr) {
            return charExpr.getText();
        }
        return column.getComment() != null ? column.getComment().toString() : null;
    }

    private static String stripIdentifier(String raw) {
        if (raw == null) {
            return null;
        }
        if (raw.length() >= 2) {
            char first = raw.charAt(0);
            char last = raw.charAt(raw.length() - 1);
            if ((first == '`' && last == '`') || (first == '"' && last == '"')) {
                return raw.substring(1, raw.length() - 1);
            }
        }
        return raw;
    }
}
