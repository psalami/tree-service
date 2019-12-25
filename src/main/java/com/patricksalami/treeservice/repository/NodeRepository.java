package com.patricksalami.treeservice.repository;


import com.patricksalami.treeservice.dao.Node;
import com.patricksalami.treeservice.util.JsonResultSetExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.io.OutputStream;

@Repository
public class NodeRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * returns a single Node object based on its id; the node height is determined by checking the entry in the closure
     * table that links the node's root node to the node and referencing the depth column of that record
     *
     * @param id
     * @return
     * @throws RuntimeException
     */
    public Node findById(int id) throws RuntimeException {
        var parameterSource = new MapSqlParameterSource()
                .addValue("nodeId", id);
        var sql = "SELECT id, n.parent as parentId, n.root as rootId, d.depth as height " +
                "FROM nodes n " +
                "LEFT JOIN children d ON d.parent = n.root AND d.child = :nodeId " +
                "WHERE id = :nodeId";
        try {
            return namedParameterJdbcTemplate.queryForObject(sql, parameterSource,
                    (resultSet, rowNumber) -> new Node(resultSet.getInt("id"),
                            resultSet.getInt("parentId"), resultSet.getInt("rootId"),
                            resultSet.getInt("height")));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }

    }

    /**
     * Streams all descendants of a given node to the provided OutputStream in JSON format using a fast and simple
     * SELECT query;
     * in order to support a large number of nodes in the subtree, we do not represent the nodes in memory and
     * instead construct the JSON output on the fly as we are streaming rows from the db. Therefore we opt not to
     * use JPA with this Repository.
     *
     * @param id
     * @param outputStream
     * @throws RuntimeException
     */
    public void streamDescendantsById(int id, OutputStream outputStream) throws RuntimeException {
        try {
            var parameterSource = new MapSqlParameterSource()
                    .addValue("nodeId", id);
            var sql = "SELECT c.child as \"id\", childNodes.parent as \"parentId\", childNodes.root as \"rootId\", " +
                    "d.depth as height " +
                    "FROM " +
                    "children c " +
                    "LEFT JOIN nodes childNodes ON c.child = childNodes.id " +
                    "LEFT JOIN children d ON d.parent = childNodes.root AND d.child = c.child " +
                    "WHERE c.parent = :nodeId AND c.child != :nodeId;";
            namedParameterJdbcTemplate.query(sql, parameterSource, new JsonResultSetExtractor(outputStream));
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * adds a single entry to the closure (children) table that links the node to itself as one of its descendants
     *
     * @param node
     * @throws RuntimeException
     */
    public void createChildrenTableEntry(Node node) throws RuntimeException {
        var sql = "INSERT INTO children(parent, child, depth) " +
                "VALUES(:nodeId, :nodeId, 0)";
        var parameterSource = new MapSqlParameterSource()
                .addValue("nodeId", node.id);
        namedParameterJdbcTemplate.update(sql, parameterSource);
    }

    /**
     * Use a self-join to update the closure (children) table; for each parent of the node that is being moved, we
     * add a row that links the parent to the node being moved or inserted, as well as each of its descendants as
     * parent-descendant relationships. This is called whenever a node is inserted or
     * moved. In the case of insertions, this query only adds a single entry for each ancestor node, making insertions
     * very fast.
     *
     * This approach makes it very easy to look up all descendants for a given node, at the expense of using additional
     * memory; however, moving a node from one parent to another is fast allowing this approach to make strong
     * consistency and atomicity guarantees.
     *
     * @param nodeId
     * @param parentId
     * @throws RuntimeException
     */
    public void addNodeToParentUpdate(int nodeId, int parentId) throws RuntimeException {
        var sql = "INSERT into children(parent, child, depth) " +
                "SELECT p.parent, c.child, p.depth+c.depth+1 " +
                "FROM children p, children c " +
                "WHERE p.child=:parentId and c.parent=:childId";
        var parameterSource = new MapSqlParameterSource()
                .addValue("parentId", parentId)
                .addValue("childId", nodeId);
        namedParameterJdbcTemplate.update(sql, parameterSource);
    }

    /**
     * references the closure table to check for an entry that links the parent node to the child node; if no such entry
     * exists, then there is no parent-descendant relationship between the two nodes
     *
     * @param parentId
     * @param childId
     * @return
     * @throws RuntimeException
     */
    public boolean isDescendantOf(int parentId, int childId) throws RuntimeException {
        var sql = "SELECT CASE WHEN EXISTS (" +
                        "SELECT parent " +
                        "FROM children " +
                        "WHERE parent = :parentId AND child = :childId" +
                    ") " +
                    "THEN TRUE " +
                    "ELSE FALSE " +
                    "END as exists";

        var parameterSource = new MapSqlParameterSource()
                .addValue("parentId", parentId)
                .addValue("childId", childId);
        return namedParameterJdbcTemplate.queryForObject(sql, parameterSource, (resultSet, rowNumber) -> resultSet.getBoolean("exists"));
    }

    /**
     * This is the reverse of addNodeToParentUpdate; we delete all parent-descendant relationships for all parents
     * of the node and the nodes in its subtree.
     *
     * @param nodeId
     * @param parentId
     * @throws RuntimeException
     */
    public void removeNodeFromParentUpdate(int nodeId, int parentId) throws RuntimeException {
        var sql = "DELETE FROM children " +
                "USING " +
                "(SELECT link.parent AS parent, link.child AS child, link.depth AS depth " +
                " FROM children p, children link, children c " +
                " WHERE p.parent = link.parent AND c.child = link.child " +
                "   AND p.child=:parentId AND c.parent=:childId) l " +
                "WHERE children.parent = l.parent AND children.child = l.child AND children.depth = l.depth";
        var parameterSource = new MapSqlParameterSource()
                .addValue("parentId", parentId)
                .addValue("childId", nodeId);
        namedParameterJdbcTemplate.update(sql, parameterSource);
    }

    /**
     * creates a single entry in the main nodes table; this is called initially whenever a new node is created
     *
     * @param node
     * @return
     * @throws RuntimeException
     */
    public Node createNodesTableEntry(Node node) throws RuntimeException {
        var sql = "INSERT INTO nodes(id, parent, root) " +
                "VALUES (:nodeId, :parentId, :rootNodeId);";
        var parameterSource = new MapSqlParameterSource()
                .addValue("nodeId", node.id)
                .addValue("parentId", node.parentId)
                .addValue("rootNodeId", node.rootId);
        KeyHolder holder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update(sql, parameterSource, holder);
        var resultNode = new Node();
        resultNode.id = (int) holder.getKeys().getOrDefault("id", -1L);
        if(holder.getKeys().containsKey("parent")) {
            resultNode.parentId = (int) holder.getKeys().get("parent");
        }
        if(holder.getKeys().containsKey("root")) {
            resultNode.rootId = (int) holder.getKeys().get("root");
        }

        return resultNode;
    }

    /**
     * updates the main nodes table with a new node and its parent relationship; this is called whenever a node is
     * moved to a new parent
     *
     * @param nodeId
     * @param newParentId
     * @throws RuntimeException
     */
    public void updateNodesTableEntry(int nodeId, int newParentId) throws RuntimeException {
        var sql = "UPDATE nodes  " +
                "SET parent = :parentId " +
                "WHERE id = :nodeId";
        var parameterSource = new MapSqlParameterSource()
                .addValue("parentId", newParentId)
                .addValue("nodeId", nodeId);
        namedParameterJdbcTemplate.update(sql, parameterSource);
    }

}
