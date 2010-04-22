/*

A datamap has two types of items: Attributes and values.

Each attribute has a name.

Each value has a name.

Each problem space has a root datamap attribute.

Each attribute has a collection of child attributes.

Each attribute has a collection of child values.

Each value is of type Enumeration, Integer, Float, or String.

If a value is of type Enumeration, Integer, or Float, parameters may be set to restrict valid values (such as numeric range or enumeration values).

If a value is of type String, any value is valid.

Implementation:

The child attributes of an atribute are directed-joined children.

The child values of an attribute are directed-joined children.

Each type of value gets its own database table.

 */

drop table if exists datamap_attributes;
create table datamap_attributes
(
id integer primary key,
name varchar(100),
-- for root datamap nodes:
problem_space_id integer
);

drop table if exists datamap_enumeration_values;
create table datamap_enumeration_values
(
id integer primary key,
name varchar(100),
datamap_attribute_id integer
);

drop table if exists datamap_integer_values;
create table datamap_integer_values
(
id integer primary key,
name varchar(100),
datamap_attribute_id integer,
min_value integer,
max_value integer
);

drop table if exists datamap_float_values;
create table datamap_float_values
(
id integer primary key,
name varchar(100),
datamap_attribute_id integer,
min_value float,
max_value float
);

drop table if exists datamap_string_values;
create table datamap_string_values
(
id integer primary key,
name varchar(100),
datamap_attribute_id integer
);

-- Directed join tables:
drop table if exists directed_join_datamap_attributes_datamap_attributes;
create table directed_join_datamap_attributes_datamap_attributes
(
id integer primary key,
parent_id integer,
child_id integer
);

drop table if exists directed_join_datamap_attributes_datamap_enumeration_values;
create table directed_join_datamap_attributes_datamap_enumeration_values
(
id integer primary key,
parent_id integer,
child_id integer
);

drop table if exists directed_join_datamap_attributes_datamap_integer_values;
create table directed_join_datamap_attributes_datamap_integer_values
(
id integer primary key,
parent_id integer,
child_id integer
);

drop table if exists directed_join_datamap_attributes_datamap_float_values;
create table directed_join_datamap_attributes_datamap_float_values
(
id integer primary key,
parent_id integer,
child_id integer
);

drop table if exists directed_join_datamap_attributes_datamap_string_values;
create table directed_join_datamap_attributes_datamap_string_values
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
