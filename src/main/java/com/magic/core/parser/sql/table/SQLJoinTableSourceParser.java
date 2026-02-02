package com.magic.core.parser.sql.table;

import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.magic.sqllineageparser.model.TableNode;
import com.magic.sqllineageparser.model.TreeNode;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * JOIN表源解析器
 * 处理 SQLJoinTableSource 类型的表源
 *
 * @author Guan Peixiang
 * @date 2023/9/12
 */
public class SQLJoinTableSourceParser implements BaseTableSourceParser {

    private static final SQLJoinTableSourceParser INSTANCE = new SQLJoinTableSourceParser();

    private SQLJoinTableSourceParser() {
    }

    public static SQLJoinTableSourceParser getInstance() {
        return INSTANCE;
    }

    @Override
    public void process(String dbType, AtomicInteger sequence, TreeNode<TableNode> parent, SQLTableSource sqlTableSource) {
        if (!(sqlTableSource instanceof SQLJoinTableSource joinTableSource)) {
            return;
        }

        // 获取JOIN类型
        SQLJoinTableSource.JoinType joinType = joinTableSource.getJoinType();
        System.out.println("JOIN类型: " + joinType);

        // 获取左表
        SQLTableSource left = joinTableSource.getLeft();
        System.out.println("左表: " + left);

        // 获取右表
        SQLTableSource right = joinTableSource.getRight();
        System.out.println("右表: " + right);

        // 获取JOIN条件
        var condition = joinTableSource.getCondition();
        System.out.println("JOIN条件: " + condition);

        // TODO: 递归处理左右表源
    }
}
