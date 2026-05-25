package com.magic.core.parser;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.magic.core.parser.sql.alter.SqlAlterTableParser;
import com.magic.core.parser.sql.dml.SqlCreateTableAsParser;
import com.magic.core.parser.sql.dml.SqlInsertParser;
import com.magic.core.parser.sql.expr.BaseSqlExprParser;
import com.magic.core.parser.sql.expr.ExprParseContext;
import com.magic.core.utils.StringUtils;
import com.magic.sqllineageparser.model.AlterTableInfo;
import com.magic.sqllineageparser.model.ColumnNode;
import com.magic.sqllineageparser.model.DmlLineageInfo;
import com.magic.sqllineageparser.model.TreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQL血缘解析入口
 *
 * @author Guan Peixiang
 * @since 2023/12/19
 */
public final class SqlLineageParser {

    private static final Logger LOGGER = Logger.getLogger(SqlLineageParser.class.getName());

    private SqlLineageParser() {
    }

    /**
     * 解析单个 SELECT SQL 语句
     * <p>支持: SELECT 查询块、UNION/INTERSECT/EXCEPT、WITH (CTE)
     *
     * @param sql SQL语句
     * @return 血缘树根节点，解析失败返回 null
     */
    public static TreeNode<ColumnNode> parserSingleSelectSql(String sql) {
        if (StringUtils.isEmpty(sql)) {
            return null;
        }

        SQLStatement stmt = SQLUtils.parseSingleStatement(sql, DbType.hive);
        if (!(stmt instanceof SQLSelectStatement selectStmt)) {
            LOGGER.warning(() -> "非 SELECT 语句: " + stmt.getClass().getSimpleName());
            return null;
        }

        return parseSelect(selectStmt.getSelect());
    }

    /**
     * 解析 ALTER TABLE 语句
     *
     * @param sql ALTER 语句文本
     * @return 表结构变更信息，非 ALTER 或空 SQL 返回 null
     */
    public static AlterTableInfo parserAlterTableSql(String sql) {
        if (StringUtils.isEmpty(sql)) {
            return null;
        }
        SQLStatement stmt = SQLUtils.parseSingleStatement(sql, DbType.hive);
        if (stmt instanceof SQLAlterTableStatement alter) {
            return SqlAlterTableParser.parse(alter);
        }
        LOGGER.warning(() -> "非 ALTER TABLE 语句: " + stmt.getClass().getSimpleName());
        return null;
    }

    /**
     * 解析 INSERT INTO / INSERT OVERWRITE ... SELECT 语句
     *
     * @param sql INSERT 语句文本
     * @return DML 血缘信息；非 INSERT 或空 SQL 返回 null
     */
    public static DmlLineageInfo parserInsertSql(String sql) {
        if (StringUtils.isEmpty(sql)) {
            return null;
        }
        SQLStatement stmt = SQLUtils.parseSingleStatement(sql, DbType.hive);
        if (stmt instanceof SQLInsertStatement insert) {
            return SqlInsertParser.parse(insert);
        }
        LOGGER.warning(() -> "非 INSERT 语句: " + stmt.getClass().getSimpleName());
        return null;
    }

    /**
     * 解析 CREATE TABLE ... AS SELECT 语句
     *
     * @param sql CREATE TABLE AS SELECT 文本
     * @return DML 血缘信息；非 CTAS 或空 SQL 返回 null
     */
    public static DmlLineageInfo parserCreateTableSql(String sql) {
        if (StringUtils.isEmpty(sql)) {
            return null;
        }
        SQLStatement stmt = SQLUtils.parseSingleStatement(sql, DbType.hive);
        if (stmt instanceof SQLCreateTableStatement create) {
            return SqlCreateTableAsParser.parse(create);
        }
        LOGGER.warning(() -> "非 CREATE TABLE 语句: " + stmt.getClass().getSimpleName());
        return null;
    }

