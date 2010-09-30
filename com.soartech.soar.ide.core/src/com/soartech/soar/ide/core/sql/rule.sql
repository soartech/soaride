/*

LHS grammar:

The grammar that this recognizes
<ConditionSide> ::= <Condition>+
<FirstCondition> ::= '(' "state" VARIABLE <AttributeValueTest>* ')' 
<Condition> ::= ['-'] PositiveCondition
<PositiveCondition>     ::= <ConditionForOneIdentifier> | '{' <Condition>+ '}'
<ConditionForOneIdentifier> ::= '(' VARIABLE <AttributeValueTest>* ')'
<AttributeValueTest> ::= ['-'] ^ <AttributeTest> ['.'<AttributeTest>]* <ValueTest>* 
<AttributeTest> ::= <Test>
<ValueTest> ::= <Test> ['+']
<Test> ::= <ConjunctiveTest> | <SimpleTest>
<ConjunctiveTest> ::= '{' <SimpleTest>+ '}'
<SimpleTest> ::= <DisjunctionTest> | <RelationalTest>
<DisjunctionTest> ::= '<<' <Constant>+ '>>'
<RelationalTest>            ::= [<Relation>] <SingleTest>
<Relation> ::=  '<>' | '<=>' | '<' | '<=' | '>=' | '>' | '='
<SingleTest> ::= <Constant> | VARIABLE
<Constant> ::= INTEGER_CONST | SYMBOLIC_CONST | FLOATING_POINT_CONST

RHS grammar:

<ActionSide> ::= <Action>*
<Action> ::= <VarAttrValMake> | <FunctionCall>
<VarAttrValMake> ::= ( VARIABLE <AttributeValueMake>+ )
<FunctionCall> ::= ( <FunctionName> (<RHSValue>)* )
<FunctionName> ::= SYMBOLIC_CONST | + | -
  (WARNING: might need others besides +, - here if the lexer changes)
<RHSValue> ::= <Constant> | <FunctionCall> | VARIABLE
<Constant> ::= SYMBOLIC_CONST | INTEGER_CONST | FLOATING_POINT_CONST
<AttributeValueMake> ::= ^ <RHSValue> ['.'<RHSValue>]* <ValueMake>+
<ValueMake> ::= <RHSValue> <Preferences>
<Preferences> ::= <PreferenceSpecifier>*   
<PreferenceSpecifier> ::= <NaturallyUnaryPreference> [,]
                        | <ForcedUnaryPreference>
                        | <BinaryPreference> <RHSValue> [,]
<NaturallyUnaryPreference> ::= + | - | ! | ~ 
<BinaryPreference> ::= > | = | < 
<ForcedUnaryPreference> ::= <BinaryPreference> [,]   

*/

-- Items that have associated tokens get the following fields:
-- token varchar
-- begin_offset integer
-- end_offset integer

-- Triples, to cache in database instead of re-creating each time they are needed:
create table if not exists triples
(
id integer primary key,
rule_id integer,
variable_string varchar(255),
attribute_string varchar(255),
value_string varchar(255),
has_state boolean not null,
variable_offset integer,
attribute_offset integer,
value_offset integer
);

create table if not exists directed_join_triples_triples
(
id integer primary key,
parent_id integer,
child_id integer
);

--Condition side:

create table if not exists conditions
(
id integer primary key,
rule_id integer,
positive_condition_id integer,
name varchar(255) not null,
is_negated boolean not null
);

create table if not exists positive_conditions
(
id integer primary key,
condition_id integer,
is_negated boolean,
name varchar(255) not null,
is_conjunction boolean not null
);

create table if not exists condition_for_one_identifiers
(
id integer primary key,
positive_condition_id integer,
name varchar(255) not null,
has_state boolean not null,
variable varchar(255) not null,
token varchar(255),
begin_offset integer,
end_offset integer
);

create table if not exists attribute_value_tests
(
id integer primary key,
condition_for_one_identifier_id integer,
value_test_id integer,
name varchar(255) not null,
is_negated boolean not null,
token varchar(255),
begin_offset integer,
end_offset integer
);

