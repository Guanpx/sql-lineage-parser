package com.magic.core.debug;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.magic.core.util.SqlFileReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SqlExprParser 调试类
 * 用于开发时调试各种SQL表达式的解析结果
 *
 * @author Debug
 */
public class SqlExprParserDebug {

    public static void main(String[] args) {
//        debugSQLPropertyExpr();
//        debugSQLIdentifierExpr();
        debugSQLCaseExpr();
//        debugSQLAggregateExpr();
//        debugSQLMethodInvokeExpr();
//        debugSQLBinaryOpExpr();
//        debugSQLConstantExpr();
//        debugComplexExpr();
    }

    /**
     * 调试 SQLPropertyExpr (表.列 表达式)
     * 支持解析表别名，识别 a.col1 属于表 t1
     */
    public static void debugSQLPropertyExpr() {
        DebugHelper.printTitle("调试 SQLPropertyExpr (表.列表达式)");

        String[] sqls = {
            "SELECT t.id FROM users t",
            "SELECT db.users.id FROM db.users",
            "SELECT a.col1, b.col2 FROM t1 a, t2 b"
        };

        for (String sql : sqls) {
            DebugHelper.printSql(sql);

            SQLSelectQueryBlock queryBlock = getQueryBlock(sql);
            // 构建别名 -> 表名的映射
            Map<String, String> aliasToTableMap = buildAliasToTableMap(queryBlock.getFrom());

            DebugHelper.printSubTitle("表别名映射");
            if (aliasToTableMap.isEmpty()) {
                System.out.println("  (无别名)");
            } else {
                aliasToTableMap.forEach((alias, table) ->
                    System.out.println("  " + alias + " -> " + table));
            }

            List<SQLSelectItem> items = queryBlock.getSelectList();
            for (SQLSelectItem item : items) {
                SQLExpr expr = item.getExpr();
                if (expr instanceof SQLPropertyExpr propertyExpr) {
                    System.out.println("\n  【SQLPropertyExpr详情】");
                    DebugHelper.printKeyValue("完整表达式", propertyExpr.toString());
                    DebugHelper.printKeyValue("所有者(Owner)", propertyExpr.getOwner());
                    DebugHelper.printKeyValue("所有者名称", propertyExpr.getOwnerName());
                    DebugHelper.printKeyValue("列名(Name)", propertyExpr.getName());

                    // 解析真实表名
                    String ownerName = propertyExpr.getOwnerName();
                    String realTableName = aliasToTableMap.getOrDefault(ownerName, ownerName);
                    DebugHelper.printKeyValue("真实表名", realTableName);
                    DebugHelper.printKeyValue("列归属", realTableName + "." + propertyExpr.getName());
                }
            }
        }
    }

    /**
     * 从 FROM 子句构建别名 -> 表名的映射
     * 支持: 单表、逗号分隔多表、JOIN
     */
    private static Map<String, String> buildAliasToTableMap(SQLTableSource tableSource) {
        Map<String, String> map = new HashMap<>();
        collectTableAlias(tableSource, map);
        return map;
    }

    /**
     * 递归收集表别名
     */
    private static void collectTableAlias(SQLTableSource tableSource, Map<String, String> map) {
        if (tableSource == null) {
            return;
        }

        if (tableSource instanceof SQLExprTableSource exprTable) {
            // 普通表: t1 a 或 db.t1 a
            String tableName = exprTable.getExpr().toString();
            String alias = exprTable.getAlias();
            if (alias != null && !alias.isEmpty()) {
                map.put(alias, tableName);
            } else {
                // 无别名时，表名本身也可作为引用
                map.put(tableName, tableName);
            }
        } else if (tableSource instanceof SQLJoinTableSource joinTable) {
            // JOIN 表: 递归处理左右两侧
            // 注意: FROM t1 a, t2 b 会被解析为 SQLJoinTableSource(COMMA)
            collectTableAlias(joinTable.getLeft(), map);
            collectTableAlias(joinTable.getRight(), map);
        } else if (tableSource instanceof SQLSubqueryTableSource subquery) {
            // 子查询: (SELECT ...) AS sub
            String alias = subquery.getAlias();
            if (alias != null && !alias.isEmpty()) {
                map.put(alias, "(subquery)");
            }
        }
        // 其他类型如 UNION 表源暂不处理
    }

