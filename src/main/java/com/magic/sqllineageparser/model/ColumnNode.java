package com.magic.sqllineageparser.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据血缘解析时字段节点
 */
public class ColumnNode {

    /**
     * 列所属的表 todo
     */
    private TableNode owner;
    /**
     * 表
     */
    private String tableName;
    /**
     * 名称
     */
    private String name;
    /**
     * 别名
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
     * 字段所在的表树Id
     */
    private Long tableTreeNodeId;

    /**
     * 表的表达式
     */
    private String tableExpression;

    /**
     * 字段是否为常量
     */
    private boolean isConstant = false;

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

    public List<ColumnNode> getSourceColumns() {
        return sourceColumns;
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
        return isConstant;
    }

    public void setConstant(boolean constant) {
        isConstant = constant;
    }
}
