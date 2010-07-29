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

create table if not exists datamap_identifiers
(
id integer primary key,
name varchar(100),
-- for root datamap nodes:
problem_space_id integer,
comment_text varchar(100)
);

create table if not exists datamap_enumerations
(
id integer primary key,
name varchar(100),
comment_text varchar(100)
);

create table if not exists datamap_enumeration_values
(
id integer primary key,
name varchar(100),
datamap_enumeration_id integer,
comment_text varchar(100)
);

create table if not exists datamap_integers
(
id integer primary key,
name varchar(100),
min_value integer,
max_value integer,
comment_text varchar(100)
);

create table if not exists datamap_floats
(
id integer primary key,
name varchar(100),
min_value float,
max_value float,
comment_text varchar(100)
);

create table if not exists datamap_strings
(
id integer primary key,
name varchar(100),
comment_text varchar(100)
);

-- Directed join tables:
create table if not exists directed_join_datamap_identifiers_datamap_identifiers
(
id integer primary key,
parent_id integer,
child_id integer
);

create table if not exists directed_join_datamap_identifiers_datamap_enumerations
(
id integer primary key,
parent_id integer,
child_id integer
);

create table if not exists directed_join_datamap_identifiers_datamap_integers
(
id integer primary key,
parent_id integer,
child_id integer
);

create table if not exists directed_join_datamap_identifiers_datamap_floats
(
id integer primary key,
parent_id integer,
child_id integer
);

create table if not exists directed_join_datamap_identifiers_datamap_strings
(
id integer primary key,
parent_id integer,
child_id integer
);

-- Undirected join tables for linked attributes:
create table if not exists join_datamap_identifiers_datamap_identifiers
(
id integer primary key,
first_id integer,
second_id integer
);

create table if not exists join_datamap_integers_datamap_integers
(
id integer primary key,
first_id integer,
second_id integer
);

create table if not exists join_datamap_floats_datamap_floats
(
id integer primary key,
first_id integer,
second_id integer
);

create table if not exists join_datamap_strings_datamap_strings
(
id integer primary key,
first_id integer,
second_id integer
);

create table if not exists join_datamap_enumerations_datamap_enumerations
(
id integer primary key,
first_id integer,
second_id integer
);