    /**
     * 调试 SQLIdentifierExpr (列标识符)
     */
    public static void debugSQLIdentifierExpr() {
        DebugHelper.printTitle("调试 SQLIdentifierExpr (列标识符)");

        String sql = "SELECT id, name, age FROM users";
        DebugHelper.printSql(sql);

        List<SQLSelectItem> items = getSelectItems(sql);
        for (SQLSelectItem item : items) {
            SQLExpr expr = item.getExpr();
            if (expr instanceof SQLIdentifierExpr identifierExpr) {
                System.out.println("\n  【SQLIdentifierExpr详情】");
                DebugHelper.printKeyValue("名称", identifierExpr.getName());
                DebugHelper.printKeyValue("小写名称", identifierExpr.getLowerName());
            }
        }
    }

    /**
     * 调试 SQLCaseExpr (CASE WHEN表达式)
     */
    public static void debugSQLCaseExpr() {
        DebugHelper.printTitle("调试 SQLCaseExpr (CASE WHEN表达式)");

        // 从文件读取
        String sql = SqlFileReader.readCaseSql("sqlcase1.sql");
        DebugHelper.printSql(sql);

        List<SQLSelectItem> items = getSelectItems(sql);
        for (SQLSelectItem item : items) {
            SQLExpr expr = item.getExpr();
            if (expr instanceof SQLCaseExpr caseExpr) {
                System.out.println("\n  【SQLCaseExpr详情】");
                DebugHelper.printKeyValue("条件数量", caseExpr.getItems().size());

                System.out.println("  各条件分支:");
                for (int i = 0; i < caseExpr.getItems().size(); i++) {
                    SQLCaseExpr.Item caseItem = caseExpr.getItems().get(i);
                    System.out.println("    [" + (i + 1) + "] WHEN: " + caseItem.getConditionExpr());
                    System.out.println("        THEN: " + caseItem.getValueExpr());
                    System.out.println("        THEN类型: " + caseItem.getValueExpr().getClass().getSimpleName());
                }

                System.out.println("  ELSE分支:");
                if (caseExpr.getElseExpr() != null) {
                    System.out.println("    值: " + caseExpr.getElseExpr());
                    System.out.println("    类型: " + caseExpr.getElseExpr().getClass().getSimpleName());
                } else {
                    System.out.println("    (无ELSE)");
                }
            }
        }
    }

    /**
     * 调试 SQLAggregateExpr (聚合函数)
     */
    public static void debugSQLAggregateExpr() {
        DebugHelper.printTitle("调试 SQLAggregateExpr (聚合函数)");

        String[] sqls = {
            "SELECT COUNT(*) FROM users",
            "SELECT SUM(amount), AVG(price) FROM orders",
            "SELECT MAX(score), MIN(score) FROM students",
            "SELECT COUNT(DISTINCT id) FROM users"
        };

        for (String sql : sqls) {
            DebugHelper.printSubTitle("SQL");
            DebugHelper.printSql(sql);

            List<SQLSelectItem> items = getSelectItems(sql);
            for (SQLSelectItem item : items) {
                SQLExpr expr = item.getExpr();
                if (expr instanceof SQLAggregateExpr aggregateExpr) {
                    System.out.println("\n  【SQLAggregateExpr详情】");
                    DebugHelper.printKeyValue("方法名", aggregateExpr.getMethodName());
                    DebugHelper.printKeyValue("参数数量", aggregateExpr.getArguments().size());
                    DebugHelper.printKeyValue("参数列表", aggregateExpr.getArguments());
                    DebugHelper.printKeyValue("DISTINCT", aggregateExpr.getOption());
                    DebugHelper.printKeyValue("数据类型", aggregateExpr.computeDataType());
                }
            }
        }
    }