    /**
     * 统一 DML 入口：自动识别 INSERT / CTAS 并解析。
     *
     * @param sql 待解析 SQL 文本
     * @return DML 血缘信息；不识别返回 null
     */
    public static DmlLineageInfo parserDmlSql(String sql) {
        if (StringUtils.isEmpty(sql)) {
            return null;
        }
        SQLStatement stmt = SQLUtils.parseSingleStatement(sql, DbType.hive);
        if (stmt instanceof SQLInsertStatement insert) {
            return SqlInsertParser.parse(insert);
        }
        if (stmt instanceof SQLCreateTableStatement create) {
            return SqlCreateTableAsParser.parse(create);
        }
        LOGGER.warning(() -> "非 DML 语句（INSERT/CTAS）: " + stmt.getClass().getSimpleName());
        return null;
    }

    /**
     * 解析 SQLSelect（含可能的 WITH 子句）
     * <p>
     * 公开以便 DML / DDL 解析器复用 SELECT 部分的血缘解析
     *
     * @param select Druid AST 中的 SQLSelect
     * @return 血缘树根节点，children 为输出列
     */
    public static TreeNode<ColumnNode> parseSelect(SQLSelect select) {
        if (select == null) {
            return null;
        }
        Map<String, Map<String, List<ColumnNode>>> cteSources = collectCteSources(select.getWithSubQuery());

        var query = select.getQuery();
        if (query instanceof SQLSelectQueryBlock queryBlock) {
            return parseQueryBlock(queryBlock, cteSources);
        } else if (query instanceof SQLUnionQuery unionQuery) {
            return parseUnionQuery(unionQuery, cteSources);
        }
        LOGGER.warning(() -> "不支持的查询类型: " +
                (query == null ? "null" : query.getClass().getSimpleName()));
        return null;
    }

    /**
     * 收集 WITH 子句中每个 CTE 的列血缘
     *
     * @return CTE 名 -> 该 CTE 暴露列名 -> 真实来源列 列表
     */
    private static Map<String, Map<String, List<ColumnNode>>> collectCteSources(SQLWithSubqueryClause withClause) {
        Map<String, Map<String, List<ColumnNode>>> result = new LinkedHashMap<>();
        if (withClause == null || withClause.getEntries() == null) {
            return result;
        }
        for (SQLWithSubqueryClause.Entry entry : withClause.getEntries()) {
            String name = entry.getAlias();
            if (StringUtils.isEmpty(name) || entry.getSubQuery() == null) {
                continue;
            }
            // CTE 的子查询自身也可能引用前面的 CTE
            Map<String, List<ColumnNode>> inner = extractColumnSources(entry.getSubQuery(), result);
            result.put(name, inner);
        }
        return result;
    }

    /**
     * 解析 SQLSelectQueryBlock，返回血缘树
     */
    private static TreeNode<ColumnNode> parseQueryBlock(SQLSelectQueryBlock queryBlock,
                                                       Map<String, Map<String, List<ColumnNode>>> ctes) {
        LOGGER.fine("解析 SELECT 查询块");
        var root = new TreeNode<ColumnNode>();

        ExprParseContext context = ExprParseContext.fromTableSource(queryBlock.getFrom());
        if (ctes != null) {
            ctes.forEach(context::registerVirtualTable);
        }
        registerNestedSubqueries(queryBlock.getFrom(), context, ctes);

        LOGGER.log(Level.FINE, () -> "表别名映射: " + context.getAliasToTableMap());

        List<SQLSelectItem> selectList = queryBlock.getSelectList();
        for (SQLSelectItem item : selectList) {
            SQLExpr expr = item.getExpr();
            String alias = item.getAlias();
            String itemName = item.toString();

            ColumnNode node = new ColumnNode();
            node.setName(itemName);
            node.setAlias(alias);
            node.setExpression(expr.toString());

            TreeNode<ColumnNode> child = TreeNode.of(node);
            root.addChild(child);

            context.setCurrentColumn(node);
            BaseSqlExprParser.parserSqlExpr(expr, context);

            String displayName = StringUtils.isEmpty(alias) ? itemName : alias;
            LOGGER.log(Level.FINE, () -> "列 " + displayName + " 的来源列数: " + node.getSourceColumns().size());
        }
        return root;
    }

