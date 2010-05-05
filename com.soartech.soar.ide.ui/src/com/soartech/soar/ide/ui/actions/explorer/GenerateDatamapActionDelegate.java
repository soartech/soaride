package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.soartech.soar.ide.core.model.ast.Constant;
import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class GenerateDatamapActionDelegate implements IObjectActionDelegate {

	public class Triple {
		// This is a String that begins with '<' and ends with '>'.
		public String variable;
		
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
		
		public Triple(String variable, String attribute, int value) {
			init(variable, attribute, new Integer(value));
		}
		
		public Triple(String variable, String attribute, float value) {
			init(variable, attribute, new Float(value));
		}
		
		public Triple(String variable, String attribute, String value) {
			init(variable, attribute, value);
		}
		
		protected void init(String variable, String attribute, Object value) {
			this.variable = variable;
			this.attribute = attribute;
			this.value = value;
			assert valueIsString() || valueIsInteger() || valueIsFloat();
		}
		
		@Override
		public String toString() {
			return "Triple, variable: " + variable + ", attribute: " + attribute + " value: " + value.toString();
		}
	}
	
	StructuredSelection ss;
	SoarDatabaseRow selectedProblemSpace;
	
	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		selectedProblemSpace = null;
	}

	@Override
	public void run(IAction action) {
		// Generate the datamap.
		if (selectedProblemSpace != null) {
			ArrayList<Triple> triples = new ArrayList<Triple>();
			ArrayList<ISoarDatabaseTreeItem> childRows = selectedProblemSpace.getJoinedRowsFromTable(Table.RULES);
			for (ISoarDatabaseTreeItem item : childRows) {
				if (item instanceof SoarDatabaseRow) {
					assert ((SoarDatabaseRow)item).getTable() == Table.RULES;
					visit((SoarDatabaseRow) item, triples, new StringBuffer(), new StringBuffer(), new StringBuffer(), false, false, 0);
				}
			}
			
			// Show results
			System.out.println("***Triples:");
			for (Triple triple : triples) {
				System.out.println(triple.toString());
			}
		}
	}
	
	private void visit(SoarDatabaseRow row, ArrayList<Triple> triples, StringBuffer currentVariable, StringBuffer currentAttribute, StringBuffer currentValue, boolean testingAttribute, boolean testingValue, int depth) {
		Table table = row.getTable();
		if (table == Table.CONDITION_FOR_ONE_IDENTIFIERS) {
			String variable = (String) row.getColumnValue("variable");
			currentVariable.replace(0, currentVariable.length(), variable);
		}
		else if (table == Table.VAR_ATTR_VAL_MAKES) {
			String variable = (String) row.getColumnValue("variable");
			currentVariable.replace(0, currentVariable.length(), variable);
		}
		else if (table == Table.ATTRIBUTE_TESTS) {
			testingAttribute = true;
		}
		else if (table == Table.RHS_VALUES && !testingValue) {
			testingAttribute = true;
		}
		else if (table == Table.VALUE_TESTS) {
			testingValue = true;
		}
		else if (table == Table.VALUE_MAKES) {
			testingValue = true;
		}
		else if (table == Table.SINGLE_TESTS) {
			Boolean isConstant = ((Integer) row.getColumnValue("is_constant")) != 0;
			if (!isConstant) {
				String variable = (String) row.getColumnValue("variable");
				if (testingAttribute) {
					currentAttribute.replace(0, currentAttribute.length(), variable);
				} else if (testingValue) {
					currentValue.replace(0, currentValue.length(), variable);
				}
			}
		}
		else if (table == Table.CONSTANTS) {
			Integer constantType = (Integer) row.getColumnValue("constant_type");
			if (constantType == Constant.FLOATING_CONST) {
				Float floatingConst = (Float) row.getColumnValue("floating_const");
				if (testingAttribute) {
					currentAttribute.replace(0, currentAttribute.length(), "" + floatingConst);
				} else if (testingValue) {
					currentValue.replace(0, currentValue.length(), "" + floatingConst);
				}
			} else if (constantType == Constant.INTEGER_CONST) {
				Integer integerConst = (Integer) row.getColumnValue("integer_const");
				if (testingAttribute) {
					currentAttribute.replace(0, currentAttribute.length(), "" + integerConst);
				} else if (testingValue) {
					currentValue.replace(0, currentValue.length(), "" + integerConst);
				}
			} else if (constantType == Constant.SYMBOLIC_CONST) {
				String symbolicConst = (String) row.getColumnValue("symbolic_const");
				if (testingAttribute) {
					currentAttribute.replace(0, currentAttribute.length(), symbolicConst);
				} else if (testingValue) {
					currentValue.replace(0, currentValue.length(), symbolicConst);
				}
			}
		}
		
		// Possibly add a new triple.
		if (currentVariable.length() > 0 && currentAttribute.length() > 0 && currentValue.length() > 0) {
			Triple newTriple = new Triple(currentVariable.toString(), currentAttribute.toString(), currentValue.toString());
			currentAttribute.replace(0, currentAttribute.length(), "");
			currentValue.replace(0, currentValue.length(), "");
			triples.add(newTriple);
		}
		
		// Visit children
		ArrayList<ISoarDatabaseTreeItem> children = row.getChildren(false, false, false, false, false, true);
		for (ISoarDatabaseTreeItem item : children) {
			if (item instanceof SoarDatabaseRow) {
				SoarDatabaseRow child = (SoarDatabaseRow) item;
				for (int i = 0; i < depth; ++i) {
					System.out.print("    ");
				}
				System.out.println(child.toString());
				visit(child, triples, currentVariable, currentAttribute, currentValue, testingAttribute, testingValue, depth + 1);
			}
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
