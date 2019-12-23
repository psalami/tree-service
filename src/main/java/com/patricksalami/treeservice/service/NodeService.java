package com.patricksalami.treeservice.service;

import com.patricksalami.treeservice.exceptions.CyclicalTreeStructureException;
import com.patricksalami.treeservice.exceptions.InvalidNodeException;
import com.patricksalami.treeservice.exceptions.MoveAttemptToSelfException;
import com.patricksalami.treeservice.model.Node;
import com.patricksalami.treeservice.repository.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;

@Service()
public class NodeService {

    @Autowired
    NodeRepository nodeRepository;

    @Transactional
    public Node createNode(Node node) throws RuntimeException {
        Node resultNode = nodeRepository.createNodesTableEntry(node);
        nodeRepository.createChildrenTableEntry(resultNode);
        nodeRepository.addNodeToParentUpdate(resultNode.id, resultNode.parentId);
        return resultNode;
    }

    @Transactional
    public void moveNode(int nodeId, int newParentId) throws RuntimeException {
        if (nodeId == newParentId) {
            throw new MoveAttemptToSelfException();
        }
        if (nodeRepository.isDescendantOf(nodeId, newParentId)) {
            throw new CyclicalTreeStructureException();
        }
        Node node = nodeRepository.findById(nodeId);
        if(node == null) {
            throw new InvalidNodeException(nodeId);
        }
        Node newParent = nodeRepository.findById(newParentId);
        if(newParent == null) {
            throw new InvalidNodeException(newParentId);
        }

        nodeRepository.removeNodeFromParentUpdate(node.id, node.parentId);
        nodeRepository.updateNodesTableEntry(nodeId, newParentId);
        nodeRepository.addNodeToParentUpdate(nodeId, newParentId);
    }

    public Node findById(int id) throws RuntimeException {
        return nodeRepository.findById(id);
    }

    public void streamDescendantsById(int id, OutputStream outputStream) throws  RuntimeException {
        nodeRepository.streamDescendantsById(id, outputStream);
    }

    public boolean isDescendantOf(int parentId, int childId) {
        return nodeRepository.isDescendantOf(parentId, childId);
    }

}
