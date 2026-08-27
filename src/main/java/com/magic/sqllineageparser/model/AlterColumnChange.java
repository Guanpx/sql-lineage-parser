package com.magic.sqllineageparser.model;

/**
 * ALTER TABLE 单列变更项
 * <p>
 * 描述一次列级别的结构变更（新增 / 删除 / 重命名 / 修改）
 *
 * @author Guan Peixiang
 * @since 2026/05/19
 */
public class AlterColumnChange {

    /**
     * 变更动作
     */
    public enum Action {
        ADD,
        DROP,
        RENAME,
        MODIFY
    }

    private Action action;
    private String columnName;
    private String newColumnName;
    private String dataType;
    private String comment;

    public AlterColumnChange() {
    }

    public static AlterColumnChange ofAdd(String columnName, String dataType, String comment) {
        AlterColumnChange change = new AlterColumnChange();
        change.action = Action.ADD;
        change.columnName = columnName;
        change.dataType = dataType;
        change.comment = comment;
        return change;
    }

    public static AlterColumnChange ofDrop(String columnName) {
        AlterColumnChange change = new AlterColumnChange();
        change.action = Action.DROP;
        change.columnName = columnName;
        return change;
    }

    public static AlterColumnChange ofRename(String oldName, String newName) {
        AlterColumnChange change = new AlterColumnChange();
        change.action = Action.RENAME;
        change.columnName = oldName;
        change.newColumnName = newName;
        return change;
    }

    public static AlterColumnChange ofModify(String columnName,
                                             String newColumnName,
                                             String dataType,
                                             String comment) {
        AlterColumnChange change = new AlterColumnChange();
        change.action = Action.MODIFY;
        change.columnName = columnName;
        change.newColumnName = newColumnName;
        change.dataType = dataType;
        change.comment = comment;
        return change;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getNewColumnName() {
        return newColumnName;
    }

    public void setNewColumnName(String newColumnName) {
        this.newColumnName = newColumnName;
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

    @Override
    public String toString() {
        return switch (action) {
            case ADD -> "ADD " + columnName + (dataType != null ? " " + dataType : "")
                    + (comment != null ? " COMMENT '" + comment + "'" : "");
            case DROP -> "DROP " + columnName;
            case RENAME -> "RENAME " + columnName + " TO " + newColumnName;
            case MODIFY -> "MODIFY " + columnName + (dataType != null ? " " + dataType : "");
        };
    }
}
