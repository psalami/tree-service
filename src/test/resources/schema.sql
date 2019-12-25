
-- Reset test db ----------------------------------------------

DROP TABLE IF EXISTS children cascade;
DROP TABLE IF EXISTS nodes cascade;

-- Table Definition ----------------------------------------------


CREATE TABLE IF NOT EXISTS nodes (
    id integer PRIMARY KEY,
    parent integer REFERENCES nodes(id),
    root integer REFERENCES nodes(id)
);

-- Indices -------------------------------------------------------

CREATE UNIQUE INDEX IF NOT EXISTS nodes_pkey ON nodes(id int4_ops);

-- Table Definition ----------------------------------------------

CREATE TABLE IF NOT EXISTS children (
    parent integer REFERENCES nodes(id),
    child integer REFERENCES nodes(id),
    depth numeric
);

-- Indices -------------------------------------------------------

CREATE INDEX IF NOT EXISTS pdc_idx ON children(parent int4_ops,depth numeric_ops,child int4_ops);
CREATE INDEX IF NOT EXISTS cpd_idx ON children(child int4_ops,parent int4_ops,depth numeric_ops);
CREATE UNIQUE INDEX IF NOT EXISTS cp_unique_idx ON children(parent int4_ops,child int4_ops);