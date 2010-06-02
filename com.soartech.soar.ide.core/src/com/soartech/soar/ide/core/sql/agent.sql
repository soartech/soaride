create table if not exists agents
(
id integer primary key,
name varchar(100) not null
);

create table if not exists problem_spaces
(
id integer primary key,
agent_id int,
name varchar(100) not null
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
raw_text text
);

create table if not exists tags
(
id integer primary key,
name varchar(100),
agent_id integer
);

--Join tables
create table if not exists join_rules_problem_spaces
(
id integer primary key,
first_id integer,
second_id integer
);

create table if not exists join_rules_operators
(
id integer primary key,
first_id integer,
second_id integer
);

create table if not exists join_operators_problem_spaces
(
id integer primary key,
first_id integer,
second_id integer
);

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
