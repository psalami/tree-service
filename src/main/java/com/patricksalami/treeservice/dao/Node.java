package com.patricksalami.treeservice.dao;


public class Node {
    public int id;
    public int parentId;
    public int rootId;
    public int height;

    public Node(int id, int parentId, int rootId) {
        this.id = id;
        this.parentId = parentId;
        this.rootId = rootId;
    }

    public Node(int id, int parentId, int rootId, int height) {
        this.id = id;
        this.parentId = parentId;
        this.rootId = rootId;
        this.height = height;
    }

    public Node(int id) {
        this.id = id;
    }

    public Node() {

    }
}
