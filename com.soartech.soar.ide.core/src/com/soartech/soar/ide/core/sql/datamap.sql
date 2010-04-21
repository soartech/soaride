drop table if exists datamap_attribute_types;
create table datamap_attribute_types
(
id integer primary key,
name varchar(100)
);

drop table if exists datamap_attributes;
create table datamap_attributes
(
id integer primary key,
name varchar(100),
datamap_attribute_type_id integer,
-- for root datamap nodes:
problem_space_id integer
);

-- Directed join table:
drop table if exists directed_join_datamap_attributes_datamap_attributes;
create table directed_join_datamap_attributes_datamap_attributes
(
id integer primary key,
parent_id integer,
child_id integer
);

-- Undirected join table for linked attributes:
drop table if exists join_datamap_attributes_datamap_attributes;
create table join_datamap_attributes_datamap_attributes
(
id integer primary key,
first_id integer,
second_id integer
);