    /**
     * 调试 SQLMethodInvokeExpr (方法调用)
     */
    public static void debugSQLMethodInvokeExpr() {
        DebugHelper.printTitle("调试 SQLMethodInvokeExpr (方法调用)");

        // IF函数
        DebugHelper.printSubTitle("IF函数");
        String sql1 = SqlFileReader.readFunctionSql("sqlMaxIfNvlFunc01.sql");
        DebugHelper.printSql(sql1);
        debugMethodInvokeExpr(sql1);

        // NVL函数
        DebugHelper.printSubTitle("NVL函数");
        String sql2 = SqlFileReader.readFunctionSql("sqlMaxIfNvlFunc03.sql");
        DebugHelper.printSql(sql2);
        debugMethodInvokeExpr(sql2);

        // 嵌套函数
        DebugHelper.printSubTitle("嵌套函数");
        String sql3 = "SELECT CONCAT(UPPER(name), '_', LOWER(code)) FROM t";
        DebugHelper.printSql(sql3);
        debugMethodInvokeExpr(sql3);
    }

    private static void debugMethodInvokeExpr(String sql) {
        List<SQLSelectItem> items = getSelectItems(sql);
        for (SQLSelectItem item : items) {
            SQLExpr expr = item.getExpr();
            printExprTree(expr, 0);
        }
    }

    private static void printExprTree(SQLExpr expr, int depth) {
        String indent = "  ".repeat(depth);
        if (expr instanceof SQLMethodInvokeExpr methodExpr) {
            System.out.println(indent + "【方法调用】" + methodExpr.getMethodName());
            System.out.println(indent + "  参数数量: " + methodExpr.getArguments().size());
            for (int i = 0; i < methodExpr.getArguments().size(); i++) {
                System.out.println(indent + "  参数[" + i + "]:");
                printExprTree(methodExpr.getArguments().get(i), depth + 2);
            }
        } else if (expr instanceof SQLAggregateExpr aggregateExpr) {
            System.out.println(indent + "【聚合函数】" + aggregateExpr.getMethodName());
            for (int i = 0; i < aggregateExpr.getArguments().size(); i++) {
                System.out.println(indent + "  参数[" + i + "]:");
                printExprTree(aggregateExpr.getArguments().get(i), depth + 2);
            }
        } else {
            System.out.println(indent + "【" + expr.getClass().getSimpleName() + "】" + expr);
        }
    }

    /**
     * 调试 SQLBinaryOpExpr (二元运算表达式)
     */
    public static void debugSQLBinaryOpExpr() {
        DebugHelper.printTitle("调试 SQLBinaryOpExpr (二元运算表达式)");

        String[] sqls = {
            "SELECT a + b FROM t",
            "SELECT a - b * c FROM t",
            "SELECT a > b FROM t",
            "SELECT a = 1 AND b = 2 FROM t",
            "SELECT a LIKE '%test%' FROM t"
        };

        for (String sql : sqls) {
            DebugHelper.printSql(sql);
            List<SQLSelectItem> items = getSelectItems(sql);
            for (SQLSelectItem item : items) {
                SQLExpr expr = item.getExpr();
                if (expr instanceof SQLBinaryOpExpr binaryExpr) {
                    System.out.println("\n  【SQLBinaryOpExpr详情】");
                    DebugHelper.printKeyValue("操作符", binaryExpr.getOperator());
                    DebugHelper.printKeyValue("左操作数", binaryExpr.getLeft());
                    DebugHelper.printKeyValue("左操作数类型", binaryExpr.getLeft().getClass().getSimpleName());
                    DebugHelper.printKeyValue("右操作数", binaryExpr.getRight());
                    DebugHelper.printKeyValue("右操作数类型", binaryExpr.getRight().getClass().getSimpleName());
                }
            }
        }
    }