    /**
     * 解析 UNION / INTERSECT / EXCEPT 查询，按列位置合并左右分支血缘
     */
    private static TreeNode<ColumnNode> parseUnionQuery(SQLUnionQuery unionQuery,
                                                       Map<String, Map<String, List<ColumnNode>>> ctes) {
        LOGGER.fine(() -> "解析 UNION 查询，操作符: " + unionQuery.getOperator());

        List<TreeNode<ColumnNode>> branches = new ArrayList<>();
        List<SQLSelectQuery> relations = unionQuery.getRelations();
        if (relations == null || relations.isEmpty()) {
            relations = new ArrayList<>();
            if (unionQuery.getLeft() != null) {
                relations.add(unionQuery.getLeft());
            }
            if (unionQuery.getRight() != null) {
                relations.add(unionQuery.getRight());
            }
        }

        for (SQLSelectQuery relation : relations) {
            TreeNode<ColumnNode> branch = parseSelectQuery(relation, ctes);
            if (branch != null) {
                branches.add(branch);
            }
        }

        if (branches.isEmpty()) {
            return null;
        }

        TreeNode<ColumnNode> root = new TreeNode<>();
        TreeNode<ColumnNode> firstBranch = branches.get(0);
        int columnCount = firstBranch.getChildren().size();

        for (int i = 0; i < columnCount; i++) {
            ColumnNode template = firstBranch.getChildren().get(i).getValue();
            ColumnNode merged = new ColumnNode();
            merged.setName(template.getName());
            merged.setAlias(template.getAlias());
            merged.setExpression(template.getExpression());

            for (TreeNode<ColumnNode> branch : branches) {
                if (i < branch.getChildren().size()) {
                    ColumnNode branchCol = branch.getChildren().get(i).getValue();
                    for (ColumnNode src : branchCol.getSourceColumns()) {
                        merged.addSourceColumn(src);
                    }
                }
            }
            root.addChild(TreeNode.of(merged));
        }
        return root;
    }

    /**
     * 派发 SQLSelectQueryBlock / SQLUnionQuery 解析
     */
    private static TreeNode<ColumnNode> parseSelectQuery(SQLSelectQuery query,
                                                        Map<String, Map<String, List<ColumnNode>>> ctes) {
        if (query instanceof SQLSelectQueryBlock block) {
            return parseQueryBlock(block, ctes);
        } else if (query instanceof SQLUnionQuery union) {
            return parseUnionQuery(union, ctes);
        }
        return null;
    }

    /**
     * 遍历 FROM 子句，将子查询表源的列血缘注册为虚拟表，便于上层 alias.col 下钻
     */
    private static void registerNestedSubqueries(SQLTableSource ts,
                                                 ExprParseContext context,
                                                 Map<String, Map<String, List<ColumnNode>>> ctes) {
        if (ts == null) {
            return;
        }
        if (ts instanceof SQLSubqueryTableSource subquery) {
            String alias = subquery.getAlias();
            if (!StringUtils.isEmpty(alias) && subquery.getSelect() != null) {
                Map<String, List<ColumnNode>> inner = extractColumnSources(subquery.getSelect(), ctes);
                context.registerVirtualTable(alias, inner);
            }
        } else if (ts instanceof SQLJoinTableSource join) {
            registerNestedSubqueries(join.getLeft(), context, ctes);
            registerNestedSubqueries(join.getRight(), context, ctes);
        } else if (ts instanceof SQLLateralViewTableSource lateralView) {
            registerNestedSubqueries(lateralView.getTableSource(), context, ctes);
            registerLateralViewOutputs(lateralView, context);
        }
    }

