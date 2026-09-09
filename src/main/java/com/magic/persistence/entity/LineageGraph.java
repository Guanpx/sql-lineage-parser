package com.magic.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 血缘图实体
 * <p>
 * 包含一组节点和边，表示完整的血缘关系图
 *
 * @author Guan Peixiang
 * @since 2023/12/20
 */
@Setter
@Getter
public class LineageGraph {

    /**
     * 图的唯一标识
     */
    private String id;

    /**
     * 图名称（如SQL语句摘要）
     */
    private String name;

    /**
     * 原始SQL语句
     */
    private String sql;

    /**
     * 所有节点
     */
    private List<LineageNode> nodes = new ArrayList<>();

    /**
     * 所有边
     */
    private List<LineageEdge> edges = new ArrayList<>();

    /**
     * 创建时间戳
     */
    private Long createTime;

    public LineageGraph() {
        this.createTime = System.currentTimeMillis();
    }

    public LineageGraph(String sql) {
        this();
        this.sql = sql;
        this.id = "graph:" + sql.hashCode();
    }

    /**
     * 添加节点
     */
    public void addNode(LineageNode node) {
        if (!nodes.contains(node)) {
            nodes.add(node);
        }
    }

    /**
     * 添加边
     */
    public void addEdge(LineageEdge edge) {
        if (!edges.contains(edge)) {
            edges.add(edge);
        }
    }

    /**
     * 添加血缘关系
     *
     * @param source           源节点
     * @param target           目标节点
     * @param relationshipType 关系类型
     */
    public void addLineage(LineageNode source, LineageNode target, String relationshipType) {
        addNode(source);
        addNode(target);
        addEdge(new LineageEdge(source.getId(), target.getId(), relationshipType));
    }

    // Getters and Setters

    @Override
    public String toString() {
        return "LineageGraph{" +
                "id='" + id + '\'' +
                ", nodes=" + nodes.size() +
                ", edges=" + edges.size() +
                '}';
    }
}
