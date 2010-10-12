package com.soartech.soar.ide.core.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.soartech.soar.ide.core.ast.Pair;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class Triple {
	
	public final static String STRING_VALUE = "@string"; 
	
	// This is a String that begins with '<' and ends with '>'.
	public String variable;
	public int variableOffset = -1;

	// This is a String. If it begins with '<' and ends with '>',
	// it's a variable. Otherwise, it's a constant.
	public String attribute;
	public int attributeOffset = -1;
	
	// This is a String. If it begins with '<' and ends with '>',
	// it's a variable. Otherwise, it's a constant.
	public String value;
	public int valueOffset = -1;
	
	// Whether this triple's variable is the root node <s>.
	public boolean hasState = false;
	
	// The row to which this triple belongs.
	public SoarDatabaseRow rule;
	
    // This is a list of all datamap nodes that the triple matches against.
    // It gets filled as the existing datamap is traversed and new
    // datamap nodes are propsed and added.
	// DEPRECATED.
    public ArrayList<SoarDatabaseRow> nodes = new ArrayList<SoarDatabaseRow>();
    
    // This is a list of triples from the same rule,
    // whose values are the same as this triple's variable.
    // This gets populated by TraveralUtil#addAttributePathInformationToTriples.
    public ArrayList<Triple> parentTriples;
    
    // Similar to parent triples but for children
    public ArrayList<Triple> childTriples;
    
    private ArrayList<ArrayList<Triple>> attributePaths = null;
    
    // Set by GenerateDatamapFromVisualSoarFileAction.run() when reading from Visual Soar datamaps
    public String comment;
    
	public Triple(String variable, String attribute, String value, SoarDatabaseRow rule) {
		this.variable = variable;
		this.attribute = attribute;
		this.value = value;
		this.rule = rule;
	}
	
	public Triple(Pair variable, Pair attribute, Pair value, SoarDatabaseRow rule) {
		try {
		this.variable = variable.getString();
		variableOffset = variable.getOffset();
		this.attribute = attribute.getString();
		attributeOffset = attribute.getOffset();
		this.value = value.getString();
		valueOffset = value.getOffset();
		this.rule = rule;
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
	
	public static Triple tripleForRow(SoarDatabaseRow tripleRow) {
		assert tripleRow.getTable() == Table.TRIPLES;
		String variable = (String) tripleRow.getColumnValue("variable_string");
		String attribute = (String) tripleRow.getColumnValue("attribute_string");
		String value = (String) tripleRow.getColumnValue("value_string");
		Integer variableOffset = (Integer) tripleRow.getColumnValue("variable_offset");
		Integer attributeOffset = (Integer) tripleRow.getColumnValue("attribute_offset");
		Integer valueOffset = (Integer) tripleRow.getColumnValue("value_offset");
		boolean hasState = ((Integer) tripleRow.getColumnValue("has_state")) != 0;
		SoarDatabaseRow rule = tripleRow.getParents().get(0);
		Triple ret = new Triple(variable, attribute, value, rule);
		ret.variableOffset = variableOffset;
		ret.attributeOffset = attributeOffset;
		ret.valueOffset = valueOffset;
		ret.hasState = hasState;
		return ret;
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
		return matcher.matches() && !value.equals("-");
	}
	
	public boolean valueIsFloat() {
		if (valueIsInteger()) {
			return false;
		}
		Pattern pattern = Pattern.compile("^-?\\d*\\.\\d*$");
		Matcher matcher = pattern.matcher(value);
		return matcher.matches() && !value.equals("-");
	}
	
	public SoarDatabaseRow getDatabaseRow() {
		StatementWrapper sw = rule.getDatabaseConnection().prepareStatement("select * from " + Table.TRIPLES.tableName() + " where " + Table.RULES.idName() + "=?"
				+ " and variable_string=? and attribute_string=? and value_string=?");
		sw.setInt(1, rule.getID());
		sw.setString(2, variable);
		sw.setString(3, attribute);
		sw.setString(4, value);
		ResultSet rs = sw.executeQuery();
		SoarDatabaseRow ret = null;
		try {
			if (rs.next()) {
				int id = rs.getInt("id");
				ret = new SoarDatabaseRow(Table.TRIPLES, id, rule.getDatabaseConnection());
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		sw.close();
		return ret;
	}
	
	public SoarDatabaseRow getOrCreateTripleRow() {
		SoarDatabaseRow ret = getDatabaseRow();
		if (ret != null) return null;
		StatementWrapper sw  = rule.getDatabaseConnection().prepareStatement("insert into " + Table.TRIPLES.tableName()
				+ " (rule_id, variable_string, variable_offset, attribute_string, attribute_offset, value_string, value_offset, has_state) values (?,?,?,?,?,?,?,?)");
		sw.setInt(1, rule.getID());
		sw.setString(2, variable);
		sw.setInt(3, variableOffset);
		sw.setString(4, attribute);
		sw.setInt(5, attributeOffset);
		sw.setString(6, value);
		sw.setInt(7, valueOffset);
		sw.setBoolean(8, hasState);
		sw.execute();
		return getDatabaseRow();
	}
	
	/**
	 * 
	 * @return The list of the list of attribute Strings to get from state &lt;s&gt; to this Triple's variable.
	 */
	public ArrayList<ArrayList<Triple>> getTriplePathsFromState() {
		ArrayList<ArrayList<Triple>> ret = new ArrayList<ArrayList<Triple>>();
		
		LinkedList<ArrayList<Triple>> tripleStack = new LinkedList<ArrayList<Triple>>();
		LinkedList<Integer> indexStack = new LinkedList<Integer>();
		LinkedList<HashSet<Triple>> visitedStack = new LinkedList<HashSet<Triple>>();
		
		ArrayList<Triple> thisList = new ArrayList<Triple>();
		thisList.add(this);
		tripleStack.push(thisList);
		
		indexStack.push(new Integer(0));
		
		HashSet<Triple> thisHash = new HashSet<Triple>();
		thisHash.add(this);
		visitedStack.push(thisHash);
		
		while(tripleStack.size() > 0) {
			
			Triple leaf = tripleStack.peek().get(indexStack.peek());
			
			if (leaf.hasState) {
				// Found a solution -- don't need to look at parents.
				ArrayList<Triple> retPath = new ArrayList<Triple>();
				// Use iterators for fast performance on linked lists.
				Iterator<ArrayList<Triple>> tripleIt = tripleStack.iterator();
				Iterator<Integer> indexIt = indexStack.iterator();
				while (tripleIt.hasNext()) {
					ArrayList<Triple> nextTriples = tripleIt.next();
					Integer nextIndex = indexIt.next();
					Triple nextTriple = nextTriples.get(nextIndex);
					retPath.add(nextTriple);
				}
				ret.add(retPath);
			} else {
				// Not a solution -- keep searching.
				ArrayList<Triple> parents = leaf.parentTriples;
				ArrayList<Triple> newParents = new ArrayList<Triple>();
				HashSet<Triple> newVisited = new HashSet<Triple>(visitedStack.peek());
				for (Triple parent : parents) {
					if (!(visitedStack.peek().contains(parent))) {
						newParents.add(parent);
						newVisited.add(parent);
					}
				}
				if (newParents.size() > 0) {
					// Push onto all stacks.
					tripleStack.push(newParents);
					visitedStack.push(newVisited);
					indexStack.push(new Integer(-1));
				}
			}
			
			// Increment index
			Integer currentIndex = indexStack.pop();
			indexStack.push(new Integer(currentIndex + 1));
			
			// Pop all stacks if neccesarry.
			while (indexStack.size() > 0 && indexStack.peek() >= tripleStack.peek().size()) {
				tripleStack.pop();
				indexStack.pop();
				visitedStack.pop();
				
				if (indexStack.size() > 0) {
					// Increment index
					currentIndex = indexStack.pop();
					indexStack.push(new Integer(currentIndex + 1));
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @return The list of the list of attribute Strings to get from state &lt;s&gt; to this Triple's variable.
	 */
	public ArrayList<ArrayList<Triple>> oldGetTriplePathsFromState() {
	
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
			System.out.println("leaves.size(): " + leaves.size());
			ArrayList<TriplePath> newLeaves = new ArrayList<TriplePath>();
			for (TriplePath path : leaves) {
				if (path.last().hasState) {
					ret.add(path.path);
					System.out.println("Added to ret: " + path.path);
				}
				else {
					ArrayList<Triple> parentTriples = path.last().parentTriples;
					System.out.println("  " + path.path);
					System.out.println("  parentTriples.size(): " + parentTriples.size());
					System.out.println("  path.visited.size(): " + path.visited.size());
					int numNewLeaves = 0;
					for (Triple triple : parentTriples) {
						if (!path.visited.contains(triple)) {
							newLeaves.add(path.next(triple));
							++numNewLeaves;
						}
					}
					System.out.println("  numNewLeaves: " + numNewLeaves);
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
	
	class AttributePathNode {
		String attribute;
		ArrayList<AttributePathNode> childNodes;
		public AttributePathNode(String attribute, ArrayList<AttributePathNode> childNodes) {
			this.attribute = attribute;
			this.childNodes = childNodes;
		}
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
		
		int numPath = 0;
		ArrayList<SoarDatabaseRow> currentRows = new ArrayList<SoarDatabaseRow>();
		ArrayList<SoarDatabaseRow> childRows = new ArrayList<SoarDatabaseRow>();
		ArrayList<SoarDatabaseRow> temp;
		for (ArrayList<String> path : paths) {
			++numPath;
			//System.out.println(numPath);
			currentRows.clear();
			currentRows.add(root);
			for (int i = 0; i < path.size(); ++i) {
				String attribute = path.get(i);
				childRows.clear();
				for (SoarDatabaseRow currentRow : currentRows) {
					ArrayList<ISoarDatabaseTreeItem> childItems = currentRow.getDirectedJoinedChildren(false);
					for (ISoarDatabaseTreeItem childItem : childItems) {
						SoarDatabaseRow childRow = (SoarDatabaseRow) childItem;
						if ((childRow.getTable() == Table.DATAMAP_IDENTIFIERS || i == path.size() - 1) && attribute.equals(childRow.getName())) {
							childRows.add(childRow);
						}
					}
				}
				temp = currentRows;
				currentRows = childRows;
				childRows = temp;
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
