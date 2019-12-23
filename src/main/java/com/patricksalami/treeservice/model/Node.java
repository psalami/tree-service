package com.patricksalami.treeservice.model;


public class Node {
    public int id;
    public int parentId;
    public int rootId;

    public Node(int id, int parentId, int rootId) {
        this.id = id;
        this.parentId = parentId;
        this.rootId = rootId;
    }

    public Node(int id) {
        this.id = id;
    }

    public Node() {

    }
}
