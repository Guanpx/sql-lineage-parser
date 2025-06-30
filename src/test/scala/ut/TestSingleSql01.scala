package ut

import com.alibaba.druid.DbType
import com.alibaba.druid.sql.SQLUtils
import com.alibaba.druid.sql.ast.SQLExpr
import com.magic.core.parser.SqlLineageParser.parserSingleSelectSql
import scala.io.Source


/**
 * sql01系列 简单解析
 */
object TestSingleSql01 {
  val filePath = "sqls/sqlCase/"

  def getSql(path: String): String = {
    // 从小到大 越复杂
    val source = Source.fromFile(path)
    val sqlFileContent = source.getLines().mkString("\n")
    source.close()
    sqlFileContent
  }

  def main(args: Array[String]): Unit = {
    val caseSql01 = s"sqls/sqlCase/sqlcase1.sql"
    val realSql01 = "sqls/sqlProd/sqlProd01.sql"
    val joinSql01 = "sqls/sqlJoin/sqlJoin01.sql"
    val joinSql02 = "sqls/sqlJoin/sqlJoin02.sql"
    val unionSql01 = "sqls/sqlUnion/sqlUnion01.sql"
    val caseSql02 = "sqls/sqlCase/sqlCase02.sql"
    val caseSql03 = "sqls/sqlCase/sqlCase03.sql"

    val sql = getSql(caseSql03)

    val expr: SQLExpr = SQLUtils.toSQLExpr(sql, DbType.hive)
    println(expr)
    parserSingleSelectSql(sql)
  }

}
