# Tree Service

## Contents
- [Introduction](#Introduction)
- [Installation](#Installation)
- [Usage](#Usage)
- [Testing](#Testing)
- [Performance](#Performance)

## Introduction
This is a REST API service based on Spring Boot that retrieves and modifies nodes in a tree structure. The service
is designed to handle a large number of nodes and incorporates persistence to disk using a Postgres database (but is
designed to be database-agnostic). Several approaches were considered in order to accomplish both fast subtree 
retrieval, fast subtree modification (changing the parent of a node), and data persistence. 
In order to achieve both data persistence with strong consistency guarantees, as well as fast subtree retrieval
and modification, a **materialized path** approach with a closure table is used. Strategies for SQL-based subtree 
retrieval and modification that were considered included:

- Adjacency lists
    - naive approach
    - easy to implement but very slow subtree retrieval
    - expensive to determine height
    - very fast subtree modification
    - low memory footprint
    - can be implemented with any SQL database
- Nested Sets
    - more complex to implement
    - very fast subtree retrieval but much slower O(n/2) subtree modification (insert, move, delete)
    - lower memory footprint
    - can be implemented with any SQL database
- Materialized Path (Closure Table)
    - relatively straightforward to implement
    - very fast subtree retrieval (as fast as nested sets)
    - fast O(log n) (size of subtree) subtree modification
    - very fast insertions
    - higher memory footprint
    - can be implemented with any SQL database
- Materialized Path (Lineage Column)
    - performance comparable to closure table
    - more difficult to implement
    - relies on string manipulation or Array type (vendor-specific)
- Database-specific approaches / third-party libraries (i.e. Postgres ltree, Neo4J, recursive CTEs, django-treebeard, etc)
    - locks into a specific database vendor
    - may require additional infrastructure / maintenance
    - may require specific tech stack (i.e. Python)
    - ltree performance comparable to custom materialized path implementation

The materialized path approach was selected using a custom implementation of closure tables because it offers
the fastest retrieval and modification times compared to the other approaches, while remaining completely
database-agnostic. The trade-off is a higher disk space requirement for the database compared to other approaches.

This service is optimized to remain performant even with larger data volumes and therefore does not store retrieved
nodes in the application server's working memory; instead, data is streamed directly from the database to the 
HTTP client. Therefore, we cannot use any built-in persistence framework such as JPA / Hibernate. 
By passing the retrieved subtree data directly from the database to the client, we are not limited by the amount 
of memory available on the server node; instead the size of the dataset is limited by the amount of disk space 
available to the db, which could be scaled vertically or horizontally.

## Installation
This service requires Docker and docker-compose to be installed on your system. 
In addition, HTTP port 8084 must be available on the host.
```bash
$ cd tree-service
$ docker-compose up
```

This will first create two PostgreSQL containers (one for the unit tests and one for the API)
and then run the unit tests. After the unit tests run successfully, the API container starts and 
exposes the Spring Boot service on the host port 8084.

## Usage

The service comes pre-loaded with the following sample data:
```
            1
          /   \
         2     3
       /  \
      4    5
    / \\
   6  7 8
```


#### Get Descendants
Use the following command to return all descendants of any node.
```
$ curl http://localhost:8084/api/v1/node/2/descendants
[
  {
    "id": 4,
    "parentId": 2,
    "rootId": 1,
    "height": 2
  },
  {
    "id": 5,
    "parentId": 2,
    "rootId": 1,
    "height": 2
  },
  {
    "id": 6,
    "parentId": 4,
    "rootId": 1,
    "height": 3
  },
  {
    "id": 7,
    "parentId": 4,
    "rootId": 1,
    "height": 3
  },
  {
    "id": 8,
    "parentId": 4,
    "rootId": 1,
    "height": 3
  }
]
``` 

#### Move Node
Use the following command to move any node (and its subtree) to a new parent node:
```
$ curl http://localhost:8084/api/v1/moveNode/4/3
```
This moves node 4 from its previous parent node to node 3 as its new parent node.

We can then verify that the node was moved by checking the descendants. We first check the descendants of the previous
parent node to ensure that the child node was removed:
```
$ curl http://localhost:8084/api/v1/node/2/descendants
[
  {
    "id": 5,
    "parentId": 2,
    "rootId": 1,
    "height": 2
  }
]
```

Now, we verify that the new parent has the child node and its subtree:
```
$ curl http://localhost:8084/api/v1/node/3/descendants
[
  {
    "id": 4,
    "parentId": 3,
    "rootId": 1,
    "height": 2
  },
  {
    "id": 6,
    "parentId": 4,
    "rootId": 1,
    "height": 3
  },
  {
    "id": 7,
    "parentId": 4,
    "rootId": 1,
    "height": 3
  },
  {
    "id": 8,
    "parentId": 4,
    "rootId": 1,
    "height": 3
  }
]
```

The new tree structure now looks like this:
```
            1
          /   \
         2     3
        /      /
       5      4    
            / \\
           6  7 8
```

#### Create New Node
We can create additional nodes and insert them into the tree for further testing:

```
$ curl -X POST \
   http://localhost:8084/api/v1/node \
   -H 'Content-Type: application/json' \
   -d '{"id": 9, "parentId": 2, "rootId": 1}'
{
  "id": 9,
  "parentId": 2,
  "rootId": 1,
  "height": 2
}
```

We can verify that the new node was inserted at the correct position in the tree:
```
$ curl http://localhost:8084/api/v1/node/2/descendants
[
  {
    "id": 9,
    "parentId": 2,
    "rootId": 1,
    "height": 2
  },
  {
    "id": 5,
    "parentId": 2,
    "rootId": 1,
    "height": 2
  }
]
```

*Note*: to create a new root node, create a new node without parentId and with rootId = id, for example:
```
$ curl -X POST \
   http://localhost:8084/api/v1/node \
   -H 'Content-Type: application/json' \
   -d '{"id": 11, "rootId": 11}'
{
  "id": 11,
  "parentId": 0,
  "rootId": 11,
  "height": 0
}
```
A height of 0 and parentId of 0 indicates a root node. You may move any node / subtree under a new root node. 

#### Get Single Node
We can get information about a single node using the following command:
```
$ curl http://localhost:8084/api/v1/node/4
{
  "id": 4,
  "parentId": 2,
  "rootId": 1,
  "height": 2
}
```

## Error Handling

If we try to move a node to one of its descendants, we get an HTTP 508 (Loop Detected) error:
```
$ curl -i http://localhost:8084/api/v1/moveNode/4/6
HTTP/1.1 508
You may not move a node to one of its descendants
```

If we try to move a node to a node that does not exist, we get an HTTP 404 (Not Found) error:
```
$ curl -i http://localhost:8084/api/v1/moveNode/4/99
HTTP/1.1 404
The specified node 99 does not exist
```

## Testing
To run unit tests again:
```
$ docker-compose run test
```

## Performance
Performance test results with Postgres 12.1 and Java 12, on a 2017 MacBook Pro 2.9Ghz Core i7 CPU with 16GB RAM:

**Write performance:**
- Creating a tree with 100,000 nodes: 13m
- Moving a node with height = 2 and number of descendants = 1,636: 550ms
- Moving a node with height = 8 to height = 2 and number of descendants = 2: 35ms
- Inserting a single node at height = 8: 14ms
- Inserting a single node at height = 2: 14ms
- Write time complexity is O(log n) (size of subtree)

**Read performance:**
- Get a single node: 2ms
- Get all descendants for a node with 1,636 descendants: 33ms
- Get all descendants for a node with 262 descendants: 5ms
- Get all descendants for a node with 100,000 descendants: 3s
- Read time complexity is O(log n) (size of subtree)

**Memory:**
- nodes table: 100,000 rows, 3 integer columns
- closure table: 539,679 rows, 5 integer columns
- size of indices on disc: 49mb
- size of tables on disc: 32mb
- server RAM space complexity is O(1) because data is only stored in the db
- db disc space complexity is O(n*log n)

We gained a 500x read performance improvements after de-normalizing some data in the closure table, at the
expense of using two additional integer columns.