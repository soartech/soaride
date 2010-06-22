package com.soartech.soar.ide.ui.actions.explorer.DatabaseTraversal;

import java.util.ArrayList;
import java.util.HashMap;

import com.soartech.soar.ide.core.model.ast.Constant;
import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class TraversalUtil {
	public static ArrayList<Triple> getTriplesForRule(SoarDatabaseRow rule) {
		assert rule.getTable() == Table.RULES;
		ArrayList<Triple> triples = new ArrayList<Triple>();
		visitRuleNode(rule, triples, new StringBuffer(), new ArrayList<String>(), new StringBuffer(), false, false, false, false, false, rule, 0);
		return triples;
	}
	
	private static void visitRuleNode(
			SoarDatabaseRow row,
			ArrayList<Triple> triples,
			StringBuffer currentVariable,
			ArrayList<String> currentAttributes,
			StringBuffer currentValue,
			boolean testingAttribute,
			boolean testingValue,
			boolean inDisjunctionTest,
			boolean hasState,
			boolean inDotNotation,
			SoarDatabaseRow rule,
			int depth) {

		// Variables to hold new variables, attributes or values.
 		String newVariable = null;
		String newAttribute = null;
		String newValue = null;

		// Remember when we enter a disjunction test so that we can
		// remove the 'inDisjunctionTest' flag when we're done.
		boolean enteredDisjunctionTest = false;
		boolean wasInDisjunctionTest = inDisjunctionTest;

		// Switch on table value.
		Table table = row.getTable();

		// Possibly set flags
		if (table == Table.ATTRIBUTE_TESTS || (table == Table.RHS_VALUES && !testingValue)) {
			testingAttribute = true;
		} else if (table == Table.VALUE_TESTS || table == Table.VALUE_MAKES) {
			testingValue = true;
		} else if (table == Table.DISJUNCTION_TESTS) {
			inDisjunctionTest = true;
			enteredDisjunctionTest = true;
		} else if (table == Table.CONDITION_FOR_ONE_IDENTIFIERS) {
			Object objHasState = row.getColumnValue("has_state");
			if (objHasState instanceof Integer) {
				// should be true
				hasState = ((Integer) objHasState) == 1;
				if (hasState) {
					System.out.println("hasState = true");
				}
			}
			newVariable = (String) row.getColumnValue("variable");
		}

		// Possibly gather new variable or new value.
		else if (table == Table.CONDITION_FOR_ONE_IDENTIFIERS || table == Table.VAR_ATTR_VAL_MAKES) {
			newVariable = (String) row.getColumnValue("variable");
		} else if (table == Table.SINGLE_TESTS) {
			Boolean isConstant = ((Integer) row.getColumnValue("is_constant")) != 0;
			if (!isConstant) {
				String variable = (String) row.getColumnValue("variable");
				if (testingAttribute) {
					newAttribute = variable;
				} else if (testingValue) {
					newValue = variable;
				}
			}
		} else if (table == Table.CONSTANTS) {
			Integer constantType = (Integer) row.getColumnValue("constant_type");
			if (constantType == Constant.FLOATING_CONST) {
				Float floatingConst = (Float) row.getColumnValue("floating_const");
				if (testingAttribute) {
					newAttribute = "" + floatingConst;
				} else if (testingValue) {
					newValue = "" + floatingConst;
				}
			} else if (constantType == Constant.INTEGER_CONST) {
				Integer integerConst = (Integer) row.getColumnValue("integer_const");
				if (testingAttribute) {
					newAttribute = "" + integerConst;
				} else if (testingValue) {
					newValue = "" + integerConst;
				}
			} else if (constantType == Constant.SYMBOLIC_CONST) {
				String symbolicConst = (String) row.getColumnValue("symbolic_const");
				if (testingAttribute) {
					newAttribute = symbolicConst;
				} else if (testingValue) {
					newValue = symbolicConst;
				}
			}
		} else if (table == Table.RHS_VALUES) {
			boolean isVariable = ((Integer) row.getColumnValue("is_variable")) != 0;
			if (isVariable) {
				String variable = (String) row.getColumnValue("variable");
				if (testingAttribute) {
					newAttribute = variable;
				} else if (testingValue) {
					newValue = variable;
				}
			}
		}

		// Now we maybe have the new variable, attribute or value.
		// Depending on parse state flags, update state and / or produce a new
		// triple.
		if (newVariable != null) {
			currentVariable.replace(0, currentVariable.length(), newVariable);
		} else if (newAttribute != null) {
			if (currentAttributes.size() > 0 && !inDisjunctionTest) {
				// Dot notation, add triple

				// Make a new variable out of the current attribute.
				String recentAttribute = currentAttributes.get(currentAttributes.size() - 1);
				newVariable = "<_" + recentAttribute + ">";

				// Create new triple using the current variable and attribute
				// and the new variable
				boolean tripleHasState = hasState && !inDotNotation;
				Triple newTriple = new Triple(currentVariable.toString(), recentAttribute, newVariable, rule, tripleHasState);
				System.out.println("Triple: " + newTriple + ", has state: " + tripleHasState + " (dot)");
				triples.add(newTriple);

				// Set current variable to the newly created variable.
				currentVariable.replace(0, currentVariable.length(), newVariable);
			}

			// If we're not in a disjunction test, clear the attributes list
			// before adding the new one
			if (!inDisjunctionTest) {
				currentAttributes.clear();
			}

			// Add new attribute to attributes list.
			currentAttributes.add(newAttribute);

		} else if (newValue != null) {

			// Set the current value to the new value.
			currentValue.replace(0, currentValue.length(), newValue);
		}

		// If we have a variable, attributes and a value, produce a new triple
		// for each attribute and wipe clean the attributes and value.
		if (currentVariable.length() > 0 && currentAttributes.size() > 0 && currentValue.length() > 0) {
			for (int i = 0; i < currentAttributes.size(); ++i) {
				String attribute = currentAttributes.get(i);
				boolean tripleHasState = hasState && !inDotNotation;
				Triple newTriple = new Triple(currentVariable.toString(), attribute, currentValue.toString(), rule, tripleHasState);
				System.out.println("Triple: " + newTriple + ", hasState: " + hasState);
				triples.add(newTriple);
			}
			currentValue.replace(0, currentValue.length(), "");

			// Unless we're in a disjunction test, clear the attributes list.
			if (!inDisjunctionTest) {
				currentAttributes.clear();
			}
		}

		// Visit children
		ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, false);
		SoarDatabaseRow lastChild = null;
		for (ISoarDatabaseTreeItem item : children) {
			if (item instanceof SoarDatabaseRow) {
				SoarDatabaseRow child = (SoarDatabaseRow) item;
				
				for (int i = 0; i < depth; ++i) {
					System.out.print("    ");
				}
				System.out.println(child.toString());
				
				if (lastChild != null && lastChild.getTable() == Table.ATTRIBUTE_TESTS && child.getTable() == Table.ATTRIBUTE_TESTS) {
					inDotNotation = true;
					System.out.println("In Dot");
				}
				
				visitRuleNode(child, triples, currentVariable, currentAttributes, currentValue, testingAttribute, testingValue, inDisjunctionTest, hasState, inDotNotation, rule, depth + 1);
				lastChild = child;
			}
		}

		// Remove temporary flags
		if (enteredDisjunctionTest) {
			inDisjunctionTest = wasInDisjunctionTest;
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
}
