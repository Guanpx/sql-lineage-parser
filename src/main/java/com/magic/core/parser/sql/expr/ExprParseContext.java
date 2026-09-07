package com.magic.core.parser.sql.expr;

import com.alibaba.druid.sql.ast.statement.*;
import com.magic.sqllineageparser.model.ColumnNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表达式解析上下文
 * <p>
 * 保存解析过程中的状态信息和收集解析结果
 *
 * @author Guan Peixiang
 * @since 2023/12/20
 */
public class ExprParseContext {

    /**
     * 表别名 -> 真实表名 的映射
     */
    private final Map<String, String> aliasToTableMap = new HashMap<>();

    /**
     * 虚拟表（CTE / 子查询）列下钻映射
     * <p>
     * key:   虚拟表别名（CTE 名或子查询别名）<br>
     * value: 该虚拟表的可见列名 -> 真实来源列 列表
     */
    private final Map<String, Map<String, List<ColumnNode>>> virtualTableColumns = new HashMap<>();

    /**
     * 当前正在解析的目标列节点
     */
    private ColumnNode currentColumn;

    /**
     * 解析过程中收集到的所有来源列
     */
    private final List<ColumnNode> sourceColumns = new ArrayList<>();

    public ExprParseContext() {
    }

    /**
     * 从 FROM 子句创建解析上下文
     *
     * @param tableSource FROM 子句的表源
     * @return 解析上下文
     */
    public static ExprParseContext fromTableSource(SQLTableSource tableSource) {
        ExprParseContext context = new ExprParseContext();
        context.collectTableAlias(tableSource);
        return context;
    }

    /**
     * 设置当前正在解析的目标列
     */
    public void setCurrentColumn(ColumnNode column) {
        this.currentColumn = column;
        this.sourceColumns.clear();
    }

    /**
     * 获取当前目标列
     */
    public ColumnNode getCurrentColumn() {
        return currentColumn;
    }

    /**
     * 添加来源列
     * <p>
     * 当 tableName 命中虚拟表（CTE / 子查询）时，自动下钻到真实表的真实列；
     * 否则按 alias → 真实表名 还原，并写入一个普通来源列。
     *
     * @param tableName  表名（或别名 / CTE 名 / 子查询别名）
     * @param columnName 列名
     */
    public void addSourceColumn(String tableName, String columnName) {
        if (drillDownVirtual(tableName, columnName)) {
            return;
        }
        ColumnNode source = new ColumnNode();
        source.setTableName(resolveTableName(tableName));
        source.setName(columnName);
        source.setConstant(false);
        recordSource(source);
    }

    /**
     * 添加常量来源（无表名）
     *
     * @param value 常量值
     */
    public void addConstantSource(String value) {
        ColumnNode source = new ColumnNode();
        source.setName(value);
        source.setConstant(true);
        recordSource(source);
    }

    /**
     * 添加无表名的列引用
     * <p>
     * 当 FROM 中只有唯一表（含虚拟表）时，自动将该列归属到这张表；
     * 否则按裸列保留（无 tableName）。
     *
     * @param columnName 列名
     */
    public void addColumnReference(String columnName) {
        String inferredTable = inferSingleTableName();
        if (inferredTable != null) {
            addSourceColumn(inferredTable, columnName);
            return;
        }
        ColumnNode source = new ColumnNode();
        source.setName(columnName);
        source.setConstant(false);
        recordSource(source);
    }

    /**
     * 当 FROM 子句仅含唯一表（含虚拟表）时返回该表名，否则返回 null
     */
    private String inferSingleTableName() {
        if (aliasToTableMap.size() == 1) {
            return aliasToTableMap.values().iterator().next();
        }
        return null;
    }

    /**
     * 获取所有收集到的来源列
     */
    public List<ColumnNode> getSourceColumns() {
        return new ArrayList<>(sourceColumns);
    }

    /**
     * 解析真实表名（将别名转换为真实表名）
     *
     * @param aliasOrTableName 别名或表名
     * @return 真实表名
     */
    public String resolveTableName(String aliasOrTableName) {
        if (aliasOrTableName == null) {
            return null;
        }
        return aliasToTableMap.getOrDefault(aliasOrTableName, aliasOrTableName);
    }

    /**
     * 添加表别名映射
     *
     * @param alias     别名
     * @param tableName 真实表名
     */
    public void addTableAlias(String alias, String tableName) {
        aliasToTableMap.put(alias, tableName);
    }

    /**
     * 获取别名映射（只读）
     */
    public Map<String, String> getAliasToTableMap() {
        return Map.copyOf(aliasToTableMap);
    }

