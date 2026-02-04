package com.magic.core.parser.sql.expr;

import com.alibaba.druid.sql.ast.statement.*;
import com.magic.sqllineageparser.model.ColumnNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表达式解析上下文
 * <p>
 * 保存解析过程中的状态信息和收集解析结果
 *
 * @author Guan Peixiang
 * @since 2023/12/20
 */
public class ExprParseContext {

    /**
     * 表别名 -> 真实表名 的映射
     */
    private final Map<String, String> aliasToTableMap = new HashMap<>();

    /**
     * 当前正在解析的目标列节点
     */
    private ColumnNode currentColumn;

    /**
     * 解析过程中收集到的所有来源列
     */
    private final List<ColumnNode> sourceColumns = new ArrayList<>();

    /**
     * 创建空的解析上下文
     */
    public ExprParseContext() {
    }

    /**
     * 从 FROM 子句创建解析上下文
     *
     * @param tableSource FROM 子句的表源
     * @return 解析上下文
     */
    public static ExprParseContext fromTableSource(SQLTableSource tableSource) {
        ExprParseContext context = new ExprParseContext();
        context.collectTableAlias(tableSource);
        return context;
    }

    /**
     * 设置当前正在解析的目标列
     */
    public void setCurrentColumn(ColumnNode column) {
        this.currentColumn = column;
        this.sourceColumns.clear();
    }

    /**
     * 获取当前目标列
     */
    public ColumnNode getCurrentColumn() {
        return currentColumn;
    }

    /**
     * 添加来源列
     *
     * @param tableName  表名（或别名）
     * @param columnName 列名
     */
    public void addSourceColumn(String tableName, String columnName) {
        ColumnNode source = new ColumnNode();
        // 解析真实表名
        String realTableName = resolveTableName(tableName);
        source.setTableName(realTableName);
        source.setName(columnName);
        source.setConstant(false);
        sourceColumns.add(source);

        // 同时添加到当前列的来源列表中
        if (currentColumn != null) {
            currentColumn.addSourceColumn(source);
        }
    }

    /**
     * 添加常量来源（无表名）
     *
     * @param value 常量值
     */
    public void addConstantSource(String value) {
        ColumnNode source = new ColumnNode();
        source.setName(value);
        source.setConstant(true);
        sourceColumns.add(source);

        if (currentColumn != null) {
            currentColumn.addSourceColumn(source);
        }
    }

    /**
     * 添加无表名的列引用
     *
     * @param columnName 列名
     */
    public void addColumnReference(String columnName) {
        ColumnNode source = new ColumnNode();
        source.setName(columnName);
        source.setConstant(false);
        sourceColumns.add(source);

        if (currentColumn != null) {
            currentColumn.addSourceColumn(source);
        }
    }

    /**
     * 获取所有收集到的来源列
     */
    public List<ColumnNode> getSourceColumns() {
        return new ArrayList<>(sourceColumns);
    }

    /**
     * 解析真实表名（将别名转换为真实表名）
     *
     * @param aliasOrTableName 别名或表名
     * @return 真实表名
     */
    public String resolveTableName(String aliasOrTableName) {
        if (aliasOrTableName == null) {
            return null;
        }
        return aliasToTableMap.getOrDefault(aliasOrTableName, aliasOrTableName);
    }

    /**
     * 添加表别名映射
     *
     * @param alias     别名
     * @param tableName 真实表名
     */
    public void addTableAlias(String alias, String tableName) {
        aliasToTableMap.put(alias, tableName);
    }

    /**
     * 获取别名映射（只读）
     */
    public Map<String, String> getAliasToTableMap() {
        return Map.copyOf(aliasToTableMap);
    }

    /**
     * 递归收集表别名
     */
    private void collectTableAlias(SQLTableSource tableSource) {
        if (tableSource == null) {
            return;
        }

        if (tableSource instanceof SQLExprTableSource exprTable) {
            String tableName = exprTable.getExpr().toString();
            String alias = exprTable.getAlias();
            if (alias != null && !alias.isEmpty()) {
                aliasToTableMap.put(alias, tableName);
            } else {
                aliasToTableMap.put(tableName, tableName);
            }
        } else if (tableSource instanceof SQLJoinTableSource joinTable) {
            collectTableAlias(joinTable.getLeft());
            collectTableAlias(joinTable.getRight());
        } else if (tableSource instanceof SQLSubqueryTableSource subquery) {
            String alias = subquery.getAlias();
            if (alias != null && !alias.isEmpty()) {
                aliasToTableMap.put(alias, "(subquery:" + alias + ")");
            }
        } else if (tableSource instanceof SQLUnionQueryTableSource unionTable) {
            String alias = unionTable.getAlias();
            if (alias != null && !alias.isEmpty()) {
                aliasToTableMap.put(alias, "(union:" + alias + ")");
            }
        }
    }

    @Override
    public String toString() {
        return "ExprParseContext{" +
                "aliasToTableMap=" + aliasToTableMap +
                ", currentColumn=" + (currentColumn != null ? currentColumn.getName() : "null") +
                ", sourceColumnsCount=" + sourceColumns.size() +
                '}';
    }
}
