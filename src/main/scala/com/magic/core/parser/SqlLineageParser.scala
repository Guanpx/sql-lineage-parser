package com.magic.core.parser

import com.alibaba.druid.DbType
import com.alibaba.druid.sql.SQLUtils
import com.alibaba.druid.sql.ast.SQLExpr
import com.alibaba.druid.sql.ast.expr._
import com.alibaba.druid.sql.ast.statement._
import com.magic.core.utils.StringUtils

import java.util
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.mutable

/**
 * sql血缘解析入口函数
 *
 * @author Guan Peixiang (guanpeixiang@juzishuke.com)
 * @date 2023/12/19
 */
object SqlLineageParser {

  /**
   * 解析单个select sql
   * 包括
   * 1.select 类型
   * 2.union  类型 todo
   * @param sql
   */
  def parserSingleSelectSql(sql: String): Unit = {
    if (StringUtils.isEmpty(sql)) {
      return
    }

    val stmt: SQLSelectStatement = SQLUtils.parseSingleStatement(sql, DbType.mysql).asInstanceOf[SQLSelectStatement]

    val sqlSelectQuery: SQLSelectQuery = stmt.getSelect.getQuery

    // select语句
    sqlSelectQuery match {
      case sqlSelectQuery: SQLSelectQueryBlock => {
        parserSelectStmt(sqlSelectQuery)
      }
      // union
      case sqlSelectQuery: SQLUnionQuery => {
        println(s"${sqlSelectQuery.getClass}类型不支持!!!")
      }
      case _ => println(s"${sqlSelectQuery.getClass}类型不支持!!!")
    }
  }

  /**
   * 解析select语句 非union
   * @param sqlSelectQueryBlock
   */
  private def parserSelectStmt(sqlSelectQueryBlock: SQLSelectQueryBlock): Unit = {
    println("解析 select sqlSelectQueryBlock")

    val selectList: util.List[SQLSelectItem] = sqlSelectQueryBlock.getSelectList
    println(selectList.size())
    for (item <- selectList.asScala) {
      val expr: SQLExpr = item.getExpr
      val exprString = item.getExpr.toString
      val hint = item.getExpr.getHint
      val alias: String = item.getAlias
      val itemName: String = item.toString

      val getColumn = if (StringUtils.isEmpty(alias)) itemName else alias
      parserSqlExpr(expr)

    }

    val table: SQLTableSource = sqlSelectQueryBlock.getFrom


    // 普通单表
    table match {
      case tableSource: SQLExprTableSource =>
      println("普通表：SQLExprTableSource")
      // 处理最终表---------------------
      //handlerSQLExprTableSource(node, table.asInstanceOf[SQLExprTableSource])
      case tableSource: SQLJoinTableSource => // 处理join
        println("join表：SQLJoinTableSource")
        handlerSQLJoinTableSource(tableSource)
      case tableSource: SQLSubqueryTableSource => // 处理 subquery ---------------------
        println("子查询表：SQLSubqueryTableSource")
      //handlerSQLSubqueryTableSource(node, table, `type`)
      case tableSource: SQLUnionQueryTableSource => // 处理 union ---------------------
        println("union表：SQLUnionQueryTableSource")
      //handlerSQLUnionQueryTableSource(node, table.asInstanceOf[SQLUnionQueryTableSource], `type`)
      case _ =>
    }


  }

  /**
   * 解析子节点sql
   *
   * @param sqlExpr
   */
  private def parserSqlExpr(sqlExpr: SQLExpr): Unit = {
    sqlExpr match {
      // 聚合
      case expr: SQLAggregateExpr =>
        parserSQLAggregateExpr(expr);

      // 方法
      case expr: SQLMethodInvokeExpr =>
        parserSqlMethodInvokeExpr(expr);

      // case when
      case expr: SQLCaseExpr =>
        parserSQLCaseExpr(expr);

      // 比较
      case expr: SQLBinaryOpExpr =>
        parserSQLBinaryOpExpr(expr);

      // 表达式
      case expr: SQLPropertyExpr =>
        parserSQLPropertyExpr(expr);

      // 列
      case expr: SQLIdentifierExpr =>
        parserSqlIdentifierExpr(expr);

      // 数字
      case expr: SQLNumberExpr =>
        parserSQLNumberExpr(expr);

      // 赋值表达式
      case expr: SQLIntegerExpr =>
        parserSqlIntegerExpr(expr);

      // 字符
      case expr: SQLCharExpr =>
        parseSqlCharExpr(expr);

      // 其他未列出类型
      case _ =>
        println("！！！暂不支持未列出类型！！！")
    }
  }