    /**
     * 将 LATERAL VIEW 的输出列映射到方法表达式的输入列
     * <p>
     * 例: {@code LATERAL VIEW explode(t.arr) v AS item}
     * 会注册虚拟表 v，其列 item 的来源为 t.arr
     */
    private static void registerLateralViewOutputs(SQLLateralViewTableSource lateralView, ExprParseContext context) {
        String alias = lateralView.getAlias();
        SQLMethodInvokeExpr method = lateralView.getMethod();
        List<SQLName> outputColumns = lateralView.getColumns();
        if (StringUtils.isEmpty(alias) || method == null
                || outputColumns == null || outputColumns.isEmpty()) {
            return;
        }
        List<ColumnNode> argSources = extractColumnRefs(method.getArguments(), context);
        if (argSources.isEmpty()) {
            return;
        }
        Map<String, List<ColumnNode>> outputs = new LinkedHashMap<>();
        for (SQLName outName : outputColumns) {
            outputs.put(outName.getSimpleName(), new ArrayList<>(argSources));
        }
        context.registerVirtualTable(alias, outputs);
    }

    /**
     * 从表达式列表中提取列引用（SQLPropertyExpr / SQLIdentifierExpr），别名按上下文还原为真实表名
     */
    private static List<ColumnNode> extractColumnRefs(List<SQLExpr> exprs, ExprParseContext context) {
        List<ColumnNode> result = new ArrayList<>();
        if (exprs == null) {
            return result;
        }
        for (SQLExpr e : exprs) {
            if (e instanceof SQLPropertyExpr p) {
                ColumnNode col = new ColumnNode();
                col.setTableName(context.resolveTableName(p.getOwnerName()));
                col.setName(p.getName());
                result.add(col);
            } else if (e instanceof SQLIdentifierExpr id) {
                ColumnNode col = new ColumnNode();
                col.setName(id.getName());
                result.add(col);
            } else if (e instanceof SQLMethodInvokeExpr m) {
                result.addAll(extractColumnRefs(m.getArguments(), context));
            }
        }
        return result;
    }

    /**
     * 提取 SQLSelect 的可见列 -> 真实来源列 映射，供 CTE / 子查询下钻使用
     */
    private static Map<String, List<ColumnNode>> extractColumnSources(SQLSelect select,
                                                                     Map<String, Map<String, List<ColumnNode>>> ctes) {
        if (select == null) {
            return Collections.emptyMap();
        }
        Map<String, Map<String, List<ColumnNode>>> merged = new LinkedHashMap<>();
        if (ctes != null) {
            merged.putAll(ctes);
        }
        merged.putAll(collectCteSources(select.getWithSubQuery()));

        TreeNode<ColumnNode> tree;
        var query = select.getQuery();
        if (query instanceof SQLSelectQueryBlock block) {
            tree = parseQueryBlock(block, merged);
        } else if (query instanceof SQLUnionQuery union) {
            tree = parseUnionQuery(union, merged);
        } else {
            return Collections.emptyMap();
        }

        Map<String, List<ColumnNode>> result = new LinkedHashMap<>();
        if (tree == null) {
            return result;
        }
        for (TreeNode<ColumnNode> child : tree.getChildren()) {
            ColumnNode col = child.getValue();
            String visible = visibleColumnName(col);
            if (visible != null) {
                result.put(visible, new ArrayList<>(col.getSourceColumns()));
            }
        }
        return result;
    }

    /**
     * 计算列的可见名称：优先 alias，否则尝试从表达式中提取列名
     */
    private static String visibleColumnName(ColumnNode column) {
        if (!StringUtils.isEmpty(column.getAlias())) {
            return stripQuotes(column.getAlias());
        }
        String expr = column.getExpression();
        if (expr == null) {
            return null;
        }
        int dot = expr.lastIndexOf('.');
        String name = dot >= 0 ? expr.substring(dot + 1) : expr;
        name = stripQuotes(name.trim());
        if (name.isEmpty() || name.contains("(") || name.contains(" ")) {
            return null;
        }
        return name;
    }

    private static String stripQuotes(String raw) {
        if (raw == null || raw.length() < 2) {
            return raw;
        }
        char first = raw.charAt(0);
        char last = raw.charAt(raw.length() - 1);
        if ((first == '`' && last == '`') || (first == '"' && last == '"')) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }
}
