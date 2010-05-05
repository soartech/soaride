drop table if exists agents;
create table agents
(
id integer primary key,
name varchar(100) not null
);

drop table if exists problem_spaces;
create table problem_spaces
(
id integer primary key,
agent_id int,
name varchar(100) not null
);

drop table if exists operators;
create table operators
(
id integer primary key,
agent_id int,
name varchar(100) not null
);

drop table if exists rules;
create table rules
(
id integer primary key,
agent_id integer,
name varchar(100) not null,
raw_text text
);

drop table if exists tags;
create table tags
(
id integer primary key,
name varchar(100),
agent_id integer
);

--Join tables
drop table if exists join_rules_problem_spaces;
create table join_rules_problem_spaces
(
id integer primary key,
first_id integer,
second_id integer
);

drop table if exists join_rules_operators;
create table join_rules_operators
(
id integer primary key,
first_id integer,
second_id integer
);

drop table if exists join_operators_problem_spaces;
create table join_operators_problem_spaces
(
id integer primary key,
first_id integer,
second_id integer
);

drop table if exists join_tags_problem_spaces;
create table join_tags_problem_spaces
(
id integer primary key,
first_id integer,
second_id integer
);

drop table if exists join_tags_operators;
create table join_tags_operators
(
id integer primary key,
first_id integer,
second_id integer
);

drop table if exists join_tags_rules;
create table join_tags_rules
(
id integer primary key,
first_id integer,
second_id integer
);
