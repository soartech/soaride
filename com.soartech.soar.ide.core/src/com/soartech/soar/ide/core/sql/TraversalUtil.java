package com.soartech.soar.ide.core.sql;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import com.soartech.soar.ide.core.ast.Pair;
import com.soartech.soar.ide.core.ast.ValueTest;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class TraversalUtil {

	// global-ish variable, bad practice
	static SoarDatabaseRow rule = null;

	static long visitingRuleNodes;
	static long applyingState;
	static long addingAttributePathInfo;

	public static void resetLoggingTimes() {
		visitingRuleNodes = 0;
		applyingState = 0;
		addingAttributePathInfo = 0;
	}

	public static void printLoggingTimes() {
		System.out.println("Time Spent visiting rules nodes: " + visitingRuleNodes);
		System.out.println("Time Spent applying state: " + applyingState);
		System.out.println("Time Spent adding attribute path info: " + addingAttributePathInfo);
	}

	/**
	 * Helper method for making a Pair out of a String and a SoarDatabaseRow
	 */
	private static Pair makePair(String s, SoarDatabaseRow r) {
		Pair token = r.getToken();
		if (token != null) {
			return new Pair(s, token.getOffset(), token.getEndOffset());
		}
		return new Pair(s);
	}

	public static ArrayList<Triple> getTriplesForRule(SoarDatabaseRow rule) {
		ArrayList<SoarDatabaseRow> childTriples = rule.getChildrenOfType(Table.TRIPLES);
		ArrayList<Triple> ret = new ArrayList<Triple>();
		for (SoarDatabaseRow childTriple : childTriples) {
			Triple triple = Triple.tripleForRow(childTriple);
			ret.add(triple);
		}
		addAttributePathInformationToTriples(ret);
		return ret;
	}

	public static HashMap<String, ArrayList<Triple>> triplesWithVariable(ArrayList<Triple> triples) {
		HashMap<String, ArrayList<Triple>> triplesWithVariable = new HashMap<String, ArrayList<Triple>>();
		for (Triple triple : triples) {
			ArrayList<Triple> variableList = triplesWithVariable.get(triple.variable);
			if (variableList == null) {
				variableList = new ArrayList<Triple>();
				triplesWithVariable.put(triple.variable, variableList);
			}
			variableList.add(triple);
		}
		return triplesWithVariable;
	}

	public static HashMap<String, ArrayList<Triple>> triplesWithValue(ArrayList<Triple> triples) {
		HashMap<String, ArrayList<Triple>> triplesWithValue = new HashMap<String, ArrayList<Triple>>();
		for (Triple triple : triples) {
			if (triple.valueIsVariable()) {
				ArrayList<Triple> valueList = triplesWithValue.get(triple.value);
				if (valueList == null) {
					valueList = new ArrayList<Triple>();
					triplesWithValue.put(triple.value, valueList);
				}
				valueList.add(triple);
			}
		}
		return triplesWithValue;
	}

	public static ArrayList<Triple> buildTriplesForRule(SoarDatabaseRow rule) {
		Table table = rule.getTable();
		assert table == Table.RULES;
		TraversalUtil.rule = rule;
		ArrayList<Triple> triples = new ArrayList<Triple>();
		ArrayList<String> stateVariables = new ArrayList<String>();
		visitingRuleNodes -= new Date().getTime();
		visitRuleNode(rule, triples, stateVariables);
		visitingRuleNodes += new Date().getTime();
		applyingState -= new Date().getTime();
		applyStateToTriples(triples, stateVariables);
		applyingState += new Date().getTime();
		addingAttributePathInfo -= new Date().getTime();
		addAttributePathInformationToTriples(triples);
		addingAttributePathInfo += new Date().getTime();

		// TODO
		// Add dummy triple pointing to root node,
		// so things like this will link correctly:
		// (state <s> ^attr.attr <val>)
		// (<val> ^attr <s>)

		// for debugging:

		// System.out.println("Triples for rule: " + rule.getName());
		/*
		 * for (Triple triple : triples) { System.out.println(triple);
		 * ArrayList<ArrayList<String>> paths =
		 * triple.getAttributePathsFromState(); if (paths != null) { for
		 * (ArrayList<String> path : paths) { System.out.print("<s>"); for
		 * (String p : path) { System.out.print("." + p); }
		 * System.out.println(); } } }
		 */

		return triples;
	}

	/**
	 * Records attribute path information for each triple from root node state
	 * <s>
	 * 
	 * @param triples
	 */
	private static void addAttributePathInformationToTriples(ArrayList<Triple> triples) {
		HashMap<String, ArrayList<Triple>> triplesWithVariable = triplesWithVariable(triples);
		HashMap<String, ArrayList<Triple>> triplesWithValue = triplesWithValue(triples);

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
		if (!debug)
			return;
		System.out.println(str);
	}

	private static void applyStateToTriples(ArrayList<Triple> triples, ArrayList<String> stateVariables) {
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
		int childIndex = 0;
		for (ISoarDatabaseTreeItem item : children) {
			++childIndex;
			SoarDatabaseRow child = (SoarDatabaseRow) item;

			if (child.getTable() == Table.CONDITION_FOR_ONE_IDENTIFIERS) {
				String variable = "" + child.getColumnValue("variable");
				boolean hasState = ((Integer) child.getColumnValue("has_state") == 1);
				if (hasState) {
					stateVariables.add(variable);
				}
				Pair variablePair = makePair(variable, child);
				visitConditionForOneIdentifier(child, triples, variablePair);
			} else if (child.getTable() == Table.VAR_ATTR_VAL_MAKES) {
				String variable = "" + child.getColumnValue("variable");
				Pair variablePair = makePair(variable, child);
				visitVarAttrValMake(child, triples, variablePair);
			} else {
				visitRuleNode(child, triples, stateVariables);
			}
		}
	}

	private static void visitConditionForOneIdentifier(SoarDatabaseRow row, ArrayList<Triple> triples, Pair variable) {
		debug("visitConditionForOneIdentifier");
		ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
		for (ISoarDatabaseTreeItem item : children) {
			assert item instanceof SoarDatabaseRow;
			SoarDatabaseRow child = (SoarDatabaseRow) item;

			if (child.getTable() == Table.ATTRIBUTE_VALUE_TESTS) {
				ArrayList<ArrayList<Pair>> attributes = new ArrayList<ArrayList<Pair>>();
				ArrayList<Pair> values = new ArrayList<Pair>();
				visitAttributeValueTest(child, attributes, values, triples, variable);
				ArrayList<Pair> variables = new ArrayList<Pair>();
				variables.add(variable);
				debug("Values: " + values + " (visitConditionForOneIdentifier)");
				ArrayList<Triple> newTriples = triplesForVariablesNamesAttributes(variables, attributes, values);
				triples.addAll(newTriples);
			} else {
				visitConditionForOneIdentifier(child, triples, variable);
			}
		}
	}

	private static ArrayList<Triple> triplesForVariablesNamesAttributesShortList(ArrayList<Pair> variables, ArrayList<Pair> attributes, ArrayList<Pair> values) {
		// Turn the attributes into an ArrayList<ArrayList<String>>
		ArrayList<ArrayList<Pair>> attributesList = new ArrayList<ArrayList<Pair>>();
		for (Pair attribute : attributes) {
			ArrayList<Pair> singleAttribute = new ArrayList<Pair>();
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

	private static ArrayList<Triple> triplesForVariablesNamesAttributes(ArrayList<Pair> variables, ArrayList<ArrayList<Pair>> attributes, ArrayList<Pair> values) {
		debug("triplesForVariablesNamesAttributes, values: " + values);
		ArrayList<Triple> ret = new ArrayList<Triple>();
		ArrayList<Pair> newVariables = new ArrayList<Pair>();
		if (attributes.size() > 0) {
			ArrayList<Pair> currentAttributes = attributes.get(0);
			if (attributes.size() == 1) {
				// This is the last set of variables.
				// Create triples with the current variables, attributes,
				// values.

				for (Pair variable : variables) {
					for (Pair attribute : currentAttributes) {
						for (Pair value : values) {
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

				for (Pair variable : variables) {
					for (Pair attribute : currentAttributes) {
						String variableName = variable.getString().substring(1, variable.getString().length() - 1);
						if (!variableName.startsWith("_")) {
							variableName = "_" + variableName;
						}
						Pair newVariable = new Pair("<" + variableName + "_" + attribute + ">");
						Triple triple = new Triple(variable, attribute, newVariable, rule);
						ret.add(triple);
						newVariables.add(newVariable);
					}
				}

				ArrayList<ArrayList<Pair>> newAttributes = new ArrayList<ArrayList<Pair>>();
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

	private static void visitAttributeValueTest(SoarDatabaseRow row, ArrayList<ArrayList<Pair>> attributes, ArrayList<Pair> values, ArrayList<Triple> triples, Pair variable) {
		debug("visitAttributeValueTest");
		ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
		for (ISoarDatabaseTreeItem item : children) {
			assert item instanceof SoarDatabaseRow;
			SoarDatabaseRow child = (SoarDatabaseRow) item;

			if (child.getTable() == Table.ATTRIBUTE_TESTS) {
				ArrayList<Pair> names = new ArrayList<Pair>();
				visitAttributeTest(child, names);
				attributes.add(names);
			} else if (child.getTable() == Table.VALUE_TESTS) {
				visitValueTest(child, attributes, values, triples, variable);
			} else {
				visitAttributeValueTest(child, attributes, values, triples, variable);
			}
		}
	}

	private static void visitAttributeTest(SoarDatabaseRow row, ArrayList<Pair> attributes) {
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

	private static void visitDisjunctionTest(SoarDatabaseRow row, ArrayList<Pair> names) {
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

	private static void visitConjunctiveTest(SoarDatabaseRow row, ArrayList<Pair> names) {
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

	private static void visitValueTest(SoarDatabaseRow row, ArrayList<ArrayList<Pair>> attributes, ArrayList<Pair> values, ArrayList<Triple> triples, Pair variable) {
		debug("visitValueTest");
		ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
		for (ISoarDatabaseTreeItem item : children) {
			assert item instanceof SoarDatabaseRow;
			SoarDatabaseRow child = (SoarDatabaseRow) item;

			if (child.getTable() == Table.SINGLE_TESTS) {
				visitSingleTest(child, values);
			} else if (child.getTable() == Table.CONSTANTS) {
				visitConstant(child, values);
			} else if (child.getTable() == Table.ATTRIBUTE_VALUE_TESTS) {
				// This happens when you have structured-value notation
				Object svVariableObj = row.getColumnValue("variable");
				String svVariable = null;
				if (svVariableObj != null) {
					svVariable = "" + svVariableObj;
				} else {
					// make fake variable for structured-value expression
					String variableName = variable.getString().substring(1, variable.getString().length() - 1);
					if (!variableName.startsWith("_"))
						variableName = "_" + variableName;
					svVariable = "<" + variableName + "_" + attributes + ">";
				}
				Pair svVariablePair = makePair(svVariable, row);
				values.add(svVariablePair);
				ArrayList<ArrayList<Pair>> svAttributes = new ArrayList<ArrayList<Pair>>();
				ArrayList<Pair> svValues = new ArrayList<Pair>();
				visitAttributeValueTest(child, svAttributes, svValues, triples, svVariablePair);
				ArrayList<Pair> svVariables = new ArrayList<Pair>();
				svVariables.add(svVariablePair);
				debug("Values: " + values + " (visitValueTest, structured value)");
				ArrayList<Triple> newTriples = triplesForVariablesNamesAttributes(svVariables, svAttributes, svValues);
				triples.addAll(newTriples);
			} else {
				visitValueTest(child, attributes, values, triples, variable);
			}
		}
	}

	private static void visitSingleTest(SoarDatabaseRow row, ArrayList<Pair> names) {
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
			names.add(makePair(variable, row));
		}
	}

	private static void visitConstant(SoarDatabaseRow row, ArrayList<Pair> names) {
		debug("visitConstant");
		String name = "" + row.getColumnValue("symbolic_const");
		names.add(makePair(name, row));
	}

	private static void visitVarAttrValMake(SoarDatabaseRow row, ArrayList<Triple> triples, Pair variable) {
		debug("visitVarAttrValMake");
		ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
		for (ISoarDatabaseTreeItem item : children) {
			assert item instanceof SoarDatabaseRow;
			SoarDatabaseRow child = (SoarDatabaseRow) item;
			if (child.getTable() == Table.ATTRIBUTE_VALUE_MAKES) {
				ArrayList<Pair> attributes = new ArrayList<Pair>();
				ArrayList<Pair> values = new ArrayList<Pair>();
				visitAttributeValueMake(child, attributes, values);
				ArrayList<Pair> variables = new ArrayList<Pair>();
				variables.add(variable);
				ArrayList<Triple> newTriples = triplesForVariablesNamesAttributesShortList(variables, attributes, values);
				triples.addAll(newTriples);
			} else {
				visitVarAttrValMake(child, triples, variable);
			}
		}
	}

	private static void visitAttributeValueMake(SoarDatabaseRow row, ArrayList<Pair> attributes, ArrayList<Pair> values) {
		debug("visitAttributeValueMake, values: " + values);
		ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
		for (ISoarDatabaseTreeItem item : children) {
			assert item instanceof SoarDatabaseRow;
			SoarDatabaseRow child = (SoarDatabaseRow) item;
			if (child.getTable() == Table.RHS_VALUES) {
				boolean isVariable = ((Integer) child.getColumnValue("is_variable") == 1);
				if (isVariable) {
					String variable = "" + child.getColumnValue("variable");
					attributes.add(makePair(variable, child));
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

	private static void visitRHSValue(SoarDatabaseRow row, ArrayList<Pair> names) {
		debug("visitRHSValue");
		assert row.getTable() == Table.RHS_VALUES;
		Object objIsVariable = row.getColumnValue("is_variable");
		if (((Integer) objIsVariable) == 1) {
			String name = "" + row.getColumnValue("variable");
			names.add(makePair(name, row));
		}
		if (((Integer) row.getColumnValue("is_constant")) == 1) {
			ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
			for (ISoarDatabaseTreeItem item : children) {
				assert item instanceof SoarDatabaseRow;
				SoarDatabaseRow child = (SoarDatabaseRow) item;
				if (child.getTable() == Table.CONSTANTS) {
					String[] constTypes = { "integer_const", "floating_const", "symbolic_const" };
					Object value = null;
					for (String constType : constTypes) {
						value = child.getColumnValue(constType);
						if (value != null) {
							break;
						}
					}
					String name = "" + value;
					names.add(makePair(name, child));
				}
			}
		}
	}

	private static void visitValueMake(SoarDatabaseRow row, ArrayList<Pair> values) {
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
	 * 
	 * @param row
	 * @return
	 */
	public static ArrayList<ISoarDatabaseTreeItem> getRelatedRules(SoarDatabaseRow row) {
		ArrayList<ISoarDatabaseTreeItem> ret = new ArrayList<ISoarDatabaseTreeItem>();

		Table table = row.getTable();
		if (table == Table.AGENTS) {
			ret.addAll(row.getChildrenOfType(Table.RULES));
		} else if (table == Table.PROBLEM_SPACES) {
			ret.addAll(row.getJoinedRowsFromTable(Table.RULES));

			ArrayList<SoarDatabaseRow> operators = row.getJoinedRowsFromTable(Table.OPERATORS);
			for (SoarDatabaseRow operator : operators) {
				ret.addAll(operator.getJoinedRowsFromTable(Table.RULES));
			}
		} else if (table == Table.OPERATORS) {
			ret.addAll(row.getJoinedRowsFromTable(Table.RULES));
		} else if (table == Table.RULES) {
			ret.add(row);
		}

		return ret;
	}

	/**
	 * Gets problem spaces related to the given row. For operators, those are
	 * parent problem spaces. Fo rules, those are parent problem spaces and
	 * problem spaces related to parent operators.
	 * 
	 * @param row
	 * @return
	 */
	public static ArrayList<ISoarDatabaseTreeItem> getRelatedProblemSpaces(SoarDatabaseRow row) {
		ArrayList<ISoarDatabaseTreeItem> ret = new ArrayList<ISoarDatabaseTreeItem>();

		Table table = row.getTable();
		if (table == Table.OPERATORS) {
			ret.addAll(row.getDirectedJoinedParentsOfType(Table.PROBLEM_SPACES));
		} else if (table == Table.RULES) {
			ret.addAll(row.getDirectedJoinedParentsOfType(Table.PROBLEM_SPACES));
			for (ISoarDatabaseTreeItem opItem : row.getDirectedJoinedParentsOfType(Table.OPERATORS)) {
				ret.addAll(((SoarDatabaseRow) opItem).getDirectedJoinedParentsOfType(Table.PROBLEM_SPACES));
			}
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
		ArrayList<SoarDatabaseRow> problemSpaces = agent.getChildrenOfType(table);
		for (SoarDatabaseRow row : problemSpaces) {
			assert row.getTable() == table;
			ret.put(row.getName(), row);
		}
		return ret;
	}

	/**
	 * <p>
	 * e.g. Start with:
	 * </p>
	 * <code>
	 * (state &lt;s&gt; ^name substate ^superstate &lt;ss&gt;)<br />
	 * (&lt;ss&gt; ^name superstate)
	 * </code>
	 * <p>
	 * End up with:
	 * </p>
	 * <code>
	 * (state &lt;ss&gt; ^name superstate)
	 * </code>
	 */
	/*
	 * public static ArrayList<Triple> getTriplesForSuperstate(ArrayList<Triple>
	 * triples) { ArrayList<Triple> ret = new ArrayList<Triple>();
	 * 
	 * // Find all superstate variables. HashSet<String> superstateVariables =
	 * new HashSet<String>(); for (Triple triple : triples) { if
	 * (triple.hasState) { if (triple.valueIsVariable()) {
	 * superstateVariables.add(triple.value); } } }
	 * 
	 * // Find superstate triples for (Triple triple : triples) { if
	 * (superstateVariables.contains(triple.variable)) { Triple newTriple = new
	 * Triple(triple.variable, triple.attribute, triple.value, triple.rule);
	 * newTriple.hasState = true; ret.add(newTriple); } }
	 * 
	 * return ret; }
	 */

	public static ArrayList<SoarDatabaseRow> getTagsForRow(SoarDatabaseRow row) {
		ArrayList<SoarDatabaseRow> ret = new ArrayList<SoarDatabaseRow>();
		ArrayList<SoarDatabaseRow> tags = row.getTopLevelRow().getChildrenOfType(Table.TAGS);
		for (SoarDatabaseRow tag : tags) {
			if (SoarDatabaseRow.rowsAreJoined(tag, row, tag.getDatabaseConnection())) {
				ret.add(tag);
			}
		}
		return ret;
	}

	/**
	 * 
	 * @param problemSpace
	 *            The problem space
	 * @param path
	 *            The attribute path to search for
	 * @param type
	 *            The type of node to search for
	 * @return The node of the given type at the given path from the given
	 *         problem space, or null if not found.
	 */
	public static ArrayList<SoarDatabaseRow> getNodesAtPathFromProblemSpace(SoarDatabaseRow problemSpace, String[] path, Table type) {
		if (problemSpace.getTable() != Table.PROBLEM_SPACES) {
			throw new IllegalArgumentException("Row is not of type PROBLEM_SPACES: " + problemSpace);
		}
		ArrayList<SoarDatabaseRow> leaves = problemSpace.getChildrenOfType(Table.DATAMAP_IDENTIFIERS); // Starts
																										// with
																										// root
																										// node
																										// <s>
		for (int i = 0; i < path.length; ++i) {
			String term = path[i];
			ArrayList<SoarDatabaseRow> leafChildren = new ArrayList<SoarDatabaseRow>();
			ArrayList<SoarDatabaseRow> newLeaves = new ArrayList<SoarDatabaseRow>();
			for (SoarDatabaseRow leaf : leaves) {
				leafChildren.addAll(leaf.getDirectedJoinedRowsFromTable(Table.DATAMAP_IDENTIFIERS));
				if (i == path.length - 1) {
					leafChildren.addAll(leaf.getDirectedJoinedRowsFromTable(type));
				}
			}
			for (SoarDatabaseRow leafChild : leafChildren) {
				if (leafChild.getName().equals(term)) {
					newLeaves.add(leafChild);
				}
			}

			if (newLeaves.size() == 0) {
				return newLeaves;
			}
			leaves = newLeaves;
		}

		return leaves;
	}

	/**
	 * Tests for the existance of a path on this problem space's datamap.
	 * 
	 * @param row
	 *            The problem space
	 * @param path
	 *            The path to test for
	 * @param value
	 *            The value of the path to test for, or null if only checking
	 *            for existance of the path.
	 * @return
	 */
	public static boolean problemSpaceMatchesAttributePath(SoarDatabaseRow row, String[] path, String value) {
		if (row.getTable() != Table.PROBLEM_SPACES) {
			throw new IllegalArgumentException("Row is not of type PROBLEM_SPACES: " + row);
		}
		ArrayList<SoarDatabaseRow> leaves = row.getChildrenOfType(Table.DATAMAP_IDENTIFIERS); // Starts
																								// with
																								// root
																								// node
																								// <s>
		for (int i = 0; i < path.length; ++i) {
			String term = path[i];
			ArrayList<SoarDatabaseRow> leafChildren = new ArrayList<SoarDatabaseRow>();
			ArrayList<SoarDatabaseRow> newLeaves = new ArrayList<SoarDatabaseRow>();
			for (SoarDatabaseRow leaf : leaves) {
				leafChildren.addAll(leaf.getDirectedJoinedRowsFromTable(Table.DATAMAP_IDENTIFIERS));
				if (i == path.length - 1) {
					leafChildren.addAll(leaf.getDirectedJoinedRowsFromTable(Table.DATAMAP_ENUMERATIONS));
					leafChildren.addAll(leaf.getDirectedJoinedRowsFromTable(Table.DATAMAP_STRINGS));
					leafChildren.addAll(leaf.getDirectedJoinedRowsFromTable(Table.DATAMAP_INTEGERS));
					leafChildren.addAll(leaf.getDirectedJoinedRowsFromTable(Table.DATAMAP_FLOATS));
				}
			}
			for (SoarDatabaseRow leafChild : leafChildren) {
				if (leafChild.getName().equals(term)) {
					newLeaves.add(leafChild);
				}
			}

			if (newLeaves.size() == 0) {
				return false;
			}
			leaves = newLeaves;
		}

		// The path exists.
		if (value == null) {
			return true;
		}

		for (SoarDatabaseRow leaf : leaves) {
			for (SoarDatabaseRow leafValue : leaf.getChildrenOfType(Table.DATAMAP_ENUMERATION_VALUES)) {
				if (leafValue.getName().equals(value)) {
					return true;
				}
			}
		}

		return false;
	}
}
