# Tree Service

## Contents
- [Introduction](#Introduction)
- [Installation](#Installation)
- [Usage](#Usage)
- [Testing](#Testing)

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
    - very fast subtree modification
    - low memory footprint
- Nested Sets
    - more complex to implement
    - very fast subtree retrieval but slower subtree modification
    - lower memory footprint
    - can be implemented with any SQL database
- Materialized Path (Closure Table)
    - relatively straightforward to implement
    - very fast subtree retrieval (as fast as nested sets)
    - fast subtree modification
    - very fast insertions
    - higher memory footprint
    - can be implemented with any SQL database
- Database-specific approaches (i.e. Postgres ltree, Neo4J, recursive CTEs, etc)
    - locks into a specific database vendor
    - may require additional infrastructure / maintenance
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
$ curl -I http://localhost:8084/api/v1/moveNode/4/6
HTTP/1.1 508
You may not move a node to one of its descendants
```

If we try to move a node to a node that does not exist, we get an HTTP 404 (Not Found) error:
```
$ curl http://localhost:8084/api/v1/moveNode/4/99
HTTP/1.1 404
The specified node 99 does not exist
```

## Testing
To run unit tests again:
```
$ docker-compose run test
```