    /**
     * 调试常量表达式
     */
    public static void debugSQLConstantExpr() {
        DebugHelper.printTitle("调试常量表达式");

        String sql = "SELECT 123, 3.14, 'hello', NULL, TRUE FROM dual";
        DebugHelper.printSql(sql);

        List<SQLSelectItem> items = getSelectItems(sql);
        for (int i = 0; i < items.size(); i++) {
            SQLExpr expr = items.get(i).getExpr();
            System.out.println("\n  【常量 " + (i + 1) + "】");
            DebugHelper.printKeyValue("类型", expr.getClass().getSimpleName());
            DebugHelper.printKeyValue("值", expr);

            if (expr instanceof SQLIntegerExpr intExpr) {
                DebugHelper.printKeyValue("数值", intExpr.getNumber());
            } else if (expr instanceof SQLNumberExpr numExpr) {
                DebugHelper.printKeyValue("数值", numExpr.getNumber());
            } else if (expr instanceof SQLCharExpr charExpr) {
                DebugHelper.printKeyValue("文本", charExpr.getText());
            }
        }
    }

    /**
     * 调试复杂表达式组合
     */
    public static void debugComplexExpr() {
        DebugHelper.printTitle("调试复杂表达式组合");

        // 从生产SQL读取
        String sql = SqlFileReader.readProdSql("sqlProd01.sql");
        DebugHelper.printSql(sql);

        List<SQLSelectItem> items = getSelectItems(sql);
        DebugHelper.printSubTitle("SELECT项统计");
        System.out.println("  总列数: " + items.size());

        // 统计各类型表达式
        int caseCount = 0, aggCount = 0, methodCount = 0, propertyCount = 0, otherCount = 0;

        for (SQLSelectItem item : items) {
            SQLExpr expr = item.getExpr();
            if (expr instanceof SQLCaseExpr) caseCount++;
            else if (expr instanceof SQLAggregateExpr) aggCount++;
            else if (expr instanceof SQLMethodInvokeExpr) methodCount++;
            else if (expr instanceof SQLPropertyExpr) propertyCount++;
            else otherCount++;
        }

        DebugHelper.printSubTitle("表达式类型分布");
        DebugHelper.printKeyValue("CASE表达式", caseCount);
        DebugHelper.printKeyValue("聚合函数", aggCount);
        DebugHelper.printKeyValue("方法调用", methodCount);
        DebugHelper.printKeyValue("属性表达式", propertyCount);
        DebugHelper.printKeyValue("其他类型", otherCount);

        // 打印前5个表达式详情
        DebugHelper.printSubTitle("前5个表达式详情");
        for (int i = 0; i < Math.min(5, items.size()); i++) {
            SQLSelectItem item = items.get(i);
            System.out.println("\n  [" + (i + 1) + "] " + item);
            System.out.println("      类型: " + item.getExpr().getClass().getSimpleName());
            System.out.println("      别名: " + item.getAlias());
        }
    }

    // ==================== 辅助方法 ====================

    private static SQLSelectQueryBlock getQueryBlock(String sql) {
        SQLSelectStatement stmt = (SQLSelectStatement) SQLUtils.parseSingleStatement(sql, DbType.hive);
        return (SQLSelectQueryBlock) stmt.getSelect().getQuery();
    }

    private static List<SQLSelectItem> getSelectItems(String sql) {
        return getQueryBlock(sql).getSelectList();
    }

    /**
     * 调试自定义SQL表达式
     */
    public static void debugCustomExpr(String sql) {
        DebugHelper.printTitle("调试自定义SQL表达式");
        DebugHelper.printSql(sql);

        List<SQLSelectItem> items = getSelectItems(sql);
        DebugHelper.printSelectItems(items);

        for (SQLSelectItem item : items) {
            System.out.println("\n【表达式详情】");
            printExprTree(item.getExpr(), 0);
        }
    }
}
