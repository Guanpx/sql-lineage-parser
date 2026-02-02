package com.magic.core.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SQL文件读取工具类
 *
 * @author Test
 */
public final class SqlFileReader {

    private static final String SQL_BASE_PATH = "sqls";

    private SqlFileReader() {
    }

    /**
     * 读取SQL文件内容
     *
     * @param relativePath 相对于sqls目录的路径，如 "sqlCase/sqlcase1.sql"
     * @return SQL文件内容
     */
    public static String readSqlFile(String relativePath) {
        try {
            Path path = Paths.get(SQL_BASE_PATH, relativePath);
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read SQL file: " + relativePath, e);
        }
    }

    /**
     * 读取CASE相关的SQL文件
     *
     * @param fileName 文件名，如 "sqlcase1.sql"
     * @return SQL内容
     */
    public static String readCaseSql(String fileName) {
        return readSqlFile("sqlCase/" + fileName);
    }

    /**
     * 读取JOIN相关的SQL文件
     *
     * @param fileName 文件名
     * @return SQL内容
     */
    public static String readJoinSql(String fileName) {
        return readSqlFile("sqlJoin/" + fileName);
    }

    /**
     * 读取FUNCTION相关的SQL文件
     *
     * @param fileName 文件名
     * @return SQL内容
     */
    public static String readFunctionSql(String fileName) {
        return readSqlFile("sqlFunction/" + fileName);
    }

    /**
     * 读取UNION相关的SQL文件
     *
     * @param fileName 文件名
     * @return SQL内容
     */
    public static String readUnionSql(String fileName) {
        return readSqlFile("sqlUnion/" + fileName);
    }

    /**
     * 读取生产级复杂SQL文件
     *
     * @param fileName 文件名
     * @return SQL内容
     */
    public static String readProdSql(String fileName) {
        return readSqlFile("sqlProd/" + fileName);
    }

    /**
     * 读取ALTER语句SQL文件
     *
     * @param fileName 文件名
     * @return SQL内容
     */
    public static String readAlterSql(String fileName) {
        return readSqlFile("sqlAlter/" + fileName);
    }
}
