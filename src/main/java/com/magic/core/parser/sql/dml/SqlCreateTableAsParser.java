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
 * <p>
 * 在 {@link DmlLineageInfo#getTargetColumnAt(int)} 统一处理血缘映射：AS SELECT 列 => CTAS 目标列
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
     * @return DML 血缘信息；非CTAS（无AS SELECT子句）返回 null
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
        info.setOutputColumns(SqlLineageParser.parseSelect(stmt.getSelect()));

        // 解析CTAS的声明列；无声明列时由 getTargetColumnAt 回退到 SELECT 输出名
        if(stmt.getTableElementList() != null && !stmt.getTableElementList().isEmpty()){
            collectColumnDefinitions(stmt, info);
        }
        LOGGER.log(Level.FINE, () -> "CTAS 解析完成: " + info);
        return info;
    }

    /**
     * 从 CREATE TABLE 的声明列定义中提取目标列名
     */
    private static void collectColumnDefinitions(SQLCreateTableStatement stmt, DmlLineageInfo info) {
        for (SQLTableElement element : stmt.getTableElementList()) {
            if (element instanceof SQLColumnDefinition column && column.getName() != null) {
                info.addTargetColumn(stripIdentifier(column.getName().getSimpleName()));
            }
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
