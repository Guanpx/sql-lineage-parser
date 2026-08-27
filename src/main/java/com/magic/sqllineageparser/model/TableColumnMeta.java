package com.magic.sqllineageparser.model;

/**
 * CREATE TABLE 语句中的字段元信息
 *
 * @author Guan Peixiang
 * @since 2026/08/26
 */
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

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
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
