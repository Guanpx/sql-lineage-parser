package com.magic.sqllineageparser.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 数据血缘解析时字段节点
 *
 * @author Guan Peixiang
 * @since 2023/9/12
 */
@Getter
@Setter
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

}
