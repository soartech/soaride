drop table if exists datamaps;
create table datamaps
(
id integer primary key,
substate_id integer
);

drop table if exists datamap_nodes;
create table datamap_nodes
(
id integer primary key,
datamap_id integer
);

drop table if exists datamap_attributes;
create table datamap_attributes
(
id integer primary key,
source_datamap_node_id integer,
target_datamap_node_id integer,
name varchar(100)
);

drop table if exists datamap_values;
create table datamap_values
(
id integer primary key,
datamap_node_id integer,
value varchar(100)
);
