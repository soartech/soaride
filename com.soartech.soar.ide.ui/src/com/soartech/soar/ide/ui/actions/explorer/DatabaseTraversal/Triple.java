package com.soartech.soar.ide.ui.actions.explorer.DatabaseTraversal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
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
    
    // This is a list of triples from the same rule,
    // whose values are the same as this triple's variable.
    // This gets populated by TraveralUtil#addAttributePathInformationToTriples.
    public ArrayList<Triple> parentTriples;
    
    private ArrayList<ArrayList<String>> attributePaths = null;

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
	
	/**
	 * 
	 * @return The list of the list of attribute Strings to get from state &lt;s&gt; to this Triple's variable.
	 */
	public ArrayList<ArrayList<String>> getAttributePathsFromState() {
		
		if (attributePaths != null) return attributePaths;
		
		//if (parentTriples == null) return null;
		HashSet<Triple> visited = new HashSet<Triple>();
		visited.add(this);
		ArrayList<ArrayList<String>> ret = new ArrayList<ArrayList<String>>();
		class TriplePathPair {
			public Triple triple;
			public ArrayList<String> path;
			public TriplePathPair(Triple triple, ArrayList<String> path) {
				this.triple = triple; this.path = path;
			}
		}
		
		ArrayList<TriplePathPair> leaves = new ArrayList<TriplePathPair>();
		ArrayList<String> initialPath = new ArrayList<String>();
		initialPath.add(this.attribute);
		leaves.add(new TriplePathPair(this, initialPath));
		
		while (leaves.size() > 0) {
			ArrayList<TriplePathPair> newLeaves = new ArrayList<TriplePathPair>();
			for (TriplePathPair pair : leaves) {
				if (pair.triple.hasState) {
					ret.add(pair.path);
				}
				else {
					ArrayList<Triple> triples = pair.triple.parentTriples;
					for (Triple triple : triples) {
						if (!visited.contains(triple)) {
							visited.add(triple);
							ArrayList<String> triplePath = new ArrayList<String>(pair.path);
							triplePath.add(triple.attribute);
							newLeaves.add(new TriplePathPair(triple, triplePath));
						}
					}
				}
			}
			leaves = newLeaves;
		}
		
		for (ArrayList<String> ar : ret) {
			Collections.reverse(ar);
		}
		
		// Cache result
		attributePaths = ret;
		
		return ret;
	}
	
	public boolean matchesPath(String[] matchPath) {
		ArrayList<ArrayList<String>> paths =  getAttributePathsFromState();
		if (paths == null) return false;
		for (ArrayList<String> path : paths) {
			if (path.size() == matchPath.length) {
				boolean match = true;
				for (int i = 0; i < path.size() && match; ++i) {
					if (!path.get(i).equals(matchPath[i])) {
						match = false;
					}
				}
				if (match) {
					return match;
				}
			}
		}
		return false;
	}
	
	public boolean isStateName() {
		return matchesPath(new String[] {"name"});
	}
	
	public boolean isOperatorName() {
		return matchesPath(new String[] {"operator", "name"});
	}
	
	public boolean isSuperstateName() {
		return matchesPath(new String[] {"superstaate", "name"});
	}
	
	public boolean isSuperstateOperatorName() {
		return matchesPath(new String[] {"superstate", "operator", "name"});
	}
}
