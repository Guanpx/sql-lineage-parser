package com.magic.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 血缘节点实体
 * <p>
 * 表示图数据库中的一个节点（表或列）
 *
 * @author Guan Peixiang
 * @since 2023/12/20
 */
@Setter
@Getter
public class LineageNode {

    /**
     * 节点唯一标识
     */
    private String id;

    /**
     * 节点类型：TABLE, COLUMN
     */
    private NodeType nodeType;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 所属数据库
     */
    private String database;

    /**
     * 所属schema
     */
    private String schema;

    /**
     * 所属表（仅列节点有效）
     */
    private String tableName;

    /**
     * 别名
     */
    private String alias;

    /**
     * 是否为常量
     */
    private boolean constant;

    /**
     * 表达式
     */
    private String expression;

    /**
     * 创建时间戳
     */
    private Long createTime;

    /**
     * 扩展属性
     */
    private Map<String, Object> properties = new HashMap<>();

    /**
     * 节点类型枚举
     */
    public enum NodeType {
        TABLE,
        COLUMN
    }

    public LineageNode() {
        this.createTime = System.currentTimeMillis();
    }

    /**
     * 创建表节点
     */
    public static LineageNode ofTable(String database, String tableName) {
        LineageNode node = new LineageNode();
        node.setNodeType(NodeType.TABLE);
        node.setDatabase(database);
        node.setName(tableName);
        node.setId(generateTableId(database, tableName));
        return node;
    }

    /**
     * 创建列节点
     */
    public static LineageNode ofColumn(String database, String tableName, String columnName) {
        LineageNode node = new LineageNode();
        node.setNodeType(NodeType.COLUMN);
        node.setDatabase(database);
        node.setTableName(tableName);
        node.setName(columnName);
        node.setId(generateColumnId(database, tableName, columnName));
        return node;
    }

    /**
     * 创建常量节点
     */
    public static LineageNode ofConstant(String value) {
        LineageNode node = new LineageNode();
        node.setNodeType(NodeType.COLUMN);
        node.setName(value);
        node.setConstant(true);
        node.setId("const:" + value.hashCode());
        return node;
    }

    private static String generateTableId(String database, String tableName) {
        return String.format("table:%s.%s",
                database != null ? database : "default",
                tableName);
    }

    private static String generateColumnId(String database, String tableName, String columnName) {
        return String.format("column:%s.%s.%s",
                database != null ? database : "default",
                tableName != null ? tableName : "unknown",
                columnName);
    }

    // Getters and Setters

    public void addProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LineageNode that = (LineageNode) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LineageNode{" +
                "id='" + id + '\'' +
                ", nodeType=" + nodeType +
                ", name='" + name + '\'' +
                ", tableName='" + tableName + '\'' +
                '}';
    }
}
