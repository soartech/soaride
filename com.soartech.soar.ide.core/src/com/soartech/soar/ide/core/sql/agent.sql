create table if not exists agents
(
id integer primary key,
name varchar(100) not null,
raw_text text
);

create table if not exists problem_spaces
(
id integer primary key,
agent_id int,
name varchar(100) not null,
-- whether this is a root problem space on its agent
-- 1 for true, 0 for false
is_root int
);

create table if not exists operators
(
id integer primary key,
agent_id int,
name varchar(100) not null
);

create table if not exists rules
(
id integer primary key,
agent_id integer,
name varchar(100) not null,
raw_text text,
ast blob
);

create table if not exists tags
(
id integer primary key,
name varchar(100),
agent_id integer
);

-- Join tables

-- Directed for project hierarchy

create table if not exists directed_join_problem_spaces_problem_spaces
(
id integer primary key,
parent_id integer,
child_id integer,
join_type integer -- What kind of impasse this is
);

create table if not exists directed_join_problem_spaces_rules
(
id integer primary key,
parent_id integer,
child_id integer
);

create table if not exists directed_join_operators_rules
(
id integer primary key,
parent_id integer,
child_id integer
);

create table if not exists directed_join_problem_spaces_operators
(
id integer primary key,
parent_id integer,
child_id integer
);

create table if not exists directed_join_operators_problem_spaces
(
id integer primary key,
parent_id integer,
child_id integer,
join_type integer -- What kind of impasse this is
);

-- Undirected for tags

create table if not exists join_tags_problem_spaces
(
id integer primary key,
first_id integer,
second_id integer
);

create table if not exists join_tags_operators
(
id integer primary key,
first_id integer,
second_id integer
);

create table if not exists join_tags_rules
(
id integer primary key,
first_id integer,
second_id integer
);

