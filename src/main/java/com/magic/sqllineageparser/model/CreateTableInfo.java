package com.magic.sqllineageparser.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 纯 CREATE TABLE 语句解析结果
 * <p>
 * 该模型只描述表结构元信息；带 AS SELECT 的 CTAS 血缘仍使用 {@link DmlLineageInfo}。
 *
 * @author Guan Peixiang
 * @since 2026/08/26
 */
public class CreateTableInfo {

    private String schema;
    private String tableName;
    private String comment;
    private final List<TableColumnMeta> columns = new ArrayList<>();
    private final List<TableColumnMeta> partitionColumns = new ArrayList<>();

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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<TableColumnMeta> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    public void addColumn(TableColumnMeta column) {
        if (column != null) {
            columns.add(column);
        }
    }

    public List<TableColumnMeta> getPartitionColumns() {
        return Collections.unmodifiableList(partitionColumns);
    }

    public void addPartitionColumn(TableColumnMeta column) {
        if (column != null) {
            partitionColumns.add(column);
        }
    }

    /**
     * 全限定表名
     */
    public String getQualifiedTableName() {
        if (schema == null || schema.isEmpty()) {
            return tableName;
        }
        return schema + "." + tableName;
    }

    @Override
    public String toString() {
        return "CreateTableInfo{" + getQualifiedTableName()
                + ", columns=" + columns
                + ", partitionColumns=" + partitionColumns
                + (comment == null ? "" : ", comment='" + comment + "'")
                + '}';
    }
}
