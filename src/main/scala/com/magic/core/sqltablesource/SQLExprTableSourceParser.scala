package com.magic.core.sqltablesource
import com.alibaba.druid.sql.ast.statement.{SQLExprTableSource, SQLTableSource}
import com.magic.sqllineageparser.model.{TableNode, TreeNode}

import java.util.concurrent.atomic.AtomicInteger

object SQLExprTableSourceParser extends BaseTableSourceParser {
  override def process(dbType: String, sequence: AtomicInteger, parent: TreeNode[TableNode], sqlTableSource: SQLTableSource): Unit = {
    val sqlExprTableSourceExpr = sqlTableSource.asInstanceOf[SQLExprTableSource].getExpr




  }

}
