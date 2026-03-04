package com.magic.persistence.entity;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isConstant() {
        return constant;
    }

    public void setConstant(boolean constant) {
        this.constant = constant;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

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
