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

--Condition side:

drop table if exists conditions;
create table conditions
(
id integer primary key,
rule_id integer,
positive_condition_id integer,
name varchar(100) not null,
is_negated boolean not null
);

drop table if exists positive_conditions;
create table positive_conditions
(
id integer primary key,
condition_id integer,
negated boolean,
name varchar(100) not null,
is_conjunction boolean not null
);

drop table if exists condition_for_one_identifiers;
create table condition_for_one_identifiers
(
id integer primary key,
positive_condition_id integer,
name varchar(100) not null,
has_state boolean not null,
variable varchar(100) not null
);

drop table if exists attribute_value_tests;
create table attribute_value_tests
(
id integer primary key,
condition_for_one_identifier_id integer,
name varchar(100) not null,
is_negated boolean not null
);

drop table if exists attribute_tests;
create table attribute_tests
(
id integer primary key,
attribute_value_test_id integer,
name varchar(100) not null
);

drop table if exists value_tests;
create table value_tests
(
id integer primary key,
attribute_value_test_id integer,
attribute_test_id integer,
name varchar(100) not null,
has_acceptable_preference boolean not null
);

drop table if exists tests;
create table tests
(
id integer primary key,
attribute_test_id integer,
value_test_id integer,
name varchar(100) not null,
is_conjunctive_test boolean not null
);

drop table if exists conjunctive_tests;
create table conjunctive_tests
(
id integer primary key,
test_id integer,
name varchar(100) not null
);

drop table if exists simple_tests;
create table simple_tests
(
id integer primary key,
conjunctive_test_id integer,
test_id integer,
name varchar(100) not null,
is_disjunction_test boolean not null
);

drop table if exists disjunction_tests;
create table disjunction_tests
(
id integer primary key,
simple_test_id integer,
name varchar(100) not null
);

drop table if exists relational_tests;
create table relational_tests
(
id integer primary key,
simple_test_id integer,
name varchar(100) not null,
relation integer not null
);

/*
 * Not used in SoarProductionAST
 * 
drop table if exists relations;
create table relations
(
id integer primary key,
relational_test_id integer,
value varchar(2),
name varchar(100) not null
);
*/

drop table if exists single_tests;
create table single_tests
(
id integer primary key,
relational_test_id integer,
/*variable_value varchar(100),*/
name varchar(100) not null,
is_constant boolean not null,
variable varchar(100)
);

drop table if exists constants;
create table constants
(
id integer primary key,
single_test_id integer,
disjunction_test_id integer,
rhs_value_id integer,
constant_type integer not null,
integer_const integer,
symbolic_const varchar(100),
floating_const decimal,
name varchar(100) not null
);

--Action side:

drop table if exists actions;
create table actions
(
id integer primary key,
rule_id integer,
name varchar(100) not null,
is_var_attr_val_make boolean not null
);

drop table if exists var_attr_val_makes;
create table var_attr_val_makes
(
id integer primary key,
action_id integer,
variable varchar(100),
name varchar(100) not null
);

drop table if exists attribute_value_makes;
create table attribute_value_makes
(
id integer primary key,
var_attr_val_make_id integer,
name varchar(100) not null
);

drop table if exists function_calls;
create table function_calls
(
id integer primary key,
action_id integer,
rhs_value_id integer,
name varchar(100) not null,
function_name varchar(100) not null
);

drop table if exists function_names;
create table function_names
(
id integer primary key,
function_call_id integer,
symbolic_const varchar(100),
name varchar(100) not null
);

drop table if exists rhs_values;
create table rhs_values
(
id integer primary key,
function_call_id integer,
value_make_id integer,
attribute_value_make_id integer,
name varchar(100) not null,
is_constant boolean not null,
is_variable boolean not null,
is_function_call boolean not null,
variable varchar(100)
);

/*
 * Already exists from LHS.
 * 
drop table if exists constants;
create table constants
(
id integer primary key,
rhs_value_id integer
);
*/

drop table if exists value_makes;
create table value_makes
(
id integer primary key,
attribute_value_make_id integer,
name varchar(100) not null
);

/*
drop table if exists preferences;
create table preferences
(
id integer primary key,
value_make_id integer,
name varchar(100) not null
);
*/

drop table if exists preference_specifiers;
create table preference_specifiers
(
id integer primary key,
value_make_id integer,
name varchar(100) not null,
is_unary_preference boolean not null,
preference_specifier_type integer not null
);

drop table if exists naturally_unary_preferences;
create table naturally_unary_preferences
(
id integer primary key,
preference_specifier_id integer,
value varchar(1),
name varchar(100) not null
);

drop table if exists binary_preferences;
create table binary_preferences
(
preference_specifier_id integer,
forced_unary_preference_id integer,
value varchar(1),
name varchar(100) not null
);

drop table if exists forced_unary_preferences;
create table forced_unary_preferences
(
id integer primary key,
preference_specifier_id integer,
name varchar(100) not null
);