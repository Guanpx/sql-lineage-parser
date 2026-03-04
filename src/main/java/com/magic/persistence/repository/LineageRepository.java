package com.magic.persistence.repository;

import com.magic.persistence.entity.LineageEdge;
import com.magic.persistence.entity.LineageGraph;
import com.magic.persistence.entity.LineageNode;

import java.util.List;

/**
 * 血缘数据持久化接口
 * <p>
 * 定义图数据库的读写操作，由具体实现类（Neo4j / Nebula）实现
 *
 * @author Guan Peixiang
 * @since 2023/12/20
 */
public interface LineageRepository {

    // ==================== 连接管理 ====================

    /**
     * 初始化连接
     */
    void connect();

    /**
     * 关闭连接
     */
    void close();

    /**
     * 检查连接是否有效
     *
     * @return true 如果连接正常
     */
    boolean isConnected();

    // ==================== 节点操作 ====================

    /**
     * 保存单个节点
     *
     * @param node 血缘节点
     */
    void saveNode(LineageNode node);

    /**
     * 批量保存节点
     *
     * @param nodes 节点列表
     */
    void saveNodes(List<LineageNode> nodes);

    /**
     * 根据ID查询节点
     *
     * @param nodeId 节点ID
     * @return 节点，不存在返回 null
     */
    LineageNode findNodeById(String nodeId);

    /**
     * 根据表名查询所有列节点
     *
     * @param tableName 表名
     * @return 列节点列表
     */
    List<LineageNode> findColumnsByTable(String tableName);

    /**
     * 删除节点及其关联的边
     *
     * @param nodeId 节点ID
     */
    void deleteNode(String nodeId);

    // ==================== 边操作 ====================

    /**
     * 保存单条边
     *
     * @param edge 血缘边
     */
    void saveEdge(LineageEdge edge);

    /**
     * 批量保存边
     *
     * @param edges 边列表
     */
    void saveEdges(List<LineageEdge> edges);

    /**
     * 查询节点的所有上游来源
     *
     * @param nodeId 节点ID
     * @return 上游节点列表
     */
    List<LineageNode> findUpstream(String nodeId);

    /**
     * 查询节点的所有下游影响
     *
     * @param nodeId 节点ID
     * @return 下游节点列表
     */
    List<LineageNode> findDownstream(String nodeId);

    // ==================== 图操作 ====================

    /**
     * 保存完整的血缘图（包括所有节点和边）
     *
     * @param graph 血缘图
     */
    void saveGraph(LineageGraph graph);

    /**
     * 根据SQL ID清除对应的血缘数据
     *
     * @param sqlId SQL唯一标识
     */
    void clearBySqlId(String sqlId);
}
