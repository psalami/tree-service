package com.patricksalami.treeservice.exceptions;

public class InvalidNodeException extends RuntimeException {
    private int nodeId;
    public InvalidNodeException(int nodeId) {
        this.nodeId = nodeId;
    }

    public int getNodeId() {
        return nodeId;
    }
}
