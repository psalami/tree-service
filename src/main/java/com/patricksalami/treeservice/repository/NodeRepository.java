package com.patricksalami.treeservice.repository;


import com.patricksalami.treeservice.model.Node;
import com.patricksalami.treeservice.util.JsonResultSetExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.io.OutputStream;
import java.sql.ResultSet;

@Repository
public class NodeRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public static final int ROOT_NODE_ID = 1;

    public Node findById(int id) throws RuntimeException {
        var parameterSource = new MapSqlParameterSource()
                .addValue("nodeId", id);
        var sql = "SELECT id, parent as parentId, root as rootId\n" +
                "FROM nodes\n" +
                "WHERE id = :nodeId;";
        return namedParameterJdbcTemplate.queryForObject(sql, parameterSource,
                (resultSet, rowNumber) -> new Node(resultSet.getInt("id"),
                        resultSet.getInt("parentId"), resultSet.getInt("rootId")));
    }

    public void streamDescendantsById(int id, OutputStream outputStream) throws RuntimeException {
        try {
            var parameterSource = new MapSqlParameterSource()
                    .addValue("nodeId", id)
                    .addValue("rootNodeId", ROOT_NODE_ID);
            var sql = "SELECT c.child as \"id\", childNodes.parent as \"parentId\", childNodes.root as \"rootId\", " +
                    "d.depth as height " +
                    "FROM " +
                    "children c " +
                    "LEFT JOIN nodes childNodes ON c.child = childNodes.id " +
                    "LEFT JOIN children d ON d.parent = :rootNodeId and d.child = c.child " +
                    "WHERE c.parent = :nodeId AND c.child != :nodeId;";
            namedParameterJdbcTemplate.query(sql, parameterSource, new JsonResultSetExtractor(outputStream));
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void createChildrenTableEntry(Node node) throws RuntimeException {
        var sql = "INSERT INTO children(parent, child, depth) " +
                "VALUES(:nodeId, :nodeId, 0)";
        var parameterSource = new MapSqlParameterSource()
                .addValue("nodeId", node.id);
        namedParameterJdbcTemplate.update(sql, parameterSource);
    }

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
