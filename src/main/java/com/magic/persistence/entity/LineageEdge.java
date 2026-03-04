package com.magic.persistence.entity;

import java.util.Objects;

/**
 * 血缘关系实体
 * <p>
 * 表示两个节点之间的血缘关系，用于持久化到图数据库
 *
 * @author Guan Peixiang
 * @since 2023/12/20
 */
public class LineageEdge {

    /**
     * 边的唯一标识
     */
    private String id;

    /**
     * 源节点ID
     */
    private String sourceNodeId;

    /**
     * 目标节点ID
     */
    private String targetNodeId;

    /**
     * 关系类型（如：DERIVES_FROM, TRANSFORMS_TO）
     */
    private String relationshipType;

    /**
     * 转换表达式（如函数调用）
     */
    private String transformExpression;

    /**
     * 创建时间戳
     */
    private Long createTime;

    /**
     * SQL语句ID（关联原始SQL）
     */
    private String sqlId;

    public LineageEdge() {
    }

    public LineageEdge(String sourceNodeId, String targetNodeId, String relationshipType) {
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.relationshipType = relationshipType;
        this.createTime = System.currentTimeMillis();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(String sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public String getTransformExpression() {
        return transformExpression;
    }

    public void setTransformExpression(String transformExpression) {
        this.transformExpression = transformExpression;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public String getSqlId() {
        return sqlId;
    }

    public void setSqlId(String sqlId) {
        this.sqlId = sqlId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LineageEdge that = (LineageEdge) o;
        return Objects.equals(sourceNodeId, that.sourceNodeId) &&
                Objects.equals(targetNodeId, that.targetNodeId) &&
                Objects.equals(relationshipType, that.relationshipType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceNodeId, targetNodeId, relationshipType);
    }

    @Override
    public String toString() {
        return "LineageEdge{" +
                "sourceNodeId='" + sourceNodeId + '\'' +
                " -[" + relationshipType + "]-> " +
                "targetNodeId='" + targetNodeId + '\'' +
                '}';
    }
}
