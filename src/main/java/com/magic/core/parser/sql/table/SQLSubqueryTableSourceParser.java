package com.magic.core.parser.sql.table;

import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.magic.sqllineageparser.model.TableNode;
import com.magic.sqllineageparser.model.TreeNode;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 子查询表源解析器
 * 处理 SQLSubqueryTableSource 类型的表源
 *
 * @author Guan Peixiang
 * @date 2023/9/12
 */
public class SQLSubqueryTableSourceParser implements BaseTableSourceParser {

    private static final SQLSubqueryTableSourceParser INSTANCE = new SQLSubqueryTableSourceParser();

    private SQLSubqueryTableSourceParser() {
    }

    public static SQLSubqueryTableSourceParser getInstance() {
        return INSTANCE;
    }

    @Override
    public void process(String dbType, AtomicInteger sequence, TreeNode<TableNode> parent, SQLTableSource sqlTableSource) {
        if (!(sqlTableSource instanceof SQLSubqueryTableSource subqueryTableSource)) {
            return;
        }

        // 获取子查询
        var select = subqueryTableSource.getSelect();
        System.out.println("子查询: " + select);

        // 获取别名
        String alias = subqueryTableSource.getAlias();
        System.out.println("子查询别名: " + alias);

        // TODO: 实现子查询的递归解析
    }
}
