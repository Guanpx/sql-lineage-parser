package com.magic.core.parser.sql.table;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.magic.sqllineageparser.model.TableNode;
import com.magic.sqllineageparser.model.TreeNode;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 普通表源解析器
 * 处理 SQLExprTableSource 类型的表源
 *
 * @author Guan Peixiang
 * @date 2023/9/12
 */
public class SQLExprTableSourceParser implements BaseTableSourceParser {

    private static final SQLExprTableSourceParser INSTANCE = new SQLExprTableSourceParser();

    private SQLExprTableSourceParser() {
    }

    public static SQLExprTableSourceParser getInstance() {
        return INSTANCE;
    }

    @Override
    public void process(String dbType, AtomicInteger sequence, TreeNode<TableNode> parent, SQLTableSource sqlTableSource) {
        if (!(sqlTableSource instanceof SQLExprTableSource exprTableSource)) {
            return;
        }
        SQLExpr expr = exprTableSource.getExpr();
        // TODO: 实现具体的解析逻辑
    }
}
