package com.patricksalami.treeservice;

import com.patricksalami.treeservice.exceptions.CyclicalTreeStructureException;
import com.patricksalami.treeservice.exceptions.InvalidNodeException;
import com.patricksalami.treeservice.dao.Node;
import com.patricksalami.treeservice.service.NodeService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

@SpringBootTest
public class NodeServiceTests {

    private final Logger logger = LoggerFactory.getLogger(NodeServiceTests.class);

    @Autowired
    NodeService nodeService;

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
    public void moveNode() throws IOException {
        nodeService.createNode(new Node(2, 1, 1));
        nodeService.createNode(new Node(3, 1, 1));
        nodeService.createNode(new Node(4, 2, 1));
        nodeService.createNode(new Node(5, 2, 1));
        nodeService.createNode(new Node(6, 4, 1));
        nodeService.createNode(new Node(7, 4, 1));
        nodeService.createNode(new Node(8, 4, 1));

        // call to /api/v1/moveNode/4/3  -  moves node 4 (and its subtree) from parent 2 to to parent 3
        nodeService.moveNode(4, 3);

        // first check the previous parent to make sure it no longer has the moved subtree
        var osTwo = new ByteArrayOutputStream();
        nodeService.streamDescendantsById(2, osTwo);
        osTwo.close();
        String expectedOutputTwo = "[{\"id\":5,\"parentId\":2,\"rootId\":1,\"height\":2}]";
        assertEquals(expectedOutputTwo, osTwo.toString());

        // now check the new parent to make sure it has the moved subtree
        var osThree = new ByteArrayOutputStream();
        nodeService.streamDescendantsById(3, osThree);
        osThree.close();
        String expectedOutputThree = "[" +
                "{\"id\":4,\"parentId\":3,\"rootId\":1,\"height\":2}," +
                "{\"id\":6,\"parentId\":4,\"rootId\":1,\"height\":3}," +
                "{\"id\":7,\"parentId\":4,\"rootId\":1,\"height\":3}," +
                "{\"id\":8,\"parentId\":4,\"rootId\":1,\"height\":3}" +
                "]";
        assertEquals(expectedOutputThree, osThree.toString());
    }

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
    public void findById() {
        nodeService.createNode(new Node(2, 1, 1));
        nodeService.createNode(new Node(4, 2, 1));
        Node n = nodeService.findById(4);
        assertNotNull(n);
        assertEquals(4, n.id);
        assertEquals(2, n.parentId);
        assertEquals(2, n.height);
        assertEquals(1, n.rootId);

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

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:data.sql")
    public void noCycleAllowed() {
        nodeService.createNode(new Node(2, 1, 1));
        nodeService.createNode(new Node(3, 1, 1));
        nodeService.createNode(new Node(4, 2, 1));
        nodeService.createNode(new Node(5, 2, 1));
        nodeService.createNode(new Node(6, 4, 1));
        nodeService.createNode(new Node(7, 4, 1));
        nodeService.createNode(new Node(8, 4, 1));
        assertThrows(CyclicalTreeStructureException.class, () -> {
            nodeService.moveNode(2, 4);
        });
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:data.sql")
    public void invalidNodeMove() {
        nodeService.createNode(new Node(2, 1, 1));
        assertThrows(InvalidNodeException.class, () -> {
            nodeService.moveNode(2, 3);
        });
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:data.sql")
    public void createWithInvalidRoot() {
        assertThrows(InvalidNodeException.class, () -> {
            nodeService.createNode(new Node(2, 1, 99));
        });
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:data.sql")
    public void createWithInvalidParent() {
        assertThrows(InvalidNodeException.class, () -> {
            nodeService.createNode(new Node(2, 99, 1));
        });
    }

    @Test
    @Disabled("generating performance test data takes a while")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:data.sql")
    public void preparePerformanceTestData() {
        final int limit = 10000;
        Random r = new Random();

        for(int i = 2; i < limit; i++) {
            int parentId;
            if(i == 2) {
                parentId = 1;
            } else {
                parentId = (r.nextInt(i - 2) + 2) / 10;
                if(parentId < 3) {
                    parentId = 2;
                }
            }
            var n = new Node(i, parentId, 1);
            nodeService.createNode(n);
        }
    }

    @Test
    @Disabled("first prepare performance test data with preparePerformanceTestData")
    public void performanceTest() throws IOException {
        var os = new ByteArrayOutputStream();
        nodeService.streamDescendantsById(20, os);
        os.close();
    }







}
