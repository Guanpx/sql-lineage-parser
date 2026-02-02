package com.magic.core.parser.sql.table;

import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.magic.sqllineageparser.model.TableNode;
import com.magic.sqllineageparser.model.TreeNode;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 表源解析器基础接口
 *
 * @author Guan Peixiang
 * @date 2023/9/12
 */
public interface BaseTableSourceParser {

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