create table if not exists attribute_tests
(
id integer primary key,
attribute_value_test_id integer,
name varchar(255) not null
);

create table if not exists value_tests
(
id integer primary key,
attribute_value_test_id integer,
attribute_test_id integer,
name varchar(255) not null,
is_structured_value_notation boolean not null,

/* for structured-value */
variable varchar(255),

/* for non-structured-value */
has_acceptable_preference boolean
);

create table if not exists tests
(
id integer primary key,
attribute_test_id integer,
value_test_id integer,
name varchar(255) not null,
is_conjunctive_test boolean not null
);

create table if not exists conjunctive_tests
(
id integer primary key,
test_id integer,
name varchar(255) not null
);

create table if not exists simple_tests
(
id integer primary key,
conjunctive_test_id integer,
test_id integer,
name varchar(255) not null,
is_disjunction_test boolean not null
);

create table if not exists disjunction_tests
(
id integer primary key,
simple_test_id integer,
name varchar(255) not null,
token varchar(255),
begin_offset integer,
end_offset integer
);

create table if not exists relational_tests
(
id integer primary key,
simple_test_id integer,
name varchar(255) not null,
relation integer not null
);

/*
 * Not used in SoarProductionAST
 * 
drop table if exists relations;
create table if not exists relations
(
id integer primary key,
relational_test_id integer,
value varchar(2),
name varchar(255) not null
);
*/

create table if not exists single_tests
(
id integer primary key,
relational_test_id integer,
/*variable_value varchar(255),*/
name varchar(255) not null,
is_constant boolean not null,
variable varchar(255),
token varchar(255),
begin_offset integer,
end_offset integer
);

create table if not exists constants
(
id integer primary key,
single_test_id integer,
disjunction_test_id integer,
rhs_value_id integer,
constant_type integer not null,
integer_const integer,
symbolic_const varchar(255),
floating_const decimal,
name varchar(255) not null,
token varchar(255),
begin_offset integer,
end_offset integer
);

--Action side:

create table if not exists actions
(
id integer primary key,
rule_id integer,
name varchar(255) not null,
is_var_attr_val_make boolean not null
);

create table if not exists var_attr_val_makes
(
id integer primary key,
action_id integer,
variable varchar(255),
name varchar(255) not null,
token varchar(255),
begin_offset integer,
end_offset integer
);

create table if not exists attribute_value_makes
(
id integer primary key,
var_attr_val_make_id integer,
name varchar(255) not null
);

create table if not exists function_calls
(
id integer primary key,
action_id integer,
rhs_value_id integer,
name varchar(255) not null,
function_name varchar(255) not null,
token varchar(255),
begin_offset integer,
end_offset integer
);

create table if not exists function_names
(
id integer primary key,
function_call_id integer,
symbolic_const varchar(255),
name varchar(255) not null
);

create table if not exists rhs_values
(
id integer primary key,
function_call_id integer,
value_make_id integer,
attribute_value_make_id integer,
name varchar(255) not null,
is_constant boolean not null,
is_variable boolean not null,
is_function_call boolean not null,
variable varchar(255),
token varchar(255),
begin_offset integer,
end_offset integer
);

/*
 * Already exists from LHS.
 * 
drop table if exists constants;
create table if not exists constants
(
id integer primary key,
rhs_value_id integer
);
*/

create table if not exists value_makes
(
id integer primary key,
attribute_value_make_id integer,
name varchar(255) not null
);

/*
drop table if exists preferences;
create table if not exists preferences
(
id integer primary key,
value_make_id integer,
name varchar(255) not null
);
*/

create table if not exists preference_specifiers
(
id integer primary key,
value_make_id integer,
name varchar(255) not null,
is_unary_preference boolean not null,
preference_specifier_type integer not null
);

create table if not exists naturally_unary_preferences
(
id integer primary key,
preference_specifier_id integer,
value varchar(1),
name varchar(255) not null
);

create table if not exists binary_preferences
(
preference_specifier_id integer,
forced_unary_preference_id integer,
value varchar(1),
name varchar(255) not null
);

create table if not exists forced_unary_preferences
(
id integer primary key,
preference_specifier_id integer,
name varchar(255) not null
);
