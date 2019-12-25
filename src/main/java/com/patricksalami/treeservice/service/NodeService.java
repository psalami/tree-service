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
        // first, ensure that a node with this id does not already exist
        Node existingNode = nodeRepository.findById(node.id);
        if(existingNode != null) {
            throw new NodeExistsException();
        }
        // now, ensure that the parent node and root node of this node exist
        Node parentNode = nodeRepository.findById(node.parentId);
        if(parentNode == null && node.parentId > 0) {
            throw new InvalidNodeException(node.parentId);
        }
        Node rootNode = nodeRepository.findById(node.rootId);
        if(rootNode == null && node.id != node.rootId) {
            throw new InvalidNodeException(node.rootId);
        }
        Node resultNode = nodeRepository.createNodesTableEntry(node);
        nodeRepository.createChildrenTableEntry(resultNode);
        nodeRepository.addNodeToParentUpdate(resultNode.id, resultNode.parentId);
        // in order to calculate height, we re-load the Node from the repo
        Node outputNode = nodeRepository.findById(resultNode.id);
        return outputNode;
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
        // moving a node to itself is not allowed
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
        int oldParentId = node.parentId;
        node.parentId = newParentId;
        // if we are moving the node to a new tree, update the root
        node.rootId = newParent.rootId;

        //first, update the closure table to remove the parent-descendant relationships for all affected nodes
        nodeRepository.removeNodeFromParentUpdate(node.id, oldParentId);
        //then, update the main nodes table with the new parent
        nodeRepository.updateNodesTableEntry(node);
        //now, update the one record in the children table that references the node as its own descendant
        nodeRepository.updateChildrenTableEntry(node);
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
