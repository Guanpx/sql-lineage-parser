package com.magic.core.parser.sql.table

import com.alibaba.druid.sql.ast.statement.SQLTableSource
import com.magic.sqllineageparser.model.{TableNode, TreeNode}

import java.util.concurrent.atomic.AtomicInteger

/**
 * Union类型的table source
 *
 * @author Guan Peixiang (guanpeixiang@foxmail.com)
 * @date 2023/9/12
 */
object SQLUnionQueryTableSourceParser extends BaseTableSourceParser {
  override def process(dbType: String, sequence: AtomicInteger, parent: TreeNode[TableNode], sqlTableSource: SQLTableSource): Unit = {

  }
}