    /**
     * 注册虚拟表（CTE 或子查询）的列血缘映射
     *
     * @param alias         虚拟表别名 / CTE 名
     * @param columnSources 该虚拟表暴露的列名 -> 真实来源列
     */
    public void registerVirtualTable(String alias, Map<String, List<ColumnNode>> columnSources) {
        if (alias == null || columnSources == null || columnSources.isEmpty()) {
            return;
        }
        virtualTableColumns.put(alias, columnSources);
        aliasToTableMap.putIfAbsent(alias, alias);
    }

    /**
     * 判断指定别名是否为已注册虚拟表
     */
    public boolean isVirtualTable(String alias) {
        return alias != null && virtualTableColumns.containsKey(alias);
    }

    /**
     * 获取虚拟表列血缘映射（只读视图）
     */
    public Map<String, List<ColumnNode>> getVirtualTableColumns(String alias) {
        Map<String, List<ColumnNode>> cols = virtualTableColumns.get(alias);
        return cols == null ? Collections.emptyMap() : Collections.unmodifiableMap(cols);
    }

    /**
     * 命中虚拟表时下钻到真实来源列
     *
     * @return true 表示已下钻并记录来源；false 表示不是虚拟表，调用方应按普通逻辑处理
     */
    private boolean drillDownVirtual(String tableName, String columnName) {
        if (tableName == null) {
            return false;
        }
        // 先尝试直接命中，再尝试通过别名解析后的真实名命中
        Map<String, List<ColumnNode>> cols = virtualTableColumns.get(tableName);
        if (cols == null) {
            String resolved = aliasToTableMap.get(tableName);
            if (resolved != null && !resolved.equals(tableName)) {
                cols = virtualTableColumns.get(resolved);
            }
        }
        if (cols == null) {
            // 不是虚拟表，交给普通逻辑（按 alias -> 真实表名 还原）
            return false;
        }
        // 已确认命中虚拟表：能下钻则展开到真实来源列
        List<ColumnNode> realSources = cols.get(columnName);
        if (realSources != null && !realSources.isEmpty()) {
            for (ColumnNode src : realSources) {
                recordSource(cloneColumn(src));
            }
            return true;
        }
        // 命中虚拟表但列名无法下钻（列不在暴露列中 / 该列可见名未能提取，如 SELECT *）：
        // 记录为未解析列（tableName=null），避免把虚拟表别名当作真实表名泄漏到血缘
        ColumnNode unresolved = new ColumnNode();
        unresolved.setName(columnName);
        unresolved.setConstant(false);
        recordSource(unresolved);
        return true;
    }

    private void recordSource(ColumnNode source) {
        sourceColumns.add(source);
        if (currentColumn != null) {
            currentColumn.addSourceColumn(source);
        }
    }

    private static ColumnNode cloneColumn(ColumnNode src) {
        ColumnNode copy = new ColumnNode();
        copy.setTableName(src.getTableName());
        copy.setName(src.getName());
        copy.setAlias(src.getAlias());
        copy.setConstant(src.isConstant());
        copy.setExpression(src.getExpression());
        return copy;
    }

    /**
     * 递归收集表别名
     */
    private void collectTableAlias(SQLTableSource tableSource) {
        if (tableSource == null) {
            return;
        }

        if (tableSource instanceof SQLExprTableSource exprTable) {
            String tableName = exprTable.getExpr().toString();
            String alias = exprTable.getAlias();
            if (alias != null && !alias.isEmpty()) {
                aliasToTableMap.put(alias, tableName);
            } else {
                aliasToTableMap.put(tableName, tableName);
            }
        } else if (tableSource instanceof SQLJoinTableSource joinTable) {
            collectTableAlias(joinTable.getLeft());
            collectTableAlias(joinTable.getRight());
        } else if (tableSource instanceof SQLSubqueryTableSource subquery) {
            String alias = subquery.getAlias();
            if (alias != null && !alias.isEmpty()) {
                aliasToTableMap.put(alias, alias);
            }
        } else if (tableSource instanceof SQLUnionQueryTableSource unionTable) {
            String alias = unionTable.getAlias();
            if (alias != null && !alias.isEmpty()) {
                aliasToTableMap.put(alias, alias);
            }
        } else if (tableSource instanceof SQLLateralViewTableSource lateralView) {
            collectTableAlias(lateralView.getTableSource());
            String alias = lateralView.getAlias();
            if (alias != null && !alias.isEmpty()) {
                aliasToTableMap.put(alias, alias);
            }
        }
    }

    @Override
    public String toString() {
        return "ExprParseContext{" +
                "aliasToTableMap=" + aliasToTableMap +
                ", virtualTables=" + virtualTableColumns.keySet() +
                ", currentColumn=" + (currentColumn != null ? currentColumn.getName() : "null") +
                ", sourceColumnsCount=" + sourceColumns.size() +
                '}';
    }
}
