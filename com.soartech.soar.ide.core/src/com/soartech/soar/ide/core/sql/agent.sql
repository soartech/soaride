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
