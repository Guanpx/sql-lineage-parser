package com.magic.core.parser.sql.table;

import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.magic.sqllineageparser.model.TableNode;
import com.magic.sqllineageparser.model.TreeNode;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 表源解析器基础接口 (密封接口)
 * <p>
 * 使用 Java 17 sealed interface 限制实现类，提供更好的类型安全
 *
 * @author Guan Peixiang
 * @since 2023/9/12
 */
public sealed interface BaseTableSourceParser
        permits SqlExprTableSourceParser,
        SqlJoinTableSourceParser,
        SqlSubqueryTableSourceParser,
        SqlUnionQueryTableSourceParser,
        SqlWithSubqueryTableSourceParser {

    /**
     * 处理表源解析
     *
     * @param dbType         数据库类型
     * @param sequence       序列号
     * @param parent         父节点
     * @param sqlTableSource SQL表源
     */
    void process(String dbType, AtomicInteger sequence, TreeNode<TableNode> parent, SQLTableSource sqlTableSource);
}
