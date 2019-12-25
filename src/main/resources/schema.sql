DROP TABLE IF EXISTS children;
DROP TABLE IF EXISTS nodes;

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
    ancestor integer REFERENCES nodes(id),
    descendant integer REFERENCES nodes(id),
    depth integer,
    parent integer REFERENCES nodes(id),
    root integer REFERENCES nodes(id)
);

-- Indices -------------------------------------------------------

CREATE UNIQUE INDEX IF NOT EXISTS cp_unique_idx ON children(ancestor int4_ops,descendant int4_ops);
CREATE INDEX IF NOT EXISTS pdc_idx ON children(ancestor int4_ops,depth int4_ops,descendant int4_ops);
CREATE INDEX IF NOT EXISTS cpd_idx ON children(descendant int4_ops,ancestor int4_ops,depth int4_ops);