package com.patricksalami.treeservice;

import com.patricksalami.treeservice.model.Node;
import com.patricksalami.treeservice.service.NodeService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@SpringBootTest
public class NodeServiceTests {

    @Autowired
    NodeService nodeService;


    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:data.sql")
    public void createNode() {
        var n = new Node(2, 1, 1);
        nodeService.createNode(n);
        Node result = nodeService.findById(2);
        assertEquals(result.id, 2);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:data.sql")
    public void getDescendants() throws IOException {
        nodeService.createNode(new Node(2, 1, 1));
        nodeService.createNode(new Node(3, 1, 1));
        nodeService.createNode(new Node(4, 2, 1));
        nodeService.createNode(new Node(5, 2, 1));
        nodeService.createNode(new Node(6, 4, 1));
        nodeService.createNode(new Node(7, 4, 1));
        nodeService.createNode(new Node(8, 4, 1));

        var os = new ByteArrayOutputStream();

        nodeService.streamDescendantsById(2, os);
        os.close();
        String expectedOutput = "[" +
                "{\"id\":4,\"parentId\":2,\"rootId\":1,\"height\":2}," +
                "{\"id\":5,\"parentId\":2,\"rootId\":1,\"height\":2}," +
                "{\"id\":6,\"parentId\":4,\"rootId\":1,\"height\":3}," +
                "{\"id\":7,\"parentId\":4,\"rootId\":1,\"height\":3}," +
                "{\"id\":8,\"parentId\":4,\"rootId\":1,\"height\":3}" +
                "]";

        assertEquals(expectedOutput, os.toString());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:data.sql")
    public void isDescendantOf() {
        nodeService.createNode(new Node(2, 1, 1));
        nodeService.createNode(new Node(3, 1, 1));
        nodeService.createNode(new Node(4, 2, 1));
        assertTrue(nodeService.isDescendantOf(1, 4));
        assertFalse(nodeService.isDescendantOf(3, 4));
    }



}
