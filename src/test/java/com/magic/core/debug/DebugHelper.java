package com.magic.core.debug;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.magic.sqllineageparser.model.ColumnNode;
import com.magic.sqllineageparser.model.TreeNode;

import java.util.List;

/**
 * 调试输出辅助类
 * 用于开发时打印解析结果
 *
 * @author Debug
 */
public final class DebugHelper {

    private static final String SEPARATOR = "=".repeat(60);
    private static final String SUB_SEPARATOR = "-".repeat(40);

    private DebugHelper() {
    }

    public static void printTitle(String title) {
        System.out.println("\n" + SEPARATOR);
        System.out.println("  " + title);
        System.out.println(SEPARATOR);
    }

    public static void printSubTitle(String subTitle) {
        System.out.println("\n" + SUB_SEPARATOR);
        System.out.println("  " + subTitle);
        System.out.println(SUB_SEPARATOR);
    }

    public static void printSql(String sql) {
        System.out.println("\n【SQL语句】:");
        System.out.println(sql);
    }

    public static void printExprInfo(String label, SQLExpr expr) {
        System.out.println("\n【" + label + "】");
        if (expr == null) {
            System.out.println("  (null)");
            return;
        }
        System.out.println("  类型: " + expr.getClass().getSimpleName());
        System.out.println("  内容: " + expr);
    }

    public static void printTableSourceInfo(SQLTableSource tableSource) {
        System.out.println("\n【表源信息】");
        if (tableSource == null) {
            System.out.println("  (null)");
            return;
        }
        System.out.println("  类型: " + tableSource.getClass().getSimpleName());
        System.out.println("  别名: " + tableSource.getAlias());
        System.out.println("  内容: " + tableSource);
    }

    public static void printSelectItems(List<SQLSelectItem> selectItems) {
        System.out.println("\n【SELECT列表】 共 " + selectItems.size() + " 项:");
        for (int i = 0; i < selectItems.size(); i++) {
            SQLSelectItem item = selectItems.get(i);
            System.out.println("  [" + (i + 1) + "] " + item);
            System.out.println("      表达式类型: " + item.getExpr().getClass().getSimpleName());
            System.out.println("      别名: " + item.getAlias());
        }
    }

    public static void printLineageTree(TreeNode<ColumnNode> root) {
        printLineageTree(root, 0);
    }

    private static void printLineageTree(TreeNode<ColumnNode> node, int depth) {
        if (node == null) return;

        String indent = "  ".repeat(depth);
        ColumnNode value = node.getValue();

        if (value != null) {
            System.out.println(indent + "├─ " + value.getName());
            System.out.println(indent + "│  别名: " + value.getAlias());
            System.out.println(indent + "│  表名: " + value.getTableName());
            System.out.println(indent + "│  常量: " + value.isConstant());

            // 打印来源列
            var sources = value.getSourceColumns();
            if (!sources.isEmpty()) {
                System.out.println(indent + "│  来源列 (" + sources.size() + "):");
                for (var src : sources) {
                    String srcInfo = src.isConstant()
                            ? "[常量] " + src.getName()
                            : (src.getTableName() != null
                                    ? src.getTableName() + "." + src.getName()
                                    : src.getName());
                    System.out.println(indent + "│    → " + srcInfo);
                }
            }
        } else {
            System.out.println(indent + "├─ [ROOT]");
        }

        if (node.getChildren() != null) {
            for (TreeNode<ColumnNode> child : node.getChildren()) {
                printLineageTree(child, depth + 1);
            }
        }
    }

    public static void printKeyValue(String key, Object value) {
        System.out.println("  " + key + ": " + value);
    }

    public static void printInfo(String info) {
        System.out.println("  → " + info);
    }

    public static void printError(String error) {
        System.out.println("  ✗ " + error);
    }

    public static void printSuccess(String msg) {
        System.out.println("  ✓ " + msg);
    }
}
