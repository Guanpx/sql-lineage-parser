package com.magic.core.sqltablesource

import com.alibaba.druid.sql.ast.statement.SQLTableSource
import com.magic.sqllineageparser.model.{TableNode, TreeNode}

import java.util.concurrent.atomic.AtomicInteger

/**
 * <p>
 *
 * @author Guan Peixiang
 * @date 2023/9/12
 */
trait BaseTableSourceParser {
  def process(dbType: String, sequence: AtomicInteger, parent: TreeNode[TableNode], sqlTableSource: SQLTableSource): Unit

}