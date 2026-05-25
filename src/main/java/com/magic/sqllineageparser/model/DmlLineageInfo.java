package com.magic.sqllineageparser.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DML 语句血缘解析结果
 * <p>
 * 描述 INSERT INTO / INSERT OVERWRITE / CTAS 等语句的目标表、目标列与源 SELECT 血缘。
 * <p>
 * 目标列与源列按位置对齐：第 i 个目标列的来源 = {@code sourceLineage} 第 i 个子节点的 sourceColumns。
 *
 * @author Guan Peixiang
 * @since 2026/05/20
 */
public class DmlLineageInfo {

    /**
     * 操作类型
     */
    private DmlOperation operation;

    /**
     * 目标表 schema
     */
    private String targetSchema;

    /**
     * 目标表名
     */
    private String targetTable;

    /**
     * 目标列名（按位置对应 sourceLineage 的输出列）。
     * <p>
     * - INSERT INTO t (c1, c2) SELECT ... : 显式指定，长度 = SELECT 列数<br>
     * - INSERT INTO t SELECT ... : 未指定，列表为空<br>
     * - CTAS: 来自 CREATE TABLE 的 column definitions 或 SELECT 输出名
     */
    private final List<String> targetColumns = new ArrayList<>();

    /**
     * 分区列名 -> 分区值（静态分区有值，动态分区值为 null）
     */
    private final Map<String, String> partitions = new LinkedHashMap<>();

    /**
     * 源 SELECT 的血缘树根节点。children 为输出列。
     */
    private TreeNode<ColumnNode> sourceLineage;

    public DmlOperation getOperation() {
        return operation;
    }

    public void setOperation(DmlOperation operation) {
        this.operation = operation;
    }

    public String getTargetSchema() {
        return targetSchema;
    }

    public void setTargetSchema(String targetSchema) {
        this.targetSchema = targetSchema;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public List<String> getTargetColumns() {
        return Collections.unmodifiableList(targetColumns);
    }

    public void addTargetColumn(String column) {
        if (column != null) {
            targetColumns.add(column);
        }
    }

    public Map<String, String> getPartitions() {
        return Collections.unmodifiableMap(partitions);
    }

    public void addPartition(String column, String value) {
        if (column != null) {
            partitions.put(column, value);
        }
    }

    public TreeNode<ColumnNode> getSourceLineage() {
        return sourceLineage;
    }

    public void setSourceLineage(TreeNode<ColumnNode> sourceLineage) {
        this.sourceLineage = sourceLineage;
    }

    /**
     * 全限定目标表名
     */
    public String getQualifiedTargetTable() {
        if (targetSchema == null || targetSchema.isEmpty()) {
            return targetTable;
        }
        return targetSchema + "." + targetTable;
    }

    /**
     * 输出列数
     */
    public int getOutputColumnCount() {
        return sourceLineage == null ? 0 : sourceLineage.getChildren().size();
    }

    /**
     * 获取第 i 个输出列对应的目标列名。
     * <p>
     * 优先使用显式 targetColumns；缺失时回退到 SELECT 列的 alias；
     * 再缺失时从 expression 中提取列名（如 {@code a.name} → {@code name}）。
     */
    public String getTargetColumnAt(int index) {
        if (index < 0 || index >= getOutputColumnCount()) {
            return null;
        }
        if (index < targetColumns.size()) {
            return targetColumns.get(index);
        }
        ColumnNode srcCol = sourceLineage.getChildren().get(index).getValue();
        if (srcCol == null) {
            return null;
        }
        if (srcCol.getAlias() != null && !srcCol.getAlias().isEmpty()) {
            return stripQuotes(srcCol.getAlias());
        }
        return extractNameFromExpression(srcCol.getExpression());
    }

    private static String extractNameFromExpression(String expression) {
        if (expression == null) {
            return null;
        }
        int dot = expression.lastIndexOf('.');
        String name = dot >= 0 ? expression.substring(dot + 1) : expression;
        name = stripQuotes(name.trim());
        if (name.isEmpty() || name.contains("(") || name.contains(" ")) {
            return null;
        }
        return name;
    }

    private static String stripQuotes(String raw) {
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

    @Override
    public String toString() {
        return "DmlLineageInfo{" + operation
                + " " + getQualifiedTargetTable()
                + (targetColumns.isEmpty() ? "" : " " + targetColumns)
                + (partitions.isEmpty() ? "" : " PARTITION " + partitions)
                + ", outputs=" + getOutputColumnCount()
                + '}';
    }
}
