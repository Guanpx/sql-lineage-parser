package com.magic.sqllineageparser.model;

import lombok.Getter;
import lombok.Setter;

/**
 * CREATE TABLE 语句中的字段元信息
 * <p>
 * 此元数据用于数据地图展示，也同样在图谱中保存孤立节点
 *
 * @author Guan Peixiang
 * @since 2026/08/26
 */
@Setter
@Getter
public class TableColumnMeta {

    private String columnName;
    private String dataType;
    private String comment;
    private String defaultValue;
    private boolean primaryKey;

    public TableColumnMeta() {
    }

    public static TableColumnMeta of(String columnName,
                                     String dataType,
                                     String comment,
                                     String defaultValue,
                                     boolean primaryKey) {
        TableColumnMeta meta = new TableColumnMeta();
        meta.columnName = columnName;
        meta.dataType = dataType;
        meta.comment = comment;
        meta.defaultValue = defaultValue;
        meta.primaryKey = primaryKey;
        return meta;
    }

    @Override
    public String toString() {
        return columnName
                + (dataType == null ? "" : " " + dataType)
                + (primaryKey ? " PRIMARY KEY" : "")
                + (defaultValue == null ? "" : " DEFAULT " + defaultValue)
                + (comment == null ? "" : " COMMENT '" + comment + "'");
    }
}
