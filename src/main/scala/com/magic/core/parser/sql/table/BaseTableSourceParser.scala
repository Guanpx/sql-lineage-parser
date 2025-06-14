package com.magic.core.parser.sql.table

import com.alibaba.druid.sql.ast.statement.SQLTableSource
import com.magic.sqllineageparser.model.{TableNode, TreeNode}

import java.util.concurrent.atomic.AtomicInteger

/**
 * <p>
 *
 * @author Guan Peixiang (guanpeixiang@juzishuke.com)
 * @date 2023/9/12
 */
trait BaseTableSourceParser {
  def process(dbType: String, sequence: AtomicInteger, parent: TreeNode[TableNode], sqlTableSource: SQLTableSource): Unit

}