package com.magic.sqllineageparser.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 数据血缘解析时字段节点
 *
 * @author Guan Peixiang
 * @since 2023/9/12
 */
public class ColumnNode {

    /**
     * 列所属的表
     */
    private TableNode owner;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 列名
     */
    private String name;

    /**
     * 列别名
     */
    private String alias;

    /**
     * 来源列
     */
    private final List<ColumnNode> sourceColumns = new ArrayList<>();

    /**
     * 此节点表达式
     */
    private String expression;

    /**
     * 字段所在的表树节点ID
     */
    private Long tableTreeNodeId;

    /**
     * 表的表达式
     */
    private String tableExpression;

    /**
     * 字段是否为常量
     */
    private boolean constant;

    // Getters and Setters

    public TableNode getOwner() {
        return owner;
    }

    public void setOwner(TableNode owner) {
        this.owner = owner;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * 获取来源列（不可变视图）
     */
    public List<ColumnNode> getSourceColumns() {
        return Collections.unmodifiableList(sourceColumns);
    }

    /**
     * 添加来源列
     */
    public void addSourceColumn(ColumnNode source) {
        sourceColumns.add(source);
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public Long getTableTreeNodeId() {
        return tableTreeNodeId;
    }

    public void setTableTreeNodeId(Long tableTreeNodeId) {
        this.tableTreeNodeId = tableTreeNodeId;
    }

    public String getTableExpression() {
        return tableExpression;
    }

    public void setTableExpression(String tableExpression) {
        this.tableExpression = tableExpression;
    }

    public boolean isConstant() {
        return constant;
    }

    public void setConstant(boolean constant) {
        this.constant = constant;
    }
}
