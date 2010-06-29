package com.soartech.soar.ide.ui.actions.explorer.DatabaseTraversal;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class Triple {
	
	// This is a String that begins with '<' and ends with '>'.
	public String variable;

	// This is a String. If it begins with '<' and ends with '>',
	// it's a variable. Otherwise, it's a constant.
	public String attribute;
	
	// This is a String. If it begins with '<' and ends with '>',
	// it's a variable. Otherwise, it's a constant.
	public String value;
	
	public boolean hasState = false;
	
	public SoarDatabaseRow rule;
	
    // This is a list of all datamap nodes that the triple matches against.
    // It gets filled as the existing datamap is traversed and new
    // datamap nodes are propsed and added.
    public ArrayList<SoarDatabaseRow> nodes = new ArrayList<SoarDatabaseRow>();
    

	public Triple(String variable, String attribute, String value, SoarDatabaseRow rule) {
		this.variable = variable;
		this.attribute = attribute;
		this.value = value;
		this.rule = rule;
	}
	
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

	public boolean valueIsString() {
		return value.getClass() == String.class;
	}

	@Override
	public String toString() {
		return "(" + variable + " ^" + attribute + " " + value.toString() + ")";
	}
	
	public boolean valueIsInteger() {
		Pattern pattern = Pattern.compile("^\\d*$");
		Matcher matcher = pattern.matcher(value);
		return matcher.matches();
	}
	
	public boolean valueIsFloat() {		
		Pattern pattern = Pattern.compile("^\\d*(\\.\\d*)?$");
		Matcher matcher = pattern.matcher(value);
		return matcher.matches();
	}
}
