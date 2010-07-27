package com.soartech.soar.ide.ui.actions.explorer.DatabaseTraversal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
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
    
    // Similar to parent triples but for children
    public ArrayList<Triple> childTriples;
    
    private ArrayList<ArrayList<Triple>> attributePaths = null;

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
		if (value == null) return false;
		int lastIndex = value.length() - 1;
		return value.indexOf('<') == 0 && value.lastIndexOf('>') == lastIndex;
	}
	
	public boolean valueIsConstant() {
		return !valueIsVariable();
	}

	public boolean valueIsString() {
		return !valueIsVariable() && !valueIsInteger() && !valueIsFloat();
	}

	@Override
	public String toString() {
		return "(" + variable + " ^" + attribute + " " + value.toString() + ")";
	}
	
	public boolean valueIsInteger() {
		Pattern pattern = Pattern.compile("^-?\\d*$");
		Matcher matcher = pattern.matcher(value);
		return matcher.matches();
	}
	
	public boolean valueIsFloat() {
		if (valueIsInteger()) {
			return false;
		}
		Pattern pattern = Pattern.compile("^-?\\d*\\.\\d*$");
		Matcher matcher = pattern.matcher(value);
		return matcher.matches();
	}
	
	/**
	 * 
	 * @return The list of the list of attribute Strings to get from state &lt;s&gt; to this Triple's variable.
	 */
	public ArrayList<ArrayList<Triple>> getTriplePathsFromState() {
		
		if (attributePaths != null) return attributePaths;
		
		//if (parentTriples == null) return null;
		ArrayList<ArrayList<Triple>> ret = new ArrayList<ArrayList<Triple>>();
		class TriplePath {
			public ArrayList<Triple> path;
			public HashSet<Triple> visited;
			public TriplePath(Triple triple) {
				path = new ArrayList<Triple>();
				path.add(triple);
				visited = new HashSet<Triple>();
				visited.add(triple);
			}			
			private TriplePath(Triple triple, ArrayList<Triple> path, HashSet<Triple> visited) {
				this.path = path;
				this.visited = visited;
			}
			public TriplePath next(Triple triple) {
				TriplePath ret = new TriplePath(triple, new ArrayList<Triple>(path), new HashSet<Triple>(visited));
				ret.path.add(triple);
				ret.visited.add(triple);
				return ret;
			}
			public Triple last() {
				return path.get(path.size() - 1);
			}
		}
		
		ArrayList<TriplePath> leaves = new ArrayList<TriplePath>();
		leaves.add(new TriplePath(this));
		
		while (leaves.size() > 0) {
			ArrayList<TriplePath> newLeaves = new ArrayList<TriplePath>();
			for (TriplePath path : leaves) {
				if (path.last().hasState) {
					ret.add(path.path);
				}
				else {
					ArrayList<Triple> triples = path.last().parentTriples;
					for (Triple triple : triples) {
						if (!path.visited.contains(triple)) {
							newLeaves.add(path.next(triple));
						}
					}
				}
			}
			leaves = newLeaves;
		}
		
		for (ArrayList<Triple> ar : ret) {
			Collections.reverse(ar);
		}
		
		// Cache result
		attributePaths = ret;
		
		return ret;
	}
	
	public ArrayList<ArrayList<String>> getAttributePathsFromState() {
		ArrayList<ArrayList<Triple>> triplePaths = getTriplePathsFromState();
		ArrayList<ArrayList<String>> ret = new ArrayList<ArrayList<String>>();
		for (ArrayList<Triple> triplePath : triplePaths) {
			ArrayList<String> stringPath = new ArrayList<String>(); 
			for (Triple triple : triplePath) {
				stringPath.add(triple.attribute);
			}
			ret.add(stringPath);
		}
		return ret;
	}
	
	public boolean matchesPath(String[] matchPath) {
		ArrayList<ArrayList<String>> paths = getAttributePathsFromState();
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
	
	/**
	 * 
	 * @return The list of all datamap rows that correspond to this Triple.
	 */
	public ArrayList<SoarDatabaseRow> getDatamapRowsFromProblemSpace(SoarDatabaseRow problemSpace) {
		assert problemSpace.getTable() == Table.PROBLEM_SPACES;
		SoarDatabaseRow root = (SoarDatabaseRow) problemSpace.getChildrenOfType(Table.DATAMAP_IDENTIFIERS).get(0);
		ArrayList<SoarDatabaseRow> ret = new ArrayList<SoarDatabaseRow>();
		ArrayList<ArrayList<String>> paths = getAttributePathsFromState();
		
		for (ArrayList<String> path : paths) {
			ArrayList<SoarDatabaseRow> currentRows = new ArrayList<SoarDatabaseRow>();
			currentRows.add(root);
			for (int i = 0; i < path.size(); ++i) {
				String attribute = path.get(i);
				ArrayList<SoarDatabaseRow> childRows = new ArrayList<SoarDatabaseRow>();
				for (SoarDatabaseRow currentRow : currentRows) {
					ArrayList<ISoarDatabaseTreeItem> childItems = currentRow.getDirectedJoinedChildren(false);
					for (ISoarDatabaseTreeItem childItem : childItems) {
						SoarDatabaseRow childRow = (SoarDatabaseRow) childItem;
						if (childRow.getTable() == Table.DATAMAP_IDENTIFIERS || i == path.size() - 1) {
							if (attribute.equals(childRow.getName())) {
								childRows.add(childRow);
							}
						}
					}
				}
				currentRows = childRows;
			}
			ret.addAll(currentRows);
		}
		
		return ret;
	}
	
	public boolean isStateName() {
		return matchesPath(new String[] {"name"});
	}
	
	public boolean isOperatorName() {
		return matchesPath(new String[] {"operator", "name"});
	}
	
	public boolean isSuperstateName() {
		return matchesPath(new String[] {"superstate", "name"});
	}
	
	public boolean isSuperstateOperatorName() {
		return matchesPath(new String[] {"superstate", "operator", "name"});
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Triple)) return false;
		Triple other = (Triple) obj;
		return this.variable.equals(other.variable)
				&& this.attribute.equals(other.attribute)
				&& this.value.equals(other.value);
	}
}
