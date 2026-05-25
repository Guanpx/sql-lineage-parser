package com.magic.sqllineageparser.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ALTER TABLE 语句解析结果
 * <p>
 * 描述一次 ALTER TABLE 操作影响的表与列级变更集合
 *
 * @author Guan Peixiang
 * @since 2026/05/19
 */
public class AlterTableInfo {

    private String schema;
    private String tableName;
    private final List<AlterColumnChange> changes = new ArrayList<>();

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

    public List<AlterColumnChange> getChanges() {
        return Collections.unmodifiableList(changes);
    }

    public void addChange(AlterColumnChange change) {
        if (change != null) {
            changes.add(change);
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
        return "AlterTableInfo{" + getQualifiedTableName() + ", changes=" + changes + '}';
    }
}
