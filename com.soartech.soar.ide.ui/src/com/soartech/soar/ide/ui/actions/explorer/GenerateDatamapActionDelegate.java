package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.soartech.soar.ide.core.model.ast.Constant;
import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class GenerateDatamapActionDelegate implements IObjectActionDelegate {

	public class Triple {
		
		// This is a String that begins with '<' and ends with '>'.
		public Variable variable;
		
		// This is a String. If it begins with '<' and ends with '>',
		// it's a variable. Otherwise, it's a constant.
		public String attribute;
		
		/**
		 * 
		 * @return True if the attribute is a variable
		 * (it begins with '<' and ends with '>'), false
		 * if it's a constant.
		 */
		public boolean attributeIsVariable() {
			int lastIndex = attribute.length() - 1;
			return attribute.indexOf('<') == 0 && attribute.lastIndexOf('>') == lastIndex;
		}
		
		public boolean attributeIsConstant() {
			return !attributeIsVariable();
		}
		
		/**
		 * 
		 * @return True if the value is a variable
		 * (it's a String that begins with '<' and ends with '>'), false
		 * otherwise.
		 */
		public boolean valueIsVariable() {
			if (!valueIsString()) {
				return false;
			}
			String stringValue = (String) value;
			int lastIndex = stringValue.length() - 1;
			return stringValue.indexOf('<') == 0 && stringValue.lastIndexOf('>') == lastIndex;
		}
		
		// This is either a String, Integer, or Float.
		public Object value;
		public boolean valueIsString() {
			return value.getClass() == String.class;
		}
		public boolean valueIsInteger() {
			return value.getClass() == Integer.class;
		}
		public boolean valueIsFloat() {
			return value.getClass() == Float.class;
		}
		
		public Triple(String variable, String attribute, int value, SoarDatabaseRow rule) {
			init(variable, attribute, new Integer(value), rule);
		}
		
		public Triple(String variable, String attribute, float value, SoarDatabaseRow rule) {
			init(variable, attribute, new Float(value), rule);
		}
		
		public Triple(String variable, String attribute, String value, SoarDatabaseRow rule) {
			init(variable, attribute, value, rule);
		}
		
		protected void init(String variable, String attribute, Object value, SoarDatabaseRow rule) {
			this.variable = new Variable(variable, rule);
			this.attribute = attribute;
			this.value = value;
			assert rule.getTable() == Table.RULES;
			assert valueIsString() || valueIsInteger() || valueIsFloat();
		}
		
		@Override
		public String toString() {
			return "( " + variable.name + " ^" + attribute + " " + value.toString() + " )";
		}
	}
	
	public class Variable {
		
		// e.g. "<s>"
		public String name;
		
		public SoarDatabaseRow rule;
		
		public Variable(String name, SoarDatabaseRow rule) {
			assert rule == null || rule.getTable() == Table.RULES;
			this.name = name;
			this.rule = rule;
		}
		
		@Override
		public int hashCode() {
			if (rule != null) {
				return name.hashCode() + rule.hashCode();
			}
			return name.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Variable) {
				Variable other = (Variable) obj;
				if (this.rule == null && other.rule == null) {
					boolean ret = this.name.equals(other.name);
					return ret;
				}
				boolean ret = this.name.equals(other.name) && this.rule.equals(other.rule);
				return ret;
			}
			return false;
		}
		
		@Override
		public String toString() {
			if (rule != null) {
				return name + " (" + rule.getName() + ")";
			}
			return name;
		}
	}
	
	StructuredSelection ss;
	SoarDatabaseRow selectedProblemSpace;
	Shell shell;
	
	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		selectedProblemSpace = null;
		shell = targetPart.getSite().getShell();
	}

	@Override
	public void run(IAction action) {
		// Generate the datamap.
		if (selectedProblemSpace != null) {
			ArrayList<ISoarDatabaseTreeItem> joinedRules = selectedProblemSpace.getJoinedRowsFromTable(Table.RULES);
			for (ISoarDatabaseTreeItem item : joinedRules) {
				if (item instanceof SoarDatabaseRow) {
					assert ((SoarDatabaseRow) item).getTable() == Table.RULES;
					
					
					ArrayList<Triple> triples = new ArrayList<Triple>();
					SoarDatabaseRow row = (SoarDatabaseRow) item;

					System.out.println("Visiting rule: " + row.getName());
					
					visitRuleNode(row, triples, new StringBuffer(), new ArrayList<String>(), new StringBuffer(), false, false, false, row, 0);

					// Show results
					System.out.println("***Triples:");
					for (Triple triple : triples) {
						System.out.println(triple.toString());
					}

					// Recursively visit existing datamap,
					// proposing corrections where conflicts arise.
					ArrayList<ISoarDatabaseTreeItem> childIdentifiers = selectedProblemSpace.getChildrenOfType(Table.DATAMAP_IDENTIFIERS);
					for (ISoarDatabaseTreeItem childIdentifier : childIdentifiers) {
						SoarDatabaseRow child = (SoarDatabaseRow) childIdentifier;
						HashSet<Variable> variableSet = new HashSet<Variable>();
						variableSet.add(new Variable("<s>", null));
						HashSet<SoarDatabaseRow> visitedNodes = new HashSet<SoarDatabaseRow>();
						HashSet<Triple> usedTriples = new HashSet<Triple>();
						visitDatamapNode(child, variableSet, triples, visitedNodes, usedTriples);
					}
				}
			}
		}
	}
	
	private void visitDatamapNode(SoarDatabaseRow node, HashSet<Variable> variableSet, ArrayList<Triple> triples, HashSet<SoarDatabaseRow> visitedNodes, HashSet<Triple> usedTriples) {
		
		// This method is recursive, but each node should only be visited once.
		ArrayList<SoarDatabaseRow> visitedList = new ArrayList<SoarDatabaseRow>(visitedNodes);
		if (visitedNodes.contains(node)) {
			return;
		}
		
		for (Triple triple : triples) {
			if (variableSetContainsMatchingVariable(variableSet, triple.variable)) {
				boolean matched = false;
				ArrayList<ISoarDatabaseTreeItem> nodeAttributes = node.getDirectedJoinedChildren(false);
				for (ISoarDatabaseTreeItem item : nodeAttributes) {
					SoarDatabaseRow attribute = (SoarDatabaseRow) item;
					String attributeName = attribute.getName();
					if (attributeName.equals(triple.attribute) && typesMatch(attribute, triple)) {
						matched = true;
						
						System.out.println("Matched: " + triple + ", " + attribute);
						
						// TODO
						// Check that the value of the triple is within the bounds specified by the datamap.
						// If it's not, propose a correction to the user.
					}
				}
				if (!matched) {
					
					System.out.println("No match: " + triple);
					
					if (!usedTriples.contains(triple)) {
						// Propose a new datamap attribute to match the given
						// triple.
						String title = "Generate Datamap Node?";
						String message = "No datamap node for triple in rule \"" + triple.variable.rule + "\":\n" + triple + "\n\nGenerate node?";
						String[] options = { "OK", "Cancel" };
						Image image = MessageDialog.getDefaultImage();
						MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.QUESTION, options, 0);
						int result = dialog.open();
						if (result == 0) {
							generateDatamapNodeForTriple(node, triple);
							// Remember that this triple was used
							usedTriples = new HashSet<Triple>(usedTriples);
							usedTriples.add(triple);
						}
					} else {
						System.out.println("Triple already used. Avoiding recursive node generation.");
					}
				}
			}
		}

		// Attributes may have changed -- query database again.
		ArrayList<ISoarDatabaseTreeItem> nodeAttributes = node.getDirectedJoinedChildren(false);
		for (ISoarDatabaseTreeItem item : nodeAttributes) {
			SoarDatabaseRow attribute = (SoarDatabaseRow) item;
			if (attribute.getTable() == Table.DATAMAP_IDENTIFIERS) {
				HashSet<Variable> nextVariableSet = new HashSet<Variable>();
				for (Triple triple : triples) {
					if (variableSetContainsMatchingVariable(variableSet, triple.variable)
							&& triple.attribute.equals(attribute.getName())
							&& triple.valueIsVariable()) {
						nextVariableSet.add(new Variable((String) triple.value, triple.variable.rule));
					}
				}
				visitedNodes = new HashSet<SoarDatabaseRow>(visitedNodes);
				visitedNodes.add(node);
				visitDatamapNode(attribute, nextVariableSet, triples, visitedNodes, usedTriples);
			}
		}
	}
	
	private void generateDatamapNodeForTriple(SoarDatabaseRow node, Triple triple) {
		Table childTable = null;
		if (triple.valueIsVariable()) {
			childTable = Table.DATAMAP_IDENTIFIERS;
		}
		else if (triple.valueIsString()) {
			childTable = Table.DATAMAP_STRINGS;
		}
		String name = triple.attribute;
		node.createJoinedChild(childTable, name);
	}
	
	private boolean variableSetContainsMatchingVariable(HashSet<Variable> variables, Variable variable) {
		if (variables.contains(variable)) {
			return true;
		}
		Variable newVariable = new Variable(variable.name, null);
		boolean ret = variables.contains(newVariable);
		return ret;
	}
	
	private boolean typesMatch(SoarDatabaseRow attribute, Triple triple) {
		Table table = attribute.getTable();
		return (table == Table.DATAMAP_STRINGS)
				|| (table == Table.DATAMAP_ENUMERATIONS)
				|| (table == Table.DATAMAP_IDENTIFIERS && triple.valueIsVariable())
				|| (table == Table.DATAMAP_INTEGERS && triple.valueIsInteger())
				|| (table == Table.DATAMAP_FLOATS && triple.valueIsFloat());
	}
	
	private void visitRuleNode(SoarDatabaseRow row, ArrayList<Triple> triples, StringBuffer currentVariable, ArrayList<String> currentAttributes, StringBuffer currentValue, boolean testingAttribute, boolean testingValue, boolean inDisjunctionTest, SoarDatabaseRow rule, int depth) {
		
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
		}
		else if (table == Table.VALUE_TESTS || table == Table.VALUE_MAKES) {
			testingValue = true;
		}
		else if (table == Table.DISJUNCTION_TESTS) {
			inDisjunctionTest = true;
			enteredDisjunctionTest = true;
		}
		
		// Possibly gather new variable or new value.
		else if (table == Table.CONDITION_FOR_ONE_IDENTIFIERS || table == Table.VAR_ATTR_VAL_MAKES) {
			newVariable = (String) row.getColumnValue("variable");
		}
		else if (table == Table.SINGLE_TESTS) {
			Boolean isConstant = ((Integer) row.getColumnValue("is_constant")) != 0;
			if (!isConstant) {
				String variable = (String) row.getColumnValue("variable");
				if (testingAttribute) {
					newAttribute = variable;
				} else if (testingValue) {
					newValue = variable;
				}
			}
		}
		else if (table == Table.CONSTANTS) {
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
		}
		else if (table == Table.RHS_VALUES) {
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
		// Depending on parse state flags, update state and / or produce a new triple.
		if (newVariable != null) {
			currentVariable.replace(0, currentVariable.length(), newVariable);
		}	
		else if (newAttribute != null) {
			if (currentAttributes.size() > 0 && !inDisjunctionTest) {
				// Dot notation, add triple
				
				// Make a new variable out of the current attribute.
				String recentAttribute = currentAttributes.get(currentAttributes.size() - 1);
				newVariable = "<_" + recentAttribute + ">";
				
				// Create new triple using the current variable and attribute and the new variable
				Triple newTriple = new Triple(currentVariable.toString(), recentAttribute, newVariable, rule);
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
		
		// If we have a variable, attributes and a value, produce a new triple for each attribute and wipe clean the attributes and value.
		if (currentVariable.length() > 0 && currentAttributes.size() > 0 && currentValue.length() > 0) {
			for (int i = 0; i < currentAttributes.size(); ++i) {
				String attribute = currentAttributes.get(i);
				Triple newTriple = new Triple(currentVariable.toString(), attribute, currentValue.toString(), rule);
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
		for (ISoarDatabaseTreeItem item : children) {
			if (item instanceof SoarDatabaseRow) {
				SoarDatabaseRow child = (SoarDatabaseRow) item;
				for (int i = 0; i < depth; ++i) {
					System.out.print("    ");
				}
				System.out.println(child.toString());
				visitRuleNode(child, triples, currentVariable, currentAttributes, currentValue, testingAttribute, testingValue, inDisjunctionTest, rule, depth + 1);
			}
		}
		
		// Remove temporary flags
		if (enteredDisjunctionTest) {
			inDisjunctionTest = wasInDisjunctionTest;
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		selectedProblemSpace = null;
		action.setEnabled(false);
		if (selection instanceof StructuredSelection) {
			ss = (StructuredSelection)selection;
			Object obj = ss.getFirstElement();
			if (obj instanceof SoarDatabaseRow) {
				SoarDatabaseRow row = (SoarDatabaseRow) obj;
				if (row.getTable() == Table.PROBLEM_SPACES) {
					action.setEnabled(true);
					selectedProblemSpace = row;
				}
			}
		}
	}

}
