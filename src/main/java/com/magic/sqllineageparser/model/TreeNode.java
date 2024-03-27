package com.magic.sqllineageparser.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>
 *
 * @author Guan Peixiang (guanpeixiang@juzishuke.com)
 * @date 2023/9/12
 */
public class TreeNode<T> {

    AtomicLong id = new AtomicLong(0);

    T value;

    // root 根节点
    TreeNode<T> parent;

    // leaves leaf复数
    List<TreeNode<T>> children;

    int height;

    int subtreeSize;
    // 当前节点 and 所有子节点 todo
    private List<TreeNode<T>> elementsIndex;

    public TreeNode() {
    }

    TreeNode(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public TreeNode<T> getRoot() {
        TreeNode<T> current = this;
        while (current.parent != null) {
            current = current.parent;
        }
        return current;
    }

    public void initChildList() {
        if (children == null) {
            children = new ArrayList<>();
        }
    }

    public boolean isLeaf() {
        if (children == null) {
            return true;
        }
        return children.size() == 0;
    }

    public boolean isOneChildAndLeaf() {
        return children != null && children.size() == 1 && children.get(0).isLeaf();
    }

    public void addChild(TreeNode<T> childNode) {
        initChildList();
        childNode.parent = this;
        children.add(childNode);
        childNode.height = Optional.ofNullable(childNode.parent)
                .map(node -> node.height + 1)
                .orElse(0);
        this.subtreeSize++;
        childNode.id = new AtomicLong(Optional.ofNullable(childNode.parent)
                .map(node -> node.id.get() + 1)
                .orElse(0L));
    }

    public List<TreeNode<T>> getChildren() {
        return children;
    }

    public AtomicLong getId() {
        return id;
    }

    public int getHeight() {
        return height;
    }

    public int getSubtreeSize() {
        return subtreeSize;
    }

    public TreeNode<T> getParent() {
        return parent;
    }

    public static <T> TreeNode<T> of(T data) {
        TreeNode<T> treeNode = new TreeNode<>();
        treeNode.setValue(data);
        return treeNode;
    }

}



