package com.patricksalami.treeservice.service;

import com.patricksalami.treeservice.exceptions.CyclicalTreeStructureException;
import com.patricksalami.treeservice.exceptions.InvalidNodeException;
import com.patricksalami.treeservice.exceptions.MoveAttemptToSelfException;
import com.patricksalami.treeservice.exceptions.NodeExistsException;
import com.patricksalami.treeservice.dao.Node;
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
        Node existingNode = nodeRepository.findById(node.id);
        if(existingNode != null) {
            throw new NodeExistsException();
        }
        Node resultNode = nodeRepository.createNodesTableEntry(node);
        nodeRepository.createChildrenTableEntry(resultNode);
        nodeRepository.addNodeToParentUpdate(resultNode.id, resultNode.parentId);
        return resultNode;
    }

    /**
     * changes the parent of any node to any other valid node
     *
     * @param nodeId
     * @param newParentId
     * @throws RuntimeException
     */
    @Transactional
    public void moveNode(int nodeId, int newParentId) throws RuntimeException {
        // first, make sure we are moving a valid node to another valid node
        if (nodeId == newParentId) {
            throw new MoveAttemptToSelfException();
        }
        // moving a node to one of its descendants is not allowed
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

        //first, update the closure table to remove the parent-descendant relationships for all affected nodes
        nodeRepository.removeNodeFromParentUpdate(node.id, node.parentId);
        //then, update the main nodes table with the new parent
        nodeRepository.updateNodesTableEntry(nodeId, newParentId);
        //lastly, update the closure table with new parent-descendant entries for the moved node
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
