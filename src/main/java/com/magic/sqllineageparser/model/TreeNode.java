package com.magic.sqllineageparser.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 通用泛型树结构节点
 * <p>
 * 支持父子关系、层高计算、子树大小统计等操作
 *
 * @param <T> 节点值类型
 * @author Guan Peixiang
 * @since 2023/9/12
 */
public class TreeNode<T> {

    @Getter
    private long id;
    @Setter
    @Getter
    private T value;
    @Getter
    private TreeNode<T> parent;
    private List<TreeNode<T>> children;
    @Getter
    private int height;
    @Getter
    private int subtreeSize;

    public TreeNode() {
    }

    public TreeNode(T value) {
        this.value = value;
    }

    /**
     * 静态工厂方法创建节点
     *
     * @param data 节点数据
     * @param <T>  数据类型
     * @return 新节点
     */
    public static <T> TreeNode<T> of(T data) {
        return new TreeNode<>(data);
    }

    /**
     * 获取根节点
     *
     * @return 树的根节点
     */
    public TreeNode<T> getRoot() {
        var current = this;
        while (current.parent != null) {
            current = current.parent;
        }
        return current;
    }

    /**
     * 判断是否为叶子节点
     *
     * @return true 如果没有子节点
     */
    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }

    /**
     * 判断是否只有一个叶子子节点
     *
     * @return true 如果只有一个子节点且该子节点为叶子
     */
    public boolean isOneChildAndLeaf() {
        return children != null && children.size() == 1 && children.get(0).isLeaf();
    }

    /**
     * 添加子节点
     *
     * @param childNode 子节点
     */
    public void addChild(TreeNode<T> childNode) {
        if (children == null) {
            children = new ArrayList<>();
        }

        childNode.parent = this;
        childNode.height = this.height + 1;
        childNode.id = this.id + 1;

        children.add(childNode);
        this.subtreeSize++;
    }

    /**
     * 获取子节点列表（不可变视图）
     *
     * @return 子节点列表，如果无子节点返回空列表
     */
    public List<TreeNode<T>> getChildren() {
        return children == null ? Collections.emptyList() : children;
    }

    /**
     * 获取可变的子节点列表
     *
     * @return 子节点列表，如果无子节点返回 null
     */
    public List<TreeNode<T>> getChildrenMutable() {
        return children;
    }

}
