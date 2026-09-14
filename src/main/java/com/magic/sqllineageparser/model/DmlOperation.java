package com.magic.sqllineageparser.model;

/**
 * DML 操作类型
 *
 * @author Guan Peixiang
 * @since 2023/12/22
 */
public enum DmlOperation {

    /**
     * INSERT INTO ... SELECT
     */
    INSERT_INTO,

    /**
     * INSERT OVERWRITE [TABLE] ... SELECT
     */
    INSERT_OVERWRITE,

    /**
     * CREATE TABLE ... AS SELECT
     */
    CTAS,

    /**
     * CREATE VIEW ... AS SELECT
     */
    CREATE_VIEW
}
