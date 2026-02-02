package com.magic.core.parser.sql.table;

import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUnionQueryTableSource;
import com.magic.sqllineageparser.model.TableNode;
import com.magic.sqllineageparser.model.TreeNode;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Union查询表源解析器
 * 处理 SQLUnionQueryTableSource 类型的表源
 *
 * @author Guan Peixiang
 * @date 2023/9/12
 */
public class SQLUnionQueryTableSourceParser implements BaseTableSourceParser {

    private static final SQLUnionQueryTableSourceParser INSTANCE = new SQLUnionQueryTableSourceParser();

    private SQLUnionQueryTableSourceParser() {
    }

    public static SQLUnionQueryTableSourceParser getInstance() {
        return INSTANCE;
    }

    @Override
    public void process(String dbType, AtomicInteger sequence, TreeNode<TableNode> parent, SQLTableSource sqlTableSource) {
        if (!(sqlTableSource instanceof SQLUnionQueryTableSource unionQueryTableSource)) {
            return;
        }

        // 获取Union查询
        var unionQuery = unionQueryTableSource.getUnion();
        System.out.println("Union查询: " + unionQuery);

        // TODO: 实现Union查询的解析
    }
}
