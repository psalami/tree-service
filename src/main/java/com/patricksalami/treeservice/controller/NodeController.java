package com.patricksalami.treeservice.controller;
import com.patricksalami.treeservice.exceptions.CyclicalTreeStructureException;
import com.patricksalami.treeservice.exceptions.InvalidNodeException;
import com.patricksalami.treeservice.exceptions.MoveAttemptToSelfException;
import com.patricksalami.treeservice.exceptions.NodeExistsException;
import com.patricksalami.treeservice.dao.Node;
import com.patricksalami.treeservice.service.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

@RestController()
public class NodeController {

    @Autowired
    NodeService nodeService;

    private final Logger logger = LoggerFactory.getLogger(NodeController.class);

    @RequestMapping(value = "/node/{id}/descendants", method = RequestMethod.GET)
    public ResponseEntity<StreamingResponseBody> getNode(@PathVariable("id") int nodeId,
                                                         final HttpServletResponse response) {
        response.setContentType("application/json");
        StreamingResponseBody stream = out -> {
            OutputStream o = response.getOutputStream();
            try {
                nodeService.streamDescendantsById(nodeId, o);
            } catch (final RuntimeException e) {
                logger.error("Exception while streaming data {}", e);
            } finally {
                o.close();
            }
        };

        return new ResponseEntity(stream, HttpStatus.OK);
    }

    @RequestMapping(value = "/node", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public void createNode(@RequestBody Node node) {
        nodeService.createNode(node);
    }

    @RequestMapping(value = "/node/{id}", method = RequestMethod.GET)
    public Node getNode(@PathVariable("id") int nodeId) {
        return nodeService.findById(nodeId);
    }

    @RequestMapping(value = "/moveNode/{id}/{newParentId}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public void moveNode(@PathVariable("id") int nodeId, @PathVariable("newParentId") int newParentId) {
        nodeService.moveNode(nodeId, newParentId);
    }

    @ExceptionHandler(CyclicalTreeStructureException.class)
    public final ResponseEntity<String> handleAllExceptions(CyclicalTreeStructureException ex) {
        return new ResponseEntity<String>("You may not move a node to one of its descendants",
                HttpStatus.LOOP_DETECTED);
    }

    @ExceptionHandler(InvalidNodeException.class)
    public final ResponseEntity<String> handleAllExceptions(InvalidNodeException e) {
        return new ResponseEntity<String>(String.format("The specified node %d does not exist", e.getNodeId()),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MoveAttemptToSelfException.class)
    public final ResponseEntity<String> handleAllExceptons(MoveAttemptToSelfException e) {
        return new ResponseEntity<String>("You may not move a node to itself", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(NodeExistsException.class)
    public final ResponseEntity<String> handleAllExceptons(NodeExistsException e) {
        return new ResponseEntity<String>("A node with this ID already exists", HttpStatus.CONFLICT);
    }

}
