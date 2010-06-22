package com.soartech.soar.ide.ui.actions.explorer.DatabaseTraversal;

import java.util.ArrayList;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class Triple {
	// This is a String that begins with '<' and ends with '>'.
	public Variable variable;

	// This is a String. If it begins with '<' and ends with '>',
	// it's a variable. Otherwise, it's a constant.
	public String attribute;

	// This is a list of all datamap nodes that the triple matches against.
	// It gets filled as the existing datamap is traversed and new
	// datamap nodes are propsed and added.
	public ArrayList<SoarDatabaseRow> nodes = new ArrayList<SoarDatabaseRow>();

	// Whether this triple begins with '(state <s>' or similar
	public boolean hasState;
	
	/**
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
	
	public boolean valueIsConstant() {
		return !valueIsVariable();
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

	public Triple(String variable, String attribute, int value, SoarDatabaseRow rule, boolean hasState) {
		init(variable, attribute, new Integer(value), rule, hasState);
	}

	public Triple(String variable, String attribute, float value, SoarDatabaseRow rule, boolean hasState) {
		init(variable, attribute, new Float(value), rule, hasState);
	}

	public Triple(String variable, String attribute, String value, SoarDatabaseRow rule, boolean hasState) {
		init(variable, attribute, value, rule, hasState);
	}

	protected void init(String variable, String attribute, Object value, SoarDatabaseRow rule, boolean hasState) {
		this.variable = new Variable(variable, rule);
		this.attribute = attribute;
		this.value = value;
		this.hasState = hasState;
		assert rule.getTable() == Table.RULES;
		assert valueIsString() || valueIsInteger() || valueIsFloat();
	}
	
	public String valueAsString() {
		return "" + value;
	}

	@Override
	public String toString() {
		return "(" + variable.name + " ^" + attribute + " " + value.toString() + ")";
	}
}
