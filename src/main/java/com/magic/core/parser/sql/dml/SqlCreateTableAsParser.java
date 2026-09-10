package com.magic.core.parser.sql.dml;

import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableElement;
import com.magic.core.parser.SqlLineageParser;
import com.magic.sqllineageparser.model.ColumnNode;
import com.magic.sqllineageparser.model.DmlLineageInfo;
import com.magic.sqllineageparser.model.DmlOperation;
import com.magic.sqllineageparser.model.TreeNode;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CREATE TABLE AS SELECT (CTAS) 语句解析器
 * <p>
 * 复用 {@link SqlLineageParser#parseSelect} 解析 AS SELECT 子查询的列血缘，
 * 目标列从 CTAS 中的声明列提取(table body)，否则按 AS SELECT 查询的别名兜底。
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
     * @return DML 血缘信息；非 CTAS（无AS SELECT子句）返回 null, 如果CTAS不指名列则取SELECT全部列
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
        info.setSourceLineage(SqlLineageParser.parseSelect(stmt.getSelect()));

        // 解析CTAS的声明列
        if(stmt.getTableElementList() == null || stmt.getTableElementList().isEmpty()){
            collectColumnDefinitions(info);
        } else {
            collectColumnDefinitions(stmt, info);
        }
        LOGGER.log(Level.FINE, () -> "CTAS 解析完成: " + info);
        return info;
    }

    /**
     * stmt.getTableElementList().isEmpty()
     * 对应CATS无声明列的情况，此时会从select获取补充(下重载方法)
     */
    private static void collectColumnDefinitions(SQLCreateTableStatement stmt, DmlLineageInfo info) {
        for (SQLTableElement element : stmt.getTableElementList()) {
            if (element instanceof SQLColumnDefinition column && column.getName() != null) {
                info.addTargetColumn(stripIdentifier(column.getName().getSimpleName()));
            }
        }
    }

    private static void collectColumnDefinitions(DmlLineageInfo info) {
        TreeNode<ColumnNode> sourceLineage = info.getSourceLineage();
        for (TreeNode<ColumnNode> element : sourceLineage.getChildren()) {
            ColumnNode columnNode = element.getValue();
            info.addTargetColumn(columnNode.getAlias() == null ? columnNode.getName() : columnNode.getAlias());
        }
    }

    /**
     * 去除字段的反引号 双引号
     * TODO 方法重复
     * @param raw 入参
     * @return 去除字段的反引号  双引号
     */
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