  /**
   * 表达式
   *
   * ？？？
   * @param expr
   */
  private def parserSQLPropertyExpr(expr: SQLPropertyExpr): Unit = {
    println("表达式 propertyExpr")
    val name = expr.toString
    println(name)
    println(expr.getOwner)
    println(expr.getName)
    println("表达式 propertyExpr BinaryOp 解析完成！！！")
  }


  /**
   * SQL CASE WHEN
   *
   * select case when then else end
   * @param expr
   */
  private def parserSQLCaseExpr(expr: SQLCaseExpr): Unit = {
    println("解析case when ....")
    // parserSqlExpr(expr.getValueExpr)
    val x: SQLExpr = expr.getValueExpr // todo
    println(x)
    val lst: mutable.Buffer[SQLCaseExpr.Item] = expr.getItems.asScala
    for (elem <- lst) {
      println(s"item: $elem")
      println(elem.getConditionExpr)
      parserSqlExpr(elem.getValueExpr)
    }
    println("解析case end !\n\n\n")
  }

  /**
   * SQL Method
   *
   * select max(xxx) from table
   * @param expr
   */
  private def parserSqlMethodInvokeExpr(expr: SQLMethodInvokeExpr): Unit = {
    println("方法 visitSQLMethodInvoke")
    val name = expr.getMethodName
    expr.getArguments.asScala.foreach(argsExpr => {
      parserSqlExpr(argsExpr)
    })
    println(name)
    println("方法 visitSQLMethodInvoke 解析完成！！！")
  }

  /**
   * todo
   *
   *
   * @param expr
   */
  private def parserSqlIdentifierExpr(expr: SQLIdentifierExpr): Unit = {
    println("列 identifierExpr")
    val name = expr.getName
    println(name)
    println("列 identifierExpr 解析完成！！！")


  }

  /**
   * 整数 sql
   *
   * select 1 as col
   * @param expr
   */
  private def parserSqlIntegerExpr(expr: SQLIntegerExpr): Unit = {
    println("整数常量")
    val name = expr.getNumber.toString
    println(name)
    println("整数常量 解析完成！！！")
  }

  /**
   * 数字
   * @param expr
   */
  private def parserSQLNumberExpr(expr: SQLNumberExpr): Unit = {
    println(" 数字 numberExpr")
    val name = expr.toString
    println(name)
    println("数字 numberExpr 解析完成！！！")
  }

  /**
   * 字符 sql
   *
   * select '1' as col
   * @param expr
   */
  private def parseSqlCharExpr(expr: SQLCharExpr): Unit = {
    println("字符常量")
    val name = expr.toString
    println(name)
    println("字符常量 解析完成！！！")
  }


  /**
   * 比较类 sql
   *
   * select a>b
   * @param expr
   */
  private def parserSQLBinaryOpExpr(expr: SQLBinaryOpExpr): Unit = {
    println("比较 BinaryOp")
    val name = expr.toString
    println(name)
    println("比较 BinaryOp 解析完成！！！")
  }



  /**
   * 聚合函数
   *
   * @param expr
   */
  private def parserSQLAggregateExpr(expr: SQLAggregateExpr): Unit = {
    println("聚合aggregate")
    val name = expr.toString
    println(name)
    println("聚合aggregate 解析完成！！！")
  }


  /**
   * sql join table source
   * @param tableSource
   */
  private def handlerSQLJoinTableSource(tableSource: SQLJoinTableSource): Unit ={
    println(tableSource.getJoinType)

    println(tableSource)
    println(tableSource.getFlashback)
    println(tableSource.getAlias2)
    println(tableSource.getLeft)
    println(tableSource.getRight)
    println(tableSource.getCondition)



  }


}