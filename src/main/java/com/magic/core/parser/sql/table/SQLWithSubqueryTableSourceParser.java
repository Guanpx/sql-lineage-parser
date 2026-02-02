package com.magic.core.parser.sql.table;

import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLWithSubqueryClause;
import com.magic.sqllineageparser.model.TableNode;
import com.magic.sqllineageparser.model.TreeNode;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * WITH子查询表源解析器
 * 处理 SQLWithSubqueryClause 类型的表源
 *
 * @author Guan Peixiang
 * @date 2023/12/19
 */
public class SQLWithSubqueryTableSourceParser implements BaseTableSourceParser {

    private static final SQLWithSubqueryTableSourceParser INSTANCE = new SQLWithSubqueryTableSourceParser();

    private SQLWithSubqueryTableSourceParser() {
    }

    public static SQLWithSubqueryTableSourceParser getInstance() {
        return INSTANCE;
    }

    @Override
    public void process(String dbType, AtomicInteger sequence, TreeNode<TableNode> parent, SQLTableSource sqlTableSource) {
        // TODO: 实现WITH子查询的解析
        System.out.println("处理WITH子查询表源");
    }
}
