package com.soartech.soar.ide.ui.actions.explorer.DatabaseTraversal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class TraversalUtil {
	
	// global-ish variable, bad practice
	static SoarDatabaseRow rule = null;
	
	public static ArrayList<Triple> getTriplesForRule(SoarDatabaseRow rule) {
		Table table = rule.getTable();
		assert table == Table.RULES;
		TraversalUtil.rule = rule;
		ArrayList<Triple> triples = new ArrayList<Triple>();
		ArrayList<String> stateVariables = new ArrayList<String>();
		visitRuleNode(rule, triples, stateVariables);
		appleStateToTriples(triples, stateVariables);
		addAttributePathInformationToTriples(triples);
		
		// TODO
		// Add dummy triple pointing to root node.
		
		// TODO debug
		
		//System.out.println("Triples for rule: " + rule.getName());
		/*
		for (Triple triple : triples) {
			System.out.println(triple);
			ArrayList<ArrayList<String>> paths = triple.getAttributePathsFromState();
			if (paths != null) {
				for (ArrayList<String> path : paths) {
					System.out.print("<s>");
					for (String p : path) {
						System.out.print("." + p);
					}
					System.out.println();
				}
			}
		}
		*/
		
		return triples;
	}
	
	/**
	 * Records attribute path information for each triple from root node state <s>
	 * @param triples
	 */
	private static void addAttributePathInformationToTriples(ArrayList<Triple> triples) {
		HashMap<String, ArrayList<Triple>> triplesWithVariable = new HashMap<String, ArrayList<Triple>>();
		HashMap<String, ArrayList<Triple>> triplesWithValue = new HashMap<String, ArrayList<Triple>>();
		for (Triple triple : triples) {
			ArrayList<Triple> variableList = triplesWithVariable.get(triple.variable);
			if (variableList == null) {
				variableList = new ArrayList<Triple>();
				triplesWithVariable.put(triple.variable, variableList);
			}
			variableList.add(triple);
			
			if (triple.valueIsVariable()) {
				ArrayList<Triple> valueList = triplesWithValue.get(triple.value);
				if (valueList == null) {
					valueList = new ArrayList<Triple>();
					triplesWithValue.put(triple.value, valueList);
				}
				valueList.add(triple);			
			}
		}
		
		for (Triple triple : triples) {
			ArrayList<Triple> parentTriples = triplesWithValue.get(triple.variable);
			if (parentTriples != null) {
				triple.parentTriples = parentTriples;
			} else {
				triple.parentTriples = new ArrayList<Triple>();
			}
			if (triple.valueIsVariable()) {
				ArrayList<Triple> childTriples = triplesWithVariable.get(triple.value);
				if (childTriples != null) {
					triple.childTriples = childTriples;
				}
			}
		}
	}

	static boolean debug = false;

	private static void debug(String str) {
		if (!debug) return;
		System.out.println(str);
	}
	
	private static void appleStateToTriples(ArrayList<Triple> triples, ArrayList<String> stateVariables) {
		for (Triple triple : triples) {
			for (String stateVariable : stateVariables) {
				if (triple.variable.equals(stateVariable)) {
					triple.hasState = true;
				}
			}
		}
	}
	
	private static void visitRuleNode(SoarDatabaseRow row, ArrayList<Triple> triples, ArrayList<String> stateVariables) {
		debug("visitRuleNode");
		ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
		for (ISoarDatabaseTreeItem item : children) {
			assert item instanceof SoarDatabaseRow;
			SoarDatabaseRow child = (SoarDatabaseRow) item;
			
			if (child.getTable() == Table.CONDITION_FOR_ONE_IDENTIFIERS) {
				String variable = "" + child.getColumnValue("variable");
				boolean hasState = ((Integer) child.getColumnValue("has_state") == 1);
				if (hasState) {
					stateVariables.add(variable);
				}
				visitConditionForOneIdentifier(child, triples, variable);
			} else if (child.getTable() == Table.VAR_ATTR_VAL_MAKES) {
				String variable = "" + child.getColumnValue("variable");
				visitVarAttrValMake(child, triples, variable);
			} else {
				visitRuleNode(child, triples, stateVariables);
			}
		}
	}
	
	private static void visitConditionForOneIdentifier(SoarDatabaseRow row, ArrayList<Triple> triples, String variable) {
		debug("visitConditionForOneIdentifier");
		ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
		for (ISoarDatabaseTreeItem item : children) {
			assert item instanceof SoarDatabaseRow;
			SoarDatabaseRow child = (SoarDatabaseRow) item;
			
			if (child.getTable() == Table.ATTRIBUTE_VALUE_TESTS) {
				ArrayList<ArrayList<String>> attributes = new ArrayList<ArrayList<String>>();
				ArrayList<String> values = new ArrayList<String>();
				visitAttributeValueTest(child, attributes, values);
				ArrayList<String> variables = new ArrayList<String>();
				variables.add(variable);
				debug("Values: " + values + " (visitConditionForOneIdentifier)");
				ArrayList<Triple> newTriples = triplesForVariablesNamesAttributes(variables, attributes, values);
				triples.addAll(newTriples);
			} else {
				visitConditionForOneIdentifier(child, triples, variable);
			}
		}
	}
	
	private static ArrayList<Triple> triplesForVariablesNamesAttributesShortList(ArrayList<String> variables, ArrayList<String> attributes, ArrayList<String> values) {
		// Turn the attributes into an ArrayList<ArrayList<String>>
		ArrayList<ArrayList<String>> attributesList = new ArrayList<ArrayList<String>>();
		for (String attribute : attributes) {
			ArrayList<String> singleAttribute = new ArrayList<String>();
			singleAttribute.add(attribute);
			attributesList.add(singleAttribute);
		}
		debug("Values: " + values + " (triplesForVariablesNamesAttributesShortList)");
		if (values == null) {
			debug("NULL");
		}
		ArrayList<Triple> ret = triplesForVariablesNamesAttributes(variables, attributesList, values);
		return ret;
	}
	
	private static ArrayList<Triple> triplesForVariablesNamesAttributes(ArrayList<String> variables, ArrayList<ArrayList<String>> attributes, ArrayList<String> values) {
		debug("triplesForVariablesNamesAttributes, values: " + values);
		ArrayList<Triple> ret = new ArrayList<Triple>();
		ArrayList<String> newVariables = new ArrayList<String>();
		if (attributes.size() > 0) {
			ArrayList<String> currentAttributes = attributes.get(0);
			if (attributes.size() == 1) {
				// This is the last set of variables.
				// Create triples with the current variables, attributes, values.

				for (String variable : variables) {
					for (String attribute : currentAttributes) {
						for (String value : values) {
							Triple triple = new Triple(variable, attribute, value, rule);
							ret.add(triple);
						}
					}
				}

			} else {
				// We're in dot notation.
				// Create a new set of variables using the current attributes.
				// Create triples using the current variables, current
				// attributes, and variables created from the attributes.
				// Then add all with a recursive call to this function.

				for (String variable : variables) {
					for (String attribute : currentAttributes) {
						String variableName = variable.substring(1, variable.length() - 1);
						if (!variableName.startsWith("_")) {
							variableName = "_" + variableName;
						}
						String newVariable = "<" + variableName + "_" + attribute + ">";
						Triple triple = new Triple(variable, attribute, newVariable, rule);
						ret.add(triple);
						newVariables.add(newVariable);
					}
				}
				
				ArrayList<ArrayList<String>> newAttributes = new ArrayList<ArrayList<String>>();
				for (int i = 1; i < attributes.size(); ++i) {
					newAttributes.add(attributes.get(i));
				}
				debug("Values: " + values + " (triplesForVariablesNamesAttributes)");
				ArrayList<Triple> recurse = triplesForVariablesNamesAttributes(newVariables, newAttributes, values);
				ret.addAll(recurse);
			}
		}
		return ret;
	}
	
	private static void visitAttributeValueTest(SoarDatabaseRow row, ArrayList<ArrayList<String>> attributes, ArrayList<String> values) {
		debug("visitAttributeValueTest");
		ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
		for (ISoarDatabaseTreeItem item : children) {
			assert item instanceof SoarDatabaseRow;
			SoarDatabaseRow child = (SoarDatabaseRow) item;
			
			if (child.getTable() == Table.ATTRIBUTE_TESTS) {
				ArrayList<String> names = new ArrayList<String>();
				visitAttributeTest(child, names);
				attributes.add(names);
			} else if (child.getTable() == Table.VALUE_TESTS) {
				visitValueTest(child, values);
			} else { 
				visitAttributeValueTest(child, attributes, values);
			}
		}
	}
	
	private static void visitAttributeTest(SoarDatabaseRow row, ArrayList<String> attributes) {
		debug("visitAttributeTest");
		ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
		for (ISoarDatabaseTreeItem item : children) {
			assert item instanceof SoarDatabaseRow;
			SoarDatabaseRow child = (SoarDatabaseRow) item;
			
			if (child.getTable() == Table.SINGLE_TESTS) {
				visitSingleTest(child, attributes);
			} else if (child.getTable() == Table.DISJUNCTION_TESTS) {
				visitDisjunctionTest(child, attributes);
			} else if (child.getTable() == Table.CONJUNCTIVE_TESTS) {
				visitConjunctiveTest(child, attributes);
			} else {
				visitAttributeTest(child, attributes);
			}
		}
	}
	
	private static void visitDisjunctionTest(SoarDatabaseRow row, ArrayList<String> names) {
		debug("visitDisjunctionTest");
		ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
		for (ISoarDatabaseTreeItem item : children) {
			assert item instanceof SoarDatabaseRow;
			SoarDatabaseRow child = (SoarDatabaseRow) item;
			if (child.getTable() == Table.CONSTANTS) {
				visitConstant(child, names);
			}
		}
	}
	
	private static void visitConjunctiveTest(SoarDatabaseRow row, ArrayList<String> names) {
		debug("visitConjunctiveTest");
		ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
		for (ISoarDatabaseTreeItem item : children) {
			assert item instanceof SoarDatabaseRow;
			SoarDatabaseRow child = (SoarDatabaseRow) item;
			if (child.getTable() == Table.DISJUNCTION_TESTS) {
				visitDisjunctionTest(child, names);
			} else if (child.getTable() == Table.SINGLE_TESTS) {
				visitSingleTest(child, names);
			} else {
				visitConjunctiveTest(child, names);
			}
		}
	}
	
	
	private static void visitValueTest(SoarDatabaseRow row, ArrayList<String> values) {
		debug("visitValueTest");
		ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
		for (ISoarDatabaseTreeItem item : children) {
			assert item instanceof SoarDatabaseRow;
			SoarDatabaseRow child = (SoarDatabaseRow) item;
			
			if (child.getTable() == Table.SINGLE_TESTS) {
				visitSingleTest(child, values);
			} else if (child.getTable() == Table.CONSTANTS) {
				visitConstant(child, values);
			} else {
				visitValueTest(child, values);
			}
		}
	}
	
	private static void visitSingleTest(SoarDatabaseRow row, ArrayList<String> names) {
		debug("visitSingleTest");
		boolean isConstant = ((Integer) row.getColumnValue("is_constant") == 1);
		if (isConstant) {
			ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
			for (ISoarDatabaseTreeItem item : children) {
				assert item instanceof SoarDatabaseRow;
				SoarDatabaseRow child = (SoarDatabaseRow) item;
				if (child.getTable() == Table.CONSTANTS) {
					visitConstant(child, names);
				}
			}
		} else {
			String variable = "" + row.getColumnValue("variable");
			names.add(variable);
		}
	}
	
	private static void visitConstant(SoarDatabaseRow row, ArrayList<String> names) {
		debug("visitConstant");
		Object name = row.getColumnValue("symbolic_const");
		names.add("" + name);
	}
	
	private static void visitVarAttrValMake(SoarDatabaseRow row, ArrayList<Triple> triples, String variable) {
		debug("visitVarAttrValMake");
		ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
		for (ISoarDatabaseTreeItem item : children) {
			assert item instanceof SoarDatabaseRow;
			SoarDatabaseRow child = (SoarDatabaseRow) item;
			if (child.getTable() == Table.ATTRIBUTE_VALUE_MAKES) {
				ArrayList<String> attributes = new ArrayList<String>();
				ArrayList<String> values = new ArrayList<String>();
				visitAttributeValueMake(child, attributes, values);
				ArrayList<String> variables = new ArrayList<String>();
				variables.add(variable);
				ArrayList<Triple> newTriples = triplesForVariablesNamesAttributesShortList(variables, attributes, values);
				triples.addAll(newTriples);
			} else {
				visitVarAttrValMake(child, triples, variable);
			}
		}
	}

	private static void visitAttributeValueMake(SoarDatabaseRow row, ArrayList<String> attributes, ArrayList<String> values) {
		debug("visitAttributeValueMake, values: " + values);
		ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
		for (ISoarDatabaseTreeItem item : children) {
			assert item instanceof SoarDatabaseRow;
			SoarDatabaseRow child = (SoarDatabaseRow) item;
			if (child.getTable() == Table.RHS_VALUES) {
				boolean isVariable = ((Integer) child.getColumnValue("is_variable") == 1);
				if (isVariable) {
					String variable = "" + child.getColumnValue("variable");
					attributes.add(variable);
				} else {
					visitRHSValue(child, attributes);
				}
			} else if (child.getTable() == Table.VALUE_MAKES) {
				visitValueMake(child, values);
			} else {
				visitAttributeValueMake(child, attributes, values);
			}
		}
	}
	
	private static void visitRHSValue(SoarDatabaseRow row, ArrayList<String> names) {
		debug("visitRHSValue");
		assert row.getTable() == Table.RHS_VALUES;
		Object objIsVariable = row.getColumnValue("is_variable");
		if (((Integer) objIsVariable) == 1) {
			String name = "" + row.getColumnValue("variable");
			names.add(name);
		}
		if (((Integer) row.getColumnValue("is_constant")) == 1) {
			ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
			for (ISoarDatabaseTreeItem item : children) {
				assert item instanceof SoarDatabaseRow;
				SoarDatabaseRow child = (SoarDatabaseRow) item;
				if (child.getTable() == Table.CONSTANTS) {
					String[] constTypes = {"integer_const", "floating_const", "symbolic_const"};
					Object value = null;
					for (String constType : constTypes) {
						value = child.getColumnValue(constType);
						if (value != null) {
							break;
						}
					}
					String name = "" + value;
					names.add(name);
				}
			}	
		}
	}
	
	private static void visitValueMake(SoarDatabaseRow row, ArrayList<String> values) {
		debug("visitValueMake, values: " + values);
		ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
		for (ISoarDatabaseTreeItem item : children) {
			assert item instanceof SoarDatabaseRow;
			SoarDatabaseRow child = (SoarDatabaseRow) item;
			if (child.getTable() == Table.RHS_VALUES) {
				visitRHSValue(child, values);
			} else {
				visitValueMake(child, values);
			}
		}
	}
	
	/**
	 * Finds rules associated with the given row.
	 * @param row
	 * @return
	 */
	public static ArrayList<ISoarDatabaseTreeItem> getRelatedRules(SoarDatabaseRow row) {
		ArrayList<ISoarDatabaseTreeItem> ret = new ArrayList<ISoarDatabaseTreeItem>();
		
		Table table = row.getTable();
		if (table == Table.AGENTS) {
			ret.addAll(row.getChildrenOfType(Table.RULES));
		}
		else if (table == Table.PROBLEM_SPACES) {
			ret.addAll(row.getJoinedRowsFromTable(Table.RULES));
			
			ArrayList<ISoarDatabaseTreeItem> operators = row.getJoinedRowsFromTable(Table.OPERATORS);
			for (ISoarDatabaseTreeItem item : operators) {
				if (item instanceof SoarDatabaseRow) {
					SoarDatabaseRow operator = (SoarDatabaseRow) item;
					ret.addAll(operator.getJoinedRowsFromTable(Table.RULES));
				}
			}
		}
		else if (table == Table.OPERATORS) {
			ret.addAll(row.getJoinedRowsFromTable(Table.RULES));
		}
		else if (table == Table.RULES) {
			ret.add(row);
		}
		
		return ret;
	}

	public static HashMap<String, SoarDatabaseRow> getProblemSpacesMap(SoarDatabaseRow agent) {
		return getSoarDatabaseRowMap(agent, Table.PROBLEM_SPACES);
	}
	
	public static HashMap<String, SoarDatabaseRow> getOperatorsMap(SoarDatabaseRow agent) {
		return getSoarDatabaseRowMap(agent, Table.OPERATORS);
	}
	
	private static HashMap<String, SoarDatabaseRow> getSoarDatabaseRowMap(SoarDatabaseRow agent, Table table) {
		assert agent.getTable() == Table.AGENTS;
		HashMap<String, SoarDatabaseRow> ret = new HashMap<String, SoarDatabaseRow>();
		ArrayList<ISoarDatabaseTreeItem> problemSpaces = agent.getChildrenOfType(table);
		for (ISoarDatabaseTreeItem item : problemSpaces) {
			assert item instanceof SoarDatabaseRow;
			SoarDatabaseRow row = (SoarDatabaseRow) item;
			assert row.getTable() == table;
			ret.put(row.getName(), row);
		}
		return ret;
	}
	
	/**
	 * <p>e.g. Start with:</p>
	 * <code>
	 * (state &lt;s&gt; ^name substate ^superstate &lt;ss&gt;)<br />
	 * (&lt;ss&gt; ^name superstate)
	 * </code>
	 * <p>End up with:</p>
	 * <code>
	 * (state &lt;ss&gt; ^name superstate)
	 * </code>
	 */
	public static ArrayList<Triple> getTriplesForSuperstate(ArrayList<Triple> triples) {
		ArrayList<Triple> ret = new ArrayList<Triple>();
		
		// Find all superstate variables.
		HashSet<String> superstateVariables = new HashSet<String>();
		for (Triple triple : triples) {
			if (triple.hasState) {
				if (triple.valueIsVariable()) {
					superstateVariables.add(triple.value);
				}
			}
		}
		
		// Find superstate triples
		for (Triple triple : triples) {
			if (superstateVariables.contains(triple.variable)) {
				Triple newTriple = new Triple(triple.variable, triple.attribute, triple.value, triple.rule);
				newTriple.hasState = true;
				ret.add(newTriple);
			}
		}
		
		return ret;
	}
}
