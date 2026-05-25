package com.magic.sqllineageparser.model;

/**
 * DML 操作类型
 *
 * @author Guan Peixiang
 * @since 2026/05/20
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
    CTAS
}
