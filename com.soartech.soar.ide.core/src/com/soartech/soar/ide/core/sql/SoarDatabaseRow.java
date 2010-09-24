package com.soartech.soar.ide.core.sql;

import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;

import com.soartech.soar.ide.core.SoarProblem;
import com.soartech.soar.ide.core.ast.Action;
import com.soartech.soar.ide.core.ast.AttributeTest;
import com.soartech.soar.ide.core.ast.AttributeValueMake;
import com.soartech.soar.ide.core.ast.AttributeValueTest;
import com.soartech.soar.ide.core.ast.BinaryPreference;
import com.soartech.soar.ide.core.ast.Condition;
import com.soartech.soar.ide.core.ast.ConditionForOneIdentifier;
import com.soartech.soar.ide.core.ast.ConjunctiveTest;
import com.soartech.soar.ide.core.ast.Constant;
import com.soartech.soar.ide.core.ast.DisjunctionTest;
import com.soartech.soar.ide.core.ast.ForcedUnaryPreference;
import com.soartech.soar.ide.core.ast.FunctionCall;
import com.soartech.soar.ide.core.ast.NaturallyUnaryPreference;
import com.soartech.soar.ide.core.ast.Pair;
import com.soartech.soar.ide.core.ast.ParseException;
import com.soartech.soar.ide.core.ast.PositiveCondition;
import com.soartech.soar.ide.core.ast.PreferenceSpecifier;
import com.soartech.soar.ide.core.ast.RHSValue;
import com.soartech.soar.ide.core.ast.RelationalTest;
import com.soartech.soar.ide.core.ast.SimpleTest;
import com.soartech.soar.ide.core.ast.SingleTest;
import com.soartech.soar.ide.core.ast.SoarParser;
import com.soartech.soar.ide.core.ast.SoarProductionAst;
import com.soartech.soar.ide.core.ast.Test;
import com.soartech.soar.ide.core.ast.Token;
import com.soartech.soar.ide.core.ast.TokenMgrError;
import com.soartech.soar.ide.core.ast.ValueMake;
import com.soartech.soar.ide.core.ast.ValueTest;
import com.soartech.soar.ide.core.ast.VarAttrValMake;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent.Type;

/**
 * Represents a row in a database table.
 * 
 * This class is too big.
 * Some methods could be split off into a utility class.
 * Some methods and static data could be split off into a metadata class.
 * Some methods could be removed or consilidated.
 * 
 * Static variables provide metadata about what tables exist and
 * what kinds of connections exist between them.
 * 
 * Static methods provide ways to join rows together.
 * 
 * Rows are joined in three ways:
 * 
 * 1) A row is a child row of another row if it has a column
 * in it that stores the ID of the parent row.
 * 
 * 2) A row is undirectoed-joined with another row if there is
 * an entry in an undirected-join table that contains the ids of both rows.
 * An undirected join in symmetrical; there is no parent-child relationship
 * implied.
 * 
 * 3) A row is directed-joined with another row if there is an entry
 * in a directed-join table that contains the the ids of both rows.
 * A directed join indicates that one item is the parent and one is the child.
 * 
 * @author miller
 * 
 */
public class SoarDatabaseRow implements ISoarDatabaseTreeItem {

	// One entry for each table in the database.
	public enum Table {
		
		// Agents
		AGENTS,
		PROBLEM_SPACES,
		OPERATORS,
		TAGS,

		// Rules
		TRIPLES,
		RULES,
		CONDITIONS,
		POSITIVE_CONDITIONS,
		CONDITION_FOR_ONE_IDENTIFIERS,
		ATTRIBUTE_VALUE_TESTS,
		ATTRIBUTE_TESTS,
		VALUE_TESTS,
		TESTS,
		CONJUNCTIVE_TESTS,
		SIMPLE_TESTS,
		DISJUNCTION_TESTS,
		RELATIONAL_TESTS,
		
		// Not used
		//RELATIONS,
		
		SINGLE_TESTS,
		CONSTANTS,
		ACTIONS,
		VAR_ATTR_VAL_MAKES,
		ATTRIBUTE_VALUE_MAKES,
		FUNCTION_CALLS,
		FUNCTION_NAMES,
		RHS_VALUES,
		VALUE_MAKES,
		//PREFERENCES,
		PREFERENCE_SPECIFIERS,
		//NATURALLY_UNARY_PREFERENCES,
		//BINARY_PREFERENCES,
		//FORCED_UNARY_PREFERENCES,

		// Datamap
		DATAMAP_IDENTIFIERS,
		DATAMAP_ENUMERATIONS,
		DATAMAP_ENUMERATION_VALUES,
		DATAMAP_INTEGERS,
		DATAMAP_FLOATS,
		DATAMAP_STRINGS,
		;

		public String tableName() {
			return toString().toLowerCase();
		}

		public String shortName() {
			String name = tableName();
			return name.substring(0, name.length() - 1);
		}

		public String idName() {
			return shortName() + "_id";
		}

		public String soarName() {
			return shortName().replace('_', '-');
		}
		
		public String englishName() {
			String ret = shortName().replace('_', ' ');
			return ret.substring(0, 1).toUpperCase() + ret.substring(1);
		}
		
		public String pluralEnglishName() {
			String ret = tableName().replace('_', ' ');
			return ret.substring(0, 1).toUpperCase() + ret.substring(1);
		}
		
		public boolean isDatamapTable() {
			return this == 	DATAMAP_IDENTIFIERS
					|| this == DATAMAP_ENUMERATIONS
					|| this == DATAMAP_ENUMERATION_VALUES
					|| this == DATAMAP_INTEGERS
					|| this == DATAMAP_FLOATS
					|| this == DATAMAP_STRINGS;
		}
		
		public boolean isAstTable() {
			return this == TRIPLES
			 		|| this == RULES
			 		|| this == CONDITIONS
			 		|| this == POSITIVE_CONDITIONS
			 		|| this == CONDITION_FOR_ONE_IDENTIFIERS
			 		|| this == ATTRIBUTE_VALUE_TESTS
			 		|| this == ATTRIBUTE_TESTS
			 		|| this == VALUE_TESTS
			 		|| this == TESTS
			 		|| this == CONJUNCTIVE_TESTS
			 		|| this == SIMPLE_TESTS
			 		|| this == DISJUNCTION_TESTS
			 		|| this == RELATIONAL_TESTS
			 		|| this == SINGLE_TESTS
			 		|| this == CONSTANTS
			 		|| this == ACTIONS
			 		|| this == VAR_ATTR_VAL_MAKES
			 		|| this == ATTRIBUTE_VALUE_MAKES
			 		|| this == FUNCTION_CALLS
			 		|| this == FUNCTION_NAMES
			 		|| this == RHS_VALUES
			 		|| this == VALUE_MAKES
			 		|| this == PREFERENCE_SPECIFIERS;
		}
	}

	public static Table getTableNamed(String name) {
		for (Table t : Table.values()) {
			if (name.equalsIgnoreCase(t.name())) {
				return t;
			}
		}
		return null;
	}

	/**
	 * Represents the relationship between two joined rows.
	 * For most joins, this will be JoinType.NONE.
	 * For superstate-substate relationships, this indicates
	 * the type of impasse between the problem spaces
	 * @author miller
	 *
	 */
	public enum JoinType {
		NONE,
		TIE_IMPASSE,
		CONFLICT_IMPASSE,
		CONTRAINT__FAILURE_IMPASSE,
		NO__CHANGE_IMPASSE,
		STATE_NO__CHANGE_IMPASSE,
		OPERATOR_NO__CHANGE_IMPASSE,
		;
		
		public static JoinType typeForInt(int i) {
			JoinType[] values = JoinType.values();
			if (i < 0 || i >= values.length) {
				return JoinType.NONE;
			}
			return values[i];
		}
		
		public String englishName() {
			String[] tokens = toString().toLowerCase().replace("__", "-").split("_");
			StringBuffer buff = new StringBuffer();
			for (int i = 0; i < tokens.length; ++i) {
				buff.append(tokens[i].substring(0, 1).toUpperCase() + tokens[i].substring(1));
				if (i < tokens.length) {
					buff.append(" ");
				}
			}
			return buff.toString();
		}
		
		public String shortEnglishName() {
			String[] tokens = englishName().split(" ");
			StringBuffer buff = new StringBuffer();
			for (int i = 0; i + 1 < tokens.length; ++i) {
				buff.append(tokens[i].substring(0, 1).toUpperCase() + tokens[i].substring(1));
				if (i + 2 < tokens.length) {
					buff.append(" ");
				}
			}
			return buff.toString();
		}
	}
	
	/**
	 * Assign a join type to each row
	 * This is a little messy because, in fact, the join type depends on
	 * which parent the row was found from. So, a row may have more than
	 * one valid join type.
	 * 
	 * The join type is stored in the join table, not in the row's table.
	 * 
	 **/
	protected JoinType joinType = JoinType.NONE;
	
	// Maps a table onto its possible child tables.
	private static HashMap<Table, ArrayList<Table>> childTables = new HashMap<Table, ArrayList<Table>>();

	// Maps a table onto its possible parent tables.
	private static HashMap<Table, ArrayList<Table>> parentTables = new HashMap<Table, ArrayList<Table>>();

	// Maps a class from an abstract syntax tree onto the equivelent Table.
	private static HashMap<Class<?>, Table> tableForAstNode = new HashMap<Class<?>, Table>();

	// Maps a table onto the a list of child Tables which should be displayed in
	// folders.
	private static HashMap<Table, ArrayList<Table>> childFolders = new HashMap<Table, ArrayList<Table>>();

	// Maps a table onto a list of all tables it's joined to.
	// This isn't multi-directional; the key must be the first table name in the
	// join table.
	// (see getTablesJoinedToTable)
	private static HashMap<Table, ArrayList<Table>> joinedTables = new HashMap<Table, ArrayList<Table>>();

	// Maps a table onto a list of all tables it's directionally joined to.
	// The key is the source table; each value is a child table.
	private static HashMap<Table, ArrayList<Table>> directedJoinedTables = new HashMap<Table, ArrayList<Table>>(); 
	
	// Maps a table onto a list of tables that each should have a row generated
	// automatically
	// when the key table is created.
	private static HashMap<Table, ArrayList<Table>> automaticChildren = new HashMap<Table, ArrayList<Table>>();
	
	// Maps a Table onto a list of editable columns for that table.
	private static HashMap<Table, ArrayList<EditableColumn>> editableColumns = new HashMap<Table, ArrayList<EditableColumn>>();
	
	private Table table;
	private int id;
	private SoarDatabaseConnection db;
	private HashMap<Table, SoarDatabaseRowFolder> folders = new HashMap<Table, SoarDatabaseRowFolder>();
	private SoarDatabaseEditorInput editorInput = null;
	
	// Makes sure the static init() is only called once.
	// This is a little messy -- should probably have used
	// static blocks instead.
	private static boolean initted = false;

	/**
	 * Class constructor.
	 * Doesn't change the database -- this should be called
	 * to represent rows already existing in the database.
	 * @param table 
	 * @param id
	 * @param db
	 */
	public SoarDatabaseRow(Table table, int id, SoarDatabaseConnection db) {

		if (!initted) {
			init();
		}

		this.table = table;
		this.id = id;
		this.db = db;

		// Set up folders
		if (childFolders.containsKey(table)) {
			ArrayList<Table> folderTypes = childFolders.get(table);
			for (Table type : folderTypes) {
				SoarDatabaseRowFolder folder = new SoarDatabaseRowFolder(this,
						type);
				folders.put(type, folder);
			}
		}
	}

	/**
	 * Expands on getName() to provide a little more information about certain rows.
	 */
	@Override
	public String toString() {
		String ret = getName();
		if (table == Table.DATAMAP_ENUMERATIONS) {
			ArrayList<SoarDatabaseRow> values = getChildrenOfType(Table.DATAMAP_ENUMERATION_VALUES);
			ret += " (";
			int i;
			for (i = 0; i < values.size(); ++i) {
				ret += values.get(i);
				if (i < values.size() - 1) {
					ret += ", ";
				}
			}
			ret += ")";
		} else if (table == Table.DATAMAP_INTEGERS || table == Table.DATAMAP_FLOATS) {
			ret += " (" + getColumnValue("min_value") + ", " + getColumnValue("max_value") + ")";
		}
		
		return ret;
	}

	/**
	 * Gets the name of the row.
	 * The only table that doesn't have a "name" field is Table.TRIPLES,
	 * so for those rows a String representation is created.
	 * @return
	 */
	public String getName() {
		if (table == Table.TRIPLES) {
			return "(" + getColumnValue("variable_string") + " ^" + getColumnValue("attribute_string") + " " + getColumnValue("value_string") + ")";
		}
		String name = null;
		String sql = "select (name) from " + table.tableName() + " where id=?";
		StatementWrapper ps = db.prepareStatement(sql);
		ps.setInt(1, id);
		ResultSet rs = ps.executeQuery();
		try {
			if (rs.next()) {
				name = rs.getString("name");
			} else {
				name = table.tableName() + ": NO ROW WITH ID " + id;
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return name;
	}

	/**
	 * Updates the name field in the database.
	 * Doesn't work on rows of type Table.TRIPLES.
	 * @param name
	 */
	public void setName(String name) {
		if (table == Table.TRIPLES) return;
		String sql = "update " + table.tableName() + " set name=? where id=?";
		StatementWrapper ps = db.prepareStatement(sql);
		ps.setString(1, name);
		ps.setInt(2, id);
		ps.execute();
	}
	
	/**
	 * Each datamap row has a comment column, which is displayed
	 * in the datamap editor.
	 * @return
	 */
	public String getComment() {
		if (!(table.isDatamapTable())) {
			return null;
		}
		return (String) getColumnValue("comment_text");
	}
	
	/**
	 * Sets the value of the comment column.
	 * @param comment
	 */
	public void setComment(String comment) {
		if (!(table.isDatamapTable())) {
			return;
		}
		StatementWrapper sw = db.prepareStatement("update " + table.tableName() + " set comment_text=? where id=?");
		sw.setRow(this);
		sw.setString(1, comment);
		sw.setInt(2, getID());
		sw.execute();
	}
	
	/**
	 * For datamap nodes.
	 * 
	 * Returns a String that shows the whole path to this node from the root node,
	 * including the name of the problem space.
	 * 
	 * If no ancestor node is found, returns <code>getName()</code>.
	 * @return
	 */
	public String getPathName() {
		ArrayList<SoarDatabaseRow> path = getPathToAncestorNodeOfType(Table.PROBLEM_SPACES);
		if (path == null) {
			return getName();
		}
		StringBuffer buff = new StringBuffer();
		String separator = ".";
		final int stateNodeIndex = 1; // don't include "<s>" in the path -- use problem space name instead.
		for (int i = 0; i < path.size(); ++i) {
			if (i != stateNodeIndex) {
				SoarDatabaseRow row = path.get(i);
				buff.append(row.getName());
				buff.append(separator);
			}
		}
		String ret = buff.substring(0, buff.length() - separator.length());
		return ret;
	}
	
	/**
	 * Updates the value of a single column in this row.
	 * 
	 * CAUTION:
	 * Doesn't use a prepared statment.
	 * Doesn't properly escape values.
	 * @param column
	 * @param value
	 */
	public void updateValue(String column, String value) {
		updateValues(new String[] {column}, new String[] {value});		
	}
	
	/**
	 * Updates the values of one or more columns in this row.
	 * 
	 * CAUTION:
	 * Doesn't use a prepared statment.
	 * Doesn't properly escape values.
	 * @param columns
	 * @param values
	 */
	public void updateValues(String[] columns, String[] values) {
		int size = Math.min(columns.length, values.length);
		String sql = "update " + table.tableName() + " set ";
		for (int i = 0; i < size; ++i) {
			sql += columns[i] + "=" + values[i];
			if (i != size - 1) {
				sql += ", ";
			} else {
				sql += " where id=" + this.id;
			}
		}
		db.execute(sql);
	}
	
	/**
	 * Gets the value of a single column from this row.
	 * @param column
	 * @return
	 */
	public Object getColumnValue(String column) {
		String sql = "select * from " + table.tableName() + " where id=?";
		StatementWrapper sw = db.prepareStatement(sql);
		sw.setInt(1, id);
		ResultSet rs = sw.executeQuery();
		Object ret = null;
		try {
			if (rs.next()) {
				ret = rs.getObject(column);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		sw.close();
		return ret;
	}

	/**
	 * Acessor method.
	 * @return The table this row belongs to.
	 */
	public Table getTable() {
		return table;
	}
	
	/**
	 * Acessor method.
	 * @return The primary key id for this row.
	 */
	public int getID() {
		return id;
	}

	/**
	 * Deletes this row from the database.
	 * Also deletes joins that involve this row.
	 */
	private void delete() {
		String sql = "delete from " + table.tableName() + " where id=?";
		StatementWrapper ps = db.prepareStatement(sql);
		ps.setInt(1, id);
		ps.execute();

		// Also remove joins.
		removeAllJoins();
	}

	/**
	 * Deletes all joins involving this row.
	 */
	public void removeAllJoins() {
		// Remove undirected joins
		ArrayList<Table> tables = getTablesJoinedToTable(this.table);
		for (Table t : tables) {
			ArrayList<SoarDatabaseRow> joinedRows = getJoinedRowsFromTable(t);
			for (SoarDatabaseRow other : joinedRows) {
				unjoinRows(this, other, db);
			}
		}
		
		// Remove directed joins where this is the child
		// (directed joins with this as the parent have already been removed in deleteAllChildren() ).
		ArrayList<SoarDatabaseRow> parentRows = getDirectedJoinedParents();
		for (ISoarDatabaseTreeItem iParent : parentRows) {
			SoarDatabaseRow parent = (SoarDatabaseRow) iParent;
			directedUnjoinRows(parent, this, db);
		}
	}

	/**
	 * 
	 * @return
	 * The list of all tables which are children of this table.
	 * That is, all tables that have a foreign key onto this row's table.
	 */
	public ArrayList<Table> getChildTables() {
		if (childTables.containsKey(table)) {
			ArrayList<Table> children = childTables.get(table);
			return children;
		}
		return new ArrayList<Table>();
	}
	
	/**
	 * 
	 * @return The list of all tables which are directed-joined children of this table.
	 * That is, all tables for which there is a directed join table with that table as
	 * the child table and this row's table as the parent table.
	 */
	public ArrayList<Table> getDirectedJoinedChildTables() {
		if (directedJoinedTables.containsKey(table)) {
			ArrayList<Table> children = directedJoinedTables.get(table);
			return children;
		}
		return new ArrayList<Table>();
	}

	/**
	 * Returns rows, folders, and join folders.
	 * 
	 * @param includeFolders
	 *            Whether to include child folders.
	 * @param includeChildrenInFolders
	 *            Whether to include rows that are contained in child folders.
	 * @param includeJoinedItems
	 *            Whether to include joined tables.
	 * @param includeDatamapNodes
	 *            Whether to include datamap nodes.
	 * @return The child elements of this row.
	 */
	public ArrayList<ISoarDatabaseTreeItem> getChildren(boolean includeFolders,
			boolean includeChildrenInFolders,
			boolean includeJoinedItems,
			boolean includeDirectionalJoinedItems,
			boolean putDirectionalJoinedItemsInFolders,
			boolean includeDatamapNodes) {
		ArrayList<ISoarDatabaseTreeItem> ret = new ArrayList<ISoarDatabaseTreeItem>();
		ArrayList<Table> children = getChildTables();
		for (Table t : children) {
			
			boolean isFolder = folders.containsKey(t);
			if (isFolder && includeFolders) {
				ret.add(folders.get(t));
			}
			if ((!isFolder) || includeChildrenInFolders) {
				if (t != Table.DATAMAP_IDENTIFIERS || includeDatamapNodes) {
					String sql = "select * from " + t.tableName() + " where "
							+ table.idName() + "=?";
					StatementWrapper ps = db.prepareStatement(sql);
					ps.setInt(1, id);
					ResultSet rs = ps.executeQuery();
					try {
						while (rs.next()) {
							SoarDatabaseRow row = new SoarDatabaseRow(t, rs
									.getInt("id"), db);
							ret.add(row);
						}
						rs.getStatement().close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
		}

		if (includeJoinedItems) {
			ArrayList<Table> joinedTables = getTablesJoinedToTable(table);
			for (Table t : joinedTables) {
				SoarDatabaseJoinFolder folder = new SoarDatabaseJoinFolder(this, t);
				ret.add(folder);
			}
		}
		
		if (includeDirectionalJoinedItems) {
			ret.addAll(getDirectedJoinedChildren(putDirectionalJoinedItemsInFolders));
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @param putInFolders Whether to package the result into folders.
	 * @return All directed-joined chilren of this row.
	 */
	public ArrayList<ISoarDatabaseTreeItem> getDirectedJoinedChildren(boolean putInFolders) {
		return getDirectedJoinedChildrenOfTypeNamed(null, null, putInFolders, false);
	}
	
	/**
	 * 
	 * @param type Only return rows from this table.
	 * @param putInFolders Whether to package the results into folder.
	 * @param assignJoinTypes Whether to assign join types to the child rows.
	 * @return All directed-joined children of this row from the given table.
	 */
	public ArrayList<ISoarDatabaseTreeItem> getDirectedJoinedChildrenOfType(Table type, boolean putInFolders, boolean assignJoinTypes) {		
		return getDirectedJoinedChildrenOfTypeNamed(type, null, putInFolders, assignJoinTypes);
	}
	
	/**
	 * @param type Only return rows from this table.
	 * @param name Only return rown with this name.
	 * @param putInFolders Whether to package the results into folder.
	 * @param assignJoinTypes Whether to assign join types to the child rows.
	 * @return All directed-joined children of this row from the given table.
	 */
	public ArrayList<ISoarDatabaseTreeItem> getDirectedJoinedChildrenOfTypeNamed(Table type, String name, boolean putInFolders, boolean assignJoinTypes) {		
		ArrayList<ISoarDatabaseTreeItem> ret = new ArrayList<ISoarDatabaseTreeItem>();
		if (directedJoinedTables.containsKey(table)) {
			ArrayList<Table> joinedTables;
			if (type == null) {
				joinedTables = directedJoinedTables.get(table);
			} else {
				joinedTables = new ArrayList<Table>();
				joinedTables.add(type);
			}
			for (Table t : joinedTables) {
				SoarDatabaseJoinFolder folder = new SoarDatabaseJoinFolder(this, t);
				if (putInFolders) {
					ret.add(folder);
				} else {
					if (name != null) {
						for (ISoarDatabaseTreeItem item : folder.getChildren(true, true, false, true, true, true)) {
							if (item instanceof SoarDatabaseRow && ((SoarDatabaseRow) item).getName().equals(name)) {
								ret.add(item);
							}
						}
					}
					else {
						ret.addAll(folder.getChildren(true, true, false, true, true, true));
					}
				}
			}
		}
		
		if (assignJoinTypes) {
			for (ISoarDatabaseTreeItem item : ret) {
				if (item instanceof SoarDatabaseRow) {
					SoarDatabaseRow row = (SoarDatabaseRow) item;
					JoinType joinType = SoarDatabaseRow.getDirectedJoinType(this, row);
					row.setJoinType(joinType);
				}
			}
		}
		
		return ret;
	}

	/**
	 * Doesn't return folders.
	 * 
	 * @param type Only returns rows from this table.
	 * @return Children of this row from the given table.
	 */
	public ArrayList<SoarDatabaseRow> getChildrenOfType(Table type) {
		return getChildrenOfType(type, null);
	}
	
	public ArrayList<SoarDatabaseRow> getChildrenOfType(Table type, String extraSql) {
		return getChildrenOfTypeNamed(type, null, extraSql);
	}

	public ArrayList<SoarDatabaseRow> getChildrenOfTypeNamed(Table type, String name) {
		return getChildrenOfTypeNamed(type, name, null);
	}
	
	/**
	 * Doesn't return folders.
	 * 
	 * @param type Only returns rows from this table.
	 * @param name Only returns rows with this name.
	 * @return Children of this row from the given table with the given name.
	 */
	public ArrayList<SoarDatabaseRow> getChildrenOfTypeNamed(Table type, String name, String extraSql) {
		ArrayList<SoarDatabaseRow> ret = new ArrayList<SoarDatabaseRow>();
		ArrayList<Table> children = getChildTables();
		for (Table t : children) {
			if (t == type) {
				String sql = "select * from " + t.tableName() + " where " + table.idName() + "=?";
				if (extraSql != null && extraSql.length() > 0) {
					sql += " " + extraSql;
				}
				StatementWrapper ps = db.prepareStatement(sql);
				ps.setInt(1, id);
				ResultSet rs = ps.executeQuery();
				try {
					while (rs.next()) {
						SoarDatabaseRow row = new SoarDatabaseRow(t, rs.getInt("id"), db);
						if (name == null || row.getName().equals(name)) {
							ret.add(row);
						}
					}
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return ret;
	}

	/**
	 * @return True if this row has any child rows.
	 */
	public boolean hasChildren() {
		ArrayList<Table> children = getChildTables();
		for (Table t : children) {
			String sql = "select * from " + t.tableName() + " where "
					+ table.idName() + "=?";
			StatementWrapper ps = db.prepareStatement(sql);
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			try {
				if (rs.next()) {
					ps.close();
					return true;
				}
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (folders.size() > 0) {
			return true;
		}
		if (getTablesJoinedToTable(table).size() > 0) {
			return true;
		}
		if (directedJoinedTables.containsKey(table)) {
			return directedJoinedTables.get(table).size() > 0; 
		}
		return false;
	}

	/**
	 * 
	 * @return True if this row has no parent rows.
	 */
	public boolean isOrphan() {
		if ((!hasParentJoinedRows()) && (!hasParents())) {
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @return True if this row's table is a datamap node.
	 */
	public boolean isDatamapNode() {
		return table.isDatamapTable();
	}
	
	/**
	 * 
	 * @param type
	 * @return True if this row has any children from the given table.
	 */
	public boolean hasChildrenOfType(Table type) {
		if (!getChildTables().contains(type)) return false;
		String sql = "select * from " + type.tableName() + " where "
				+ table.idName() + "=?";
		StatementWrapper ps = db.prepareStatement(sql);
		ps.setInt(1, id);
		ResultSet rs = ps.executeQuery();
		try {
			if (rs.next()) {
				ps.close();
				return true;
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 
	 * @return A list of all parent tables of this row's table.
	 * That is, all tables that this row's table contains a foreign key to.
	 */
	public ArrayList<Table> getParentTables() {
		if (parentTables.containsKey(table)) {
			ArrayList<Table> parents = parentTables.get(table);
			return parents;
		}
		return new ArrayList<Table>();
	}

	/**
	 * Should only return a list of size 0 or 1
	 * 
	 * @return All parent rows for this row.
	 */
	public ArrayList<SoarDatabaseRow> getParents() {
		ArrayList<SoarDatabaseRow> ret = new ArrayList<SoarDatabaseRow>();
		ArrayList<Table> parents = getParentTables();
		for (Table t : parents) {
			String sql = "select * from " + t.tableName()
					+ " where id = (select " + t.idName() + " from "
					+ table.tableName() + " where id=?)";
			StatementWrapper ps = db.prepareStatement(sql);
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			try {
				while (rs.next()) {
					SoarDatabaseRow row = new SoarDatabaseRow(t, rs.getInt("id"), db);
					ret.add(row);
				}
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return ret;
	}
	
	/**
	 * 
	 * @return True if this row has any non-joined parent rows.
	 */
	public boolean hasParents() {
		ArrayList<Table> parents = getParentTables();
		for (Table t : parents) {
			String sql = "select * from " + t.tableName()
					+ " where id = (select " + t.idName() + " from "
					+ table.tableName() + " where id=?)";
			StatementWrapper ps = db.prepareStatement(sql);
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			try {
				if (rs.next()) {
					ps.close();
					return true;
				}
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * @return A list of all rows that are parents in a
	 * directional join with this row.
	 */
	public ArrayList<SoarDatabaseRow> getDirectedJoinedParents() {
		return getDirectedJoinedParentsOfType(null);
	}
	
	/**
	 * 
	 * @param type The type of parent row to search for.
	 * @return A list of all rows from the given table that are parents in a directional join with this row.
	 */
	public ArrayList<SoarDatabaseRow> getDirectedJoinedParentsOfType(Table type) {
		ArrayList<SoarDatabaseRow> ret = new ArrayList<SoarDatabaseRow>();
		ArrayList<Table> parentTables = getDirectedJoinParentTables(table);
		for (Table parentTable : parentTables) {
			if (type == null || parentTable == type) {
				String joinTableName = directedJoinTableName(parentTable, table);
				String sql = "select * from " + joinTableName + " where child_id=?";
				StatementWrapper sw = db.prepareStatement(sql);
				sw.setInt(1, id);
				ResultSet rs = sw.executeQuery();
				try {
					while (rs.next()) {
						int id = rs.getInt("parent_id");
						SoarDatabaseRow parentRow = new SoarDatabaseRow(parentTable, id, db);
						ret.add(parentRow);
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
				sw.close();
			}
		}
		return ret;
	}
	
	/**
	 * Returns both regular parents and directed-join parents.
	 * @return All parent rows of this row.
	 */
	public ArrayList<SoarDatabaseRow> getAllParents() {
		ArrayList<SoarDatabaseRow> ret = getParents();
		ret.addAll(getDirectedJoinedParents());
		return ret;
	}
	
	/**
	 * 
	 * @return True if this.getParentJoinedRows().size() > 0.
	 */
	public boolean hasParentJoinedRows() {
		ArrayList<Table> parentTables = getDirectedJoinParentTables(table);
		for (Table parentTable : parentTables) {
			String joinTableName = directedJoinTableName(parentTable, table);
			String sql = "select * from " + joinTableName + " where child_id=?";
			StatementWrapper sw = db.prepareStatement(sql);
			sw.setInt(1, id);
			ResultSet rs = sw.executeQuery();
			try {
				if (rs.next()) {
					sw.close();
					return true;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			sw.close();
		}
		return false;
	}
	
	/**
	 * @param type If non-null, returns the first row of this type
	 * encountered along upward traversal. If null, returns
	 * the top-level row.
	 * @return
	 */
	public SoarDatabaseRow getAncestorRow(Table type) {
		SoarDatabaseRow ret = this;
		if (table == type) {
			return this;
		}
		HashSet<SoarDatabaseRow> alreadyVisited = new HashSet<SoarDatabaseRow>();
		ArrayList<SoarDatabaseRow> parents = getAllParents();
		ArrayList<SoarDatabaseRow> newParents = parents;
		while (newParents.size() > 0) {
			parents = newParents;
			newParents = new ArrayList<SoarDatabaseRow>();
			for (SoarDatabaseRow row : parents) {
				
				// Check for a match
				if (type != null && row.getTable() == type) {
					return row;
				}
				
				// Replace current list with their parents
				ArrayList<SoarDatabaseRow> rowParents = row.getAllParents();
				for (SoarDatabaseRow rowParent : rowParents) {
					if (!newParents.contains(rowParent) && !alreadyVisited.contains(rowParent)) {
						newParents.add(rowParent);
						alreadyVisited.add(rowParent);
					}
				}
			}
		}
		if (parents.size() > 0) {
			ret = parents.get(0);
		}
		if (type == null || ret.getTable() == type) {
			return ret;
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the path to the closest ancestor node of the given type.
	 * The list begins with the ancestor node and ends with this node.
	 * Returns <code>null</code> if no ancestor node is found.
	 * @param type
	 * @return
	 */
	public ArrayList<SoarDatabaseRow> getPathToAncestorNodeOfType(Table type) {
		ArrayList<ArrayList<SoarDatabaseRow>> paths = new ArrayList<ArrayList<SoarDatabaseRow>>();
		ArrayList<SoarDatabaseRow> firstPath = new ArrayList<SoarDatabaseRow>();
		HashSet<SoarDatabaseRow> exploredRows = new HashSet<SoarDatabaseRow>();
		firstPath.add(this);
		paths.add(firstPath);
		boolean searching = true;
		
		try {
		while (searching) {
			searching = false;
			ArrayList<ArrayList<SoarDatabaseRow>> newPaths = new ArrayList<ArrayList<SoarDatabaseRow>>();
			for (ArrayList<SoarDatabaseRow> path : paths) {
				SoarDatabaseRow leaf = path.get(path.size() - 1);
				ArrayList<SoarDatabaseRow> parents = leaf.getAllParents();
				for (SoarDatabaseRow parent : parents) {
					if (!exploredRows.contains(parent)) {
						searching = true;
					} else {
						exploredRows.add(parent);
					}
				}
				for (int i = 0; i < parents.size(); ++i) {
					SoarDatabaseRow parent = parents.get(i);
					ArrayList<SoarDatabaseRow> newPath;
					if (i == 0) {
						newPath = path;
					} else {
						newPath = new ArrayList<SoarDatabaseRow>();
						for (SoarDatabaseRow row : path) {
							newPath.add(row);
						}
					}
					newPath.add(parent);
					if (parent.getTable() == type) {
						Collections.reverse(newPath);
						return newPath;
					}
					newPaths.add(newPath);
				}
			}
			paths = newPaths;
		}
		} catch (ConcurrentModificationException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Climbs up the path of parents until no more parents are found.
	 * 
	 * @return The highest-level row.
	 */
	public SoarDatabaseRow getTopLevelRow() {
		return getAncestorRow(null);
	}
	
	/**
	 *
	 * @param type Only returns rows from this table.
	 * @return A list of all descendants of this row of the given type.
	 */
	public ArrayList<ISoarDatabaseTreeItem> getDescendantsOfType(Table type) {
		HashSet<Table> types = new HashSet<Table>();
		types.add(type);
		return getDescendantsOfTypes(types, new HashSet<ISoarDatabaseTreeItem>());
	}
	
	/**
	 * 
	 * @param types Only returns rows from this set of tables.
	 * @return A list of all descendants of this row of the given types.
	 */
	public ArrayList<ISoarDatabaseTreeItem> getDescendantsOfTypes(HashSet<Table> types) {
		return getDescendantsOfTypes(types, new HashSet<ISoarDatabaseTreeItem>());
	}
	
	/**
	 * Recursive method for performing getDescendantsOfTypes(HashSet<Table> types)
	 * @param types
	 * @param visitedRows
	 * @return
	 */
	private ArrayList<ISoarDatabaseTreeItem> getDescendantsOfTypes(HashSet<Table> types, HashSet<ISoarDatabaseTreeItem> visitedRows) {
		ArrayList<ISoarDatabaseTreeItem> ret = new ArrayList<ISoarDatabaseTreeItem>();
		
		ArrayList<ISoarDatabaseTreeItem> children = getChildren(false, true, false, true, false, true);
		for (ISoarDatabaseTreeItem child : children) {
			if (!visitedRows.contains(child)) {
				visitedRows.add(child);
				
				if (child instanceof SoarDatabaseRow) {
					SoarDatabaseRow childRow = (SoarDatabaseRow) child;
					ArrayList<ISoarDatabaseTreeItem> temp = childRow.getDescendantsOfTypes(types, visitedRows);
					ret.addAll(temp);
					if (types.contains(childRow.getTable())) {
						ret.add(childRow);
					}
				}
				
			}
		}
		
		return ret;
	}
	
	/**
	 * Gets undirected joined rows from a given table.
	 * @param other The table to look for joined rows in.
	 * @param extraSql Extra text to add to the SQL query (e.g. "order by name"), or <code>null</code>.
	 * @return
	 */
	public ArrayList<SoarDatabaseRow> getUndirectedJoinedRowsFromTable(Table other) {
		ArrayList<SoarDatabaseRow> ret = new ArrayList<SoarDatabaseRow>();
		
		if (tablesAreJoined(this.table, other)) {
			boolean sameTable = (this.table == other);
			String joinTableName = joinTableName(this.table, other);
			Table[] orderedTables = orderJoinedTables(this.table, other);
			String thisTableIdName = orderedTables[0] == this.table ? "first_id" : "second_id";
			String otherTableIdName = orderedTables[0] == this.table ? "second_id" : "first_id";
			String sql = null;
			if (sameTable) {
				sql = "select * from " + joinTableName + " where (" + thisTableIdName + "=?) or (" + otherTableIdName + "=?)";
			} else {
				sql = "select * from " + joinTableName + " where " + thisTableIdName + "=?";
			}
			StatementWrapper ps = db.prepareStatement(sql);
			if (ps == null) {
				System.out.println("Null statement");
			}
			ps.setInt(1, id);
			if (sameTable) {
				ps.setInt(2, id);
			}
			ResultSet rs = ps.executeQuery();
			try {
				while (rs.next()) {
					int thisId = rs.getInt(thisTableIdName);
					int otherId = rs.getInt(otherTableIdName);
					if (otherId == this.id) {
						otherId = thisId;
					}
					SoarDatabaseRow row = new SoarDatabaseRow(other, otherId, db);
					ret.add(row);
				}
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return ret;
	}
	
	public ArrayList<SoarDatabaseRow> getDirectedJoinedRowsFromTable(Table other) {
		return getDirectedJoinedRowsFromTable(other, null);
	}

	/**
	 * 
	 * @param other Returns only rows from this table.
	 * @param extraSql Extra sql to add to the query e.g. "order by name", or <code>null</code>.
	 * @return All directed joined children of this row from the given table.
	 */
	public ArrayList<SoarDatabaseRow> getDirectedJoinedRowsFromTable(Table other, String extraSql) {
		ArrayList<SoarDatabaseRow> ret = new ArrayList<SoarDatabaseRow>();
		
		// Add directed joins.
		if (tablesAreDirectedJoined(this.table, other)) {
			String joinTableName = directedJoinTableName(this.table, other);
			String sql = "select * from " + joinTableName + " where parent_id=?";
			if (extraSql != null && extraSql.length() > 0) {
				sql += " " + extraSql;
			}
			StatementWrapper ps = db.prepareStatement(sql);
			ps.setInt(1, this.id);
			ResultSet rs = ps.executeQuery();
			try {
				while (rs.next()) {
					int id = rs.getInt("child_id");
					SoarDatabaseRow temp = new SoarDatabaseRow(other, id, db);
					ret.add(temp);
				}
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @param other Returns only rows from this table.
	 * @return All directed children and undirected rows from this row, from the given table.
	 */
	public ArrayList<SoarDatabaseRow> getJoinedRowsFromTable(Table other) {
		ArrayList<SoarDatabaseRow> ret = getDirectedJoinedRowsFromTable(other);
		ret.addAll(getUndirectedJoinedRowsFromTable(other));
		return ret;
	}

	/**
	 * 
	 * @param other
	 * @return True if this row has directed joined children or undirected joined rows
	 * from the given table.
	 */
	public boolean hasJoinedRowsFromTable(Table other) {
		boolean ret = false;
		if (tablesAreJoined(this.table, other)) {
			String joinTableName = joinTableName(this.table, other);
			Table[] orderedTables = orderJoinedTables(this.table, other);
			String thisTableIdName = orderedTables[0] == this.table ? "first_id" : "second_id";
			String sql = "select * from " + joinTableName + " where " + thisTableIdName + "=?";
			StatementWrapper ps = db.prepareStatement(sql);
			ps.setInt(1, this.id);
			ResultSet rs = ps.executeQuery();
			try {
				if (rs.next()) {
					ret = true;
				}
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (!ret && tablesAreDirectedJoined(this.table, other)) {
			String joinTableName = directedJoinTableName(this.table, other);
			String sql = "select * from " + joinTableName + " where parent_id=?";
			StatementWrapper ps = db.prepareStatement(sql);
				ps.setInt(1, this.id);
				ResultSet rs = ps.executeQuery();
				try {
					if (rs.next()) {
						ret = true;
					}
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
		return ret;
	}

	/**
	 * Creates a new undirected join entry between two database rows.
	 * @param first
	 * @param second
	 * @param db
	 */
	public static void joinRows(SoarDatabaseRow first, SoarDatabaseRow second,
			SoarDatabaseConnection db) {

		// First, make sure that the rows are joinable.
		if (!tablesAreJoined(first.table, second.table)) {
			// TODO
			// figure out why this happens
			return;
		}

		// Make sure these rows aren't already joined.
		boolean alreadyJoined = rowsAreJoined(first, second, db);
		if (alreadyJoined) {
			return;
		}

		// Create new join row.
		String joinTableName = joinTableName(first.table, second.table);
		Table[] orderedTables = orderJoinedTables(first.table, second.table);
		String firstTableIdName = orderedTables[0] == first.table ? "first_id" : "second_id";
		String secondTableIdName = orderedTables[0] == first.table ? "second_id" : "first_id";
		String sql = "insert into " + joinTableName + " (" + firstTableIdName + ", " + secondTableIdName + ") values " + "(?,?)";
		StatementWrapper ps = db.prepareStatement(sql);
		ps.setInt(1, first.id);
		ps.setInt(2, second.id);
		ps.execute();
	}
	
	/**
	 * Removes an undirected join between the two rows, if one exists.
	 * Order of parameters doesn't matter.
	 */
	public static void unjoinRows(SoarDatabaseRow firstRow,
			SoarDatabaseRow secondRow, SoarDatabaseConnection db) {
		Table[] tables = SoarDatabaseRow.orderJoinedTables(firstRow.getTable(),
				secondRow.getTable());
		if (tables == null) return;
		if (tables[0] != firstRow.getTable()) {
			// Swap first and second rows.
			SoarDatabaseRow temp = firstRow;
			firstRow = secondRow;
			secondRow = temp;
		}
		boolean sameTable = (tables[0] == tables[1]);
		String sql = null;
		if (sameTable) {
			sql = "delete from " + SoarDatabaseRow.joinTableName(tables[0], tables[1]) + " where (first_id=? and second_id=?) or (second_id=? and first_id=?)";
		} else {
			sql = "delete from " + SoarDatabaseRow.joinTableName(tables[0], tables[1]) + " where first_id=? and second_id=?";
		}
		StatementWrapper ps = db.prepareStatement(sql);
		ps.setInt(1, firstRow.id);
		ps.setInt(2, secondRow.id);
		if (sameTable) {
			ps.setInt(3, firstRow.id);
			ps.setInt(4, secondRow.id);
		}
		ps.execute();
	}

	/**
	 * Creates a directed-joined connection between the given rows.
	 * @param parent
	 * @param child
	 * @param db
	 */
	public static void directedJoinRows(SoarDatabaseRow parent, SoarDatabaseRow child, SoarDatabaseConnection db) {
		if (!tablesAreDirectedJoined(parent.getTable(), child.getTable())) {
			return;
		}
		if (rowsAreDirectedJoined(parent, child, parent.getDatabaseConnection())) {
			return;
		}
		String joinTable = directedJoinTableName(parent.getTable(), child.getTable());
		String sql = "insert into " + joinTable + " (parent_id, child_id) values (?,?)";
		StatementWrapper ps = db.prepareStatement(sql);
		ps.setInt(1, parent.id);
		ps.setInt(2, child.id);
		ps.execute();
	}
	
	/**
	 * Removes the directed join between the given rows, if one exists.
	 * @param parent
	 * @param child
	 * @param db
	 */
	public static void directedUnjoinRows(SoarDatabaseRow parent, SoarDatabaseRow child, SoarDatabaseConnection db) {
		if (!tablesAreDirectedJoined(parent.getTable(), child.getTable())) {
			return;
		}
		String joinTable = directedJoinTableName(parent.getTable(), child.getTable());
		String sql = "delete from " + joinTable + " where parent_id=? and child_id=?";
		StatementWrapper ps = db.prepareStatement(sql);
		ps.setInt(1, parent.id);
		ps.setInt(2, child.id);
		ps.execute();
		
		// If the child is as orphan, delete the child.
		if (child != parent && child.isOrphan()) {
			child.deleteAllChildren(true, null);
		}
	}
	
	/**
	 * Creates and returns a new row as a child of this row.
	 * 
	 * @param childTable
	 *            The type of child row to create.
	 * @param name
	 *            The name of the new row.
	 * @return The new row.
	 */
	public SoarDatabaseRow createChild(Table childTable, String name) {
		db.createChild(this, childTable, name);

		// Get the row
		String sql = "select * from " + childTable.tableName()
				+ " where id=(last_insert_rowid())";
		StatementWrapper ps = db.prepareStatement(sql);
		int id = -1;
		try {
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				id = rs.getInt("id");
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// Should have been assigned to recently created row's ID.
		assert id != -1;

		SoarDatabaseRow ret = new SoarDatabaseRow(childTable, id, db);

		// Check and see if there are children that should be automatically
		// generated also.
		ArrayList<Table> automaticChildren = SoarDatabaseRow.automaticChildren.get(childTable);
		if (automaticChildren != null) {
			for (Table t : automaticChildren) {
				String newName = null;
				if (t == Table.DATAMAP_IDENTIFIERS) {
					// special case for root datamap node
					newName = "<s>";
				} else {
					newName = "New " + t.shortName();
				}
				ret.createChild(t, newName);
			}
		}
		
		// Fill in default values for editable columns
		ArrayList<EditableColumn> columns = editableColumns.get(childTable);
		if (columns != null) {
			for (EditableColumn column : columns) {
				ret.editColumnValue(column, column.getDefaultValue());
			}
		}

		return ret;
	}
	
	/**
	 * Creates a new row.
	 * The new row isn't a normal child of this row. Instead,
	 * the new row is connected to this row by a directed join.
	 * @param childTable
	 * @param name
	 * @return
	 */
	public SoarDatabaseRow createJoinedChild(Table childTable, String name) {
		
		db.insert(childTable, new String[][] {{ "name" , "\"" + name + "\"" }});

		// Get the row
		String sql = "select * from " + childTable.tableName()
				+ " where id=(last_insert_rowid())";
		StatementWrapper ps = db.prepareStatement(sql);
		int id = -1;
		try {
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				id = rs.getInt("id");
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// Should have been assigned to recently created row's ID.
		assert id != -1;

		SoarDatabaseRow ret = new SoarDatabaseRow(childTable, id, db);
		
		// Directed-join the rows
		directedJoinRows(this, ret, db);

		// Check and see if there are children that should be automatically
		// generated also.
		ArrayList<Table> automaticChildren = SoarDatabaseRow.automaticChildren.get(childTable);
		if (automaticChildren != null) {
			for (Table t : automaticChildren) {
				ret.createChild(t, "New " + t.shortName());
			}
		}
		
		// Fill in default values for editable columns
		ArrayList<EditableColumn> columns = editableColumns.get(childTable);
		if (columns != null) {
			for (EditableColumn column : columns) {
				ret.editColumnValue(column, column.getDefaultValue());
			}
		}

		return ret;
	}

	/**
	 * Turns the given AST Node into database row children of this row.
	 * @param node
	 * @throws Exception
	 */
	public void createChildrenFromAstNode(Object node) throws Exception {
		ArrayList<Table> childTables = getChildTables();
		Table childTable = null;
		try {
			childTable = tableForAstNode.get(node.getClass());
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		
		SqlArgsAndChildNodes argsAndNodes = sqlArgsAndChildNodes(node,
				childTable);
		SoarDatabaseRow childRow;

		if (childTable == this.table) {
			// This should only happen when 'node' is a SoarProductionAST and
			// this is already a 'Rule'
			childRow = this;
		} else if (childTables.contains(childTable)) {
			// create databse entry
			String[][] sqlArgs = argsAndNodes.sqlArgs;
			db.createChild(this, childTable, sqlArgs);

			// Get created row
			String sql = "select * from " + childTable.tableName()
					+ " where id=(last_insert_rowid())";
			StatementWrapper ps = db.prepareStatement(sql);
			int id = -1;
			try {
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					id = rs.getInt("id");
				}
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			// Should have been assigned to recently created row's ID.
			assert id != -1;

			childRow = new SoarDatabaseRow(childTable, id, db);
		} else {
			throw new Exception("No child table of current type \""
					+ table.shortName() + "\" for AST node \"" + childTable
					+ "\"");
		}

		// recursively create new entries
		Object[] childNodes = argsAndNodes.childNodes;
		for (Object childNode : childNodes) {
			childRow.createChildrenFromAstNode(childNode);
		}
	}

	/**
	 * Deletes all children.
	 * Also deletes directed-joined children if
	 * the child has no parents (normal parents or directed-join parents).
	 * @param alsoDeleteThis If true, deleted this row also.
	 */
	public void deleteAllChildren(boolean alsoDeleteThis, HashSet<SoarDatabaseRow> alreadyDeleted, IProgressMonitor monitor) {
		alreadyDeleted.add(this);
		ArrayList<ISoarDatabaseTreeItem> childRows = getChildren(false, true, false, false, false, true);
		for (ISoarDatabaseTreeItem child : childRows) {
			if (child instanceof SoarDatabaseRow && !alreadyDeleted.contains(child)) {
				alreadyDeleted.add((SoarDatabaseRow)child);
				((SoarDatabaseRow) child).deleteAllChildren(true, alreadyDeleted, monitor);
			}
		}
		ArrayList<ISoarDatabaseTreeItem> childJoinedRows = getDirectedJoinedChildren(false);
		for (ISoarDatabaseTreeItem child : childJoinedRows) {
			if (child instanceof SoarDatabaseRow) {
				SoarDatabaseRow childRow = (SoarDatabaseRow) child;
				directedUnjoinRows(this, childRow, db);
				if (childRow.isOrphan() && !alreadyDeleted.contains(childRow)) {
					alreadyDeleted.add(childRow);
					childRow.deleteAllChildren(true, alreadyDeleted, monitor);
				}
			}
		}
		if (alsoDeleteThis) {
			boolean exists = exists();
			String message = "Deleted \"" + getName() + "\"";
			delete();
			if (exists && monitor != null && !table.isAstTable()) {
				monitor.subTask(message);
			}
		}
	}
	
	/**
	 * Deletes all children of this row.
	 * @param alsoDeleteThis If true, also deletes this row.
	 * @param monitor The progress monitor to display updates to, or <code>null</code>.
	 */
	public void deleteAllChildren(boolean alsoDeleteThis, IProgressMonitor monitor) {
		if (monitor != null) {
			monitor.beginTask("Deleting \"" + getName() + "\"", IProgressMonitor.UNKNOWN);
		}
		boolean eventsWereSuppressed = db.getSupressEvents();
		db.setSupressEvents(true);
		deleteAllChildren(alsoDeleteThis, new HashSet<SoarDatabaseRow>(), monitor);
		db.setSupressEvents(eventsWereSuppressed);
		db.fireEvent(new SoarDatabaseEvent(Type.DATABASE_CHANGED));
		if (monitor != null) {
			monitor.done();
		}
	}
	
	/**
	 * Return the list of editable columns for this row's table.
	 * @return
	 */
	public ArrayList<EditableColumn> getEditableColumns() {
		if (editableColumns.containsKey(table)) {
			ArrayList<EditableColumn> ret = editableColumns.get(table);
			if (ret != null) {
				return ret;
			}
		}
		return new ArrayList<EditableColumn>();
	}
	
	/**
	 * 
	 * @param column
	 * @return The current value of the given editable column for this row.
	 */
	public Object getEditableColumnValue(EditableColumn column) {
		String sql = "Select " + column.getName() + " from " + table.tableName() + " where id=?";
		StatementWrapper sw = db.prepareStatement(sql);
		sw.setInt(1, id);
		ResultSet rs = sw.executeQuery();
		Object ret = null;
		try {
			if (rs.next()) {
				ret = rs.getObject(column.getName());
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		sw.close();
		return ret;
	}
	
	/**
	 * Changes the value of the given editable column in this row.
	 * @param column The editable column to change the value of.
	 * @param newValue The new value for the editable column.
	 */
	public void editColumnValue(EditableColumn column, Object newValue) {
		if (column.objectIsRightType(newValue)) {
			String sql = "update " + table.tableName() + " set " + column.getName() + "=? where id=?";
			StatementWrapper sw = db.prepareStatement(sql);
			switch (EditableColumn.typeForObject(newValue)) {
			case FLOAT:
				sw.setFloat(1, (Float)newValue);
				break;
			case INTEGER:
				sw.setInt(1, (Integer)newValue);
				break;
			case STRING:
				sw.setString(1, (String)newValue);
				break;
			}
			sw.setInt(2, id);
			sw.execute();
		}
	}

	/**
	 * Encapsulates information neccesary to add a row to the database.
	 * 
	 * @author miller
	 * 
	 */
	private class SqlArgsAndChildNodes {
		public String[][] sqlArgs;
		public Object[] childNodes;

		public SqlArgsAndChildNodes(ArrayList<String[]> sqlArgs,
				Object[] childNodes) {
			int argsLength = sqlArgs.size();
			this.sqlArgs = new String[argsLength][2];
			for (int i = 0; i < sqlArgs.size(); ++i) {
				String[] pair = sqlArgs.get(i);
				this.sqlArgs[i][0] = pair[0];
				this.sqlArgs[i][1] = pair[1];
			}
			this.childNodes = childNodes;
		}
	}

	/**
	 * Takes a Java Object from a Soar Production AST, and its corresponding
	 * Table, and returns a SqlArgsAndChildNodes that encapsulates the
	 * information required to add a row to the database.
	 * 
	 * @param node
	 * @param nodeTable
	 * @return
	 */
	private SqlArgsAndChildNodes sqlArgsAndChildNodes(Object node,
			Table nodeTable) {

		ArrayList<String[]> sqlArgs = new ArrayList<String[]>();
		Object[] childNodes = new Object[0]; // = null;

		String name = nodeTable.shortName();

		String sqlTrue = "1";
		String sqlFalse = "0";

		if (node instanceof SoarProductionAst) {
			Object[] conditions = ((SoarProductionAst) node).getConditions()
					.toArray();
			Object[] actions = ((SoarProductionAst) node).getActions()
					.toArray();
			childNodes = new Object[conditions.length + actions.length];
			System.arraycopy(conditions, 0, childNodes, 0, conditions.length);
			System.arraycopy(actions, 0, childNodes, conditions.length,
					actions.length);
		} else if (node instanceof Condition) {
			Object positiveCondition = ((Condition) node)
					.getPositiveCondition();
			childNodes = new Object[] { positiveCondition };
			boolean isNegated = ((Condition) node).isNegated();
			sqlArgs.add(new String[] { "is_negated",
					isNegated ? sqlTrue : sqlFalse });
			if (isNegated)
				name += " (negated)";
		} else if (node instanceof PositiveCondition) {
			boolean isConjunction = ((PositiveCondition) node).isConjunction();
			sqlArgs.add(new String[] { "is_conjunction",
					isConjunction ? sqlTrue : sqlFalse });
			if (isConjunction) {
				childNodes = ((PositiveCondition) node).getConjunction()
						.toArray();
			} else {
				childNodes = new Object[] { ((PositiveCondition) node)
						.getConditionForOneIdentifier() };
			}
			if (isConjunction)
				name += " (conjunction)";
		} else if (node instanceof ConditionForOneIdentifier) {
			childNodes = ((ConditionForOneIdentifier) node)
					.getAttributeValueTests().toArray();
			boolean hasState = ((ConditionForOneIdentifier) node).hasState();
			sqlArgs.add(new String[] { "has_state",
					hasState ? sqlTrue : sqlFalse });
			Pair pair = ((ConditionForOneIdentifier) node).getVariable();
			String variable = pair.getString();
			sqlArgs.add(new String[] { "variable", "\"" + variable + "\"" });
			name += " (variable: " + variable + ")";
			if (hasState)
				name += " (has state)";
		} else if (node instanceof AttributeValueTest) {
			Object[] attributeTests = ((AttributeValueTest) node)
					.getAttributeTests().toArray();
			Object[] valueTests = ((AttributeValueTest) node).getValueTests()
					.toArray();
			childNodes = new Object[attributeTests.length + valueTests.length];
			System.arraycopy(attributeTests, 0, childNodes, 0,
					attributeTests.length);
			System.arraycopy(valueTests, 0, childNodes, attributeTests.length,
					valueTests.length);
			sqlArgs.add(new String[] {
					"is_negated",
					((AttributeValueTest) node).isNegated() ? sqlTrue
							: sqlFalse });
		} else if (node instanceof AttributeTest) {
			childNodes = new Object[] { ((AttributeTest) node).getTest() };
		} else if (node instanceof Test) {
			boolean isConjunctiveTest = ((Test) node).isConjunctiveTest();
			sqlArgs.add(new String[] { "is_conjunctive_test",
					isConjunctiveTest ? sqlTrue : sqlFalse });
			if (isConjunctiveTest) {
				childNodes = new Object[] { ((Test) node).getConjunctiveTest() };
			} else {
				childNodes = new Object[] { ((Test) node).getSimpleTest() };
			}
			name += isConjunctiveTest ? " (conjunctive test)"
					: " (simple test)";
		} else if (node instanceof SimpleTest) {
			boolean isDisjunctionTest = ((SimpleTest) node).isDisjunctionTest();
			sqlArgs.add(new String[] { "is_disjunction_test",
					isDisjunctionTest ? sqlTrue : sqlFalse });
			if (isDisjunctionTest) {
				childNodes = new Object[] { ((SimpleTest) node)
						.getDisjunctionTest() };
			} else {
				childNodes = new Object[] { ((SimpleTest) node)
						.getRelationalTest() };
			}
			name += isDisjunctionTest ? " (disjunction test)"
					: " (relational test)";
		} else if (node instanceof DisjunctionTest) {
			childNodes = ((DisjunctionTest) node).getConstants().toArray();
		} else if (node instanceof RelationalTest) {
			childNodes = new Object[] { ((RelationalTest) node).getSingleTest() };
			int relation = ((RelationalTest) node).getRelation();
			sqlArgs.add(new String[] { "relation", "" + relation });
			name += " (" + RelationalTest.RELATIONS[relation] + ")";
		} else if (node instanceof SingleTest) {
			boolean isConstant = ((SingleTest) node).isConstant();
			sqlArgs.add(new String[] { "is_constant",
					isConstant ? sqlTrue : sqlFalse });
			if (isConstant) {
				childNodes = new Object[] { ((SingleTest) node).getConstant() };
				name += " (constant)";
			} else {
				Pair pair = ((SingleTest) node).getVariable();
				String variable = pair.getString();
				sqlArgs.add(new String[] { "variable", "\"" + variable + "\"" });
				name += " (variable: " + variable + ")";
			}
		} else if (node instanceof ConjunctiveTest) {
			childNodes = ((ConjunctiveTest) node).getSimpleTests().toArray();
		} else if (node instanceof ValueTest) {
			boolean isStructuredValueNotation = ((ValueTest) node).isStructuredValueNotation();
			sqlArgs.add(new String[] { "is_structured_value_notation", isStructuredValueNotation ? sqlTrue : sqlFalse });
			if (isStructuredValueNotation) {
				name += " (structured-value notation)";
				childNodes = ((ValueTest) node).getAttributeValueTests().toArray();
				Pair variablePair = ((ValueTest) node).getVariable();
				if (variablePair != null) {
					String variable = variablePair.getString();
					if (variable != null && variable.length() > 0) {
						sqlArgs.add(new String[] { "variable", "\"" + variable + "\"" });
						name += " (variable: " + variable + ")";
					}
				}
			} else {
				childNodes = new Object[] { ((ValueTest) node).getTest() };
				boolean hasAcceptablePreference = ((ValueTest) node).hasAcceptablePreference();
				sqlArgs.add(new String[] { "has_acceptable_preference", hasAcceptablePreference ? sqlTrue : sqlFalse });
				if (hasAcceptablePreference) name += " (has acceptable preference)";
			}
		} else if (node instanceof Action) {
			boolean isVarAttrValMake = ((Action) node).isVarAttrValMake();
			sqlArgs.add(new String[] { "is_var_attr_val_make",
					isVarAttrValMake ? sqlTrue : sqlFalse });
			if (isVarAttrValMake) {
				childNodes = new Object[] { ((Action) node).getVarAttrValMake() };
			} else {
				childNodes = new Object[] { ((Action) node).getFunctionCall() };
			}
			name += isVarAttrValMake ? " (variable attribute make)"
					: " (function call)";
		} else if (node instanceof VarAttrValMake) {
			childNodes = ((VarAttrValMake) node).getAttributeValueMakes()
					.toArray();
			Pair pair = ((VarAttrValMake) node).getVariable();
			String variable = pair.getString();
			sqlArgs.add(new String[] { "variable", "\"" + variable + "\"" });
			name += " (variable: " + variable + ")";
		} else if (node instanceof AttributeValueMake) {
			Object[] rhsValues = ((AttributeValueMake) node).getRHSValues()
					.toArray();
			Object[] valueMakes = ((AttributeValueMake) node).getValueMakes()
					.toArray();
			childNodes = new Object[rhsValues.length + valueMakes.length];
			System.arraycopy(rhsValues, 0, childNodes, 0, rhsValues.length);
			System.arraycopy(valueMakes, 0, childNodes, rhsValues.length,
					valueMakes.length);
		} else if (node instanceof RHSValue) {
			boolean isConstant = ((RHSValue) node).isConstant();
			boolean isFunctionCall = ((RHSValue) node).isFunctionCall();
			boolean isVariable = ((RHSValue) node).isVariable();
			sqlArgs.add(new String[] { "is_constant",
					isConstant ? sqlTrue : sqlFalse });
			sqlArgs.add(new String[] { "is_function_call",
					isFunctionCall ? sqlTrue : sqlFalse });
			sqlArgs.add(new String[] { "is_variable",
					isVariable ? sqlTrue : sqlFalse });
			if (isConstant) {
				childNodes = new Object[] { ((RHSValue) node).getConstant() };
				name += " (constant)";
			} else if (isFunctionCall) {
				childNodes = new Object[] { ((RHSValue) node).getFunctionCall() };
				name += " (function call)";
			} else if (isVariable) {
				Pair pair = ((RHSValue) node).getVariable();
				String variable = pair.getString();
				sqlArgs.add(new String[] { "variable", "\"" + variable + "\"" });
				name += " (variable: " + variable + ")";
			}
		} else if (node instanceof Constant) {
			int constantType = ((Constant) node).getConstantType();
			sqlArgs.add(new String[] { "constant_type", "" + constantType });
			if (constantType == Constant.FLOATING_CONST) {
				float floatingConst = ((Constant) node).getFloatConst();
				sqlArgs.add(new String[] { "floating_const",
								"" + floatingConst });
				name += " (floating constant: " + floatingConst + ")";
			} else if (constantType == Constant.INTEGER_CONST) {
				int intConst = ((Constant) node).getIntConst();
				sqlArgs.add(new String[] { "integer_const", "" + intConst });
				name += " (integer constant: " + intConst + ")";
			} else if (constantType == Constant.SYMBOLIC_CONST) {
				String symConst = ((Constant) node).getSymConst();
				sqlArgs.add(new String[] { "symbolic_const",
						"\"" + symConst + "\"" });
				name += " (symbolic constant: " + symConst + ")";
			}
		} else if (node instanceof FunctionCall) {
			childNodes = ((FunctionCall) node).getRHSValues().toArray();
			Pair pair = ((FunctionCall) node).getFunctionName();
			String variable = pair.getString();
			sqlArgs.add(new String[] { "function_name", "\"" + variable + "\"" });
			name += " (variable: " + variable + ")";
		} else if (node instanceof ValueMake) {
			Object[] preferenceSpecifiers = objectsArrayFromIterator(((ValueMake) node)
					.getPreferenceSpecifiers());
			Object[] rhsValue = new Object[] { ((ValueMake) node).getRHSValue() };
			childNodes = new Object[preferenceSpecifiers.length
					+ rhsValue.length];
			System.arraycopy(preferenceSpecifiers, 0, childNodes, 0,
					preferenceSpecifiers.length);
			System.arraycopy(rhsValue, 0, childNodes,
					preferenceSpecifiers.length, rhsValue.length);
		} else if (node instanceof PreferenceSpecifier) {
			boolean isUnaryPreference = ((PreferenceSpecifier) node).isUnaryPreference();
			if (!isUnaryPreference) {
				childNodes = new Object[] { ((PreferenceSpecifier) node).getRHS() };
			}
			int preferenceSpecifierType = ((PreferenceSpecifier) node).getPreferenceSpecifierType();
			sqlArgs.add(new String[] { "is_unary_preference", isUnaryPreference ? sqlTrue : sqlFalse });
			sqlArgs.add(new String[] { "preference_specifier_type", "" + preferenceSpecifierType });
			if (isUnaryPreference) {
				name += " (unary preference)";
			}
			name += " (preference: "
					+ PreferenceSpecifier.PREFERENCES[preferenceSpecifierType]
					+ ")";
		}

		// Every row has a name -- this could change later
		sqlArgs.add(new String[] { "name", "\"" + name + "\"" });

		SqlArgsAndChildNodes ret = new SqlArgsAndChildNodes(sqlArgs, childNodes);
		return ret;
	}

	/**
	 * 
	 * @param <T>
	 * @param it
	 * @return An Object array from the given iterator.
	 */
	private <T> Object[] objectsArrayFromIterator(Iterator<T> it) {
		ArrayList<T> list = new ArrayList<T>();
		while (it.hasNext()) {
			list.add(it.next());
		}
		return list.toArray();
	}

	/**
	 * Static initalization.
	 * 
	 * Initalizes data about realtionships between tables.
	 * 
	 * Would have been better to use a static block, but hey, this works too. 
	 */
	private static void init() {

		// table problem spaces has foreign key agent_id:
		addParent(Table.PROBLEM_SPACES, Table.AGENTS);
		addParent(Table.OPERATORS, Table.AGENTS);
		addParent(Table.RULES, Table.AGENTS);
		addParent(Table.TAGS, Table.AGENTS);

		// Which tables have folders
		ArrayList<Table> agentFolders = new ArrayList<Table>();
		agentFolders.add(Table.PROBLEM_SPACES);
		agentFolders.add(Table.OPERATORS);
		agentFolders.add(Table.RULES);
		agentFolders.add(Table.TAGS);
		childFolders.put(Table.AGENTS, agentFolders);
		
		// Which tables have editable columns
		addEditableColumnToTable(Table.DATAMAP_INTEGERS, new EditableColumn("min_value", EditableColumn.Type.INTEGER, new Integer(0)));
		addEditableColumnToTable(Table.DATAMAP_INTEGERS, new EditableColumn("max_value", EditableColumn.Type.INTEGER, new Integer(0)));
		addEditableColumnToTable(Table.DATAMAP_FLOATS, new EditableColumn("min_value", EditableColumn.Type.FLOAT, new Float(0.0f)));
		addEditableColumnToTable(Table.DATAMAP_FLOATS, new EditableColumn("max_value", EditableColumn.Type.FLOAT, new Float(0.0f)));

		// Project structure
		directedJoinTables(Table.PROBLEM_SPACES, Table.RULES); // Project
		directedJoinTables(Table.OPERATORS, Table.RULES);
		directedJoinTables(Table.PROBLEM_SPACES, Table.OPERATORS);
		directedJoinTables(Table.OPERATORS, Table.PROBLEM_SPACES);
		joinTables(Table.TAGS, Table.PROBLEM_SPACES); // Tags
		joinTables(Table.TAGS, Table.OPERATORS);
		joinTables(Table.TAGS, Table.RULES);
		
		// For linked attributes
		joinTables(Table.DATAMAP_IDENTIFIERS, Table.DATAMAP_IDENTIFIERS);
		joinTables(Table.DATAMAP_INTEGERS, Table.DATAMAP_INTEGERS);
		joinTables(Table.DATAMAP_FLOATS, Table.DATAMAP_FLOATS);
		joinTables(Table.DATAMAP_STRINGS, Table.DATAMAP_STRINGS);
		joinTables(Table.DATAMAP_ENUMERATIONS, Table.DATAMAP_ENUMERATIONS);
		
		// Declare directional joined tables.
		directedJoinTables(Table.DATAMAP_IDENTIFIERS, Table.DATAMAP_IDENTIFIERS);
		directedJoinTables(Table.DATAMAP_IDENTIFIERS, Table.DATAMAP_ENUMERATIONS);
		directedJoinTables(Table.DATAMAP_IDENTIFIERS, Table.DATAMAP_INTEGERS);
		directedJoinTables(Table.DATAMAP_IDENTIFIERS, Table.DATAMAP_FLOATS);
		directedJoinTables(Table.DATAMAP_IDENTIFIERS, Table.DATAMAP_STRINGS);
		directedJoinTables(Table.PROBLEM_SPACES, Table.PROBLEM_SPACES); // Superstate relationships

		// Triples
		addParent(Table.TRIPLES, Table.RULES);
		directedJoinTables(Table.TRIPLES, Table.TRIPLES);
		
		// rule structure
		addParent(Table.CONDITIONS, Table.RULES);
		addParent(Table.POSITIVE_CONDITIONS, Table.CONDITIONS);
		addParent(Table.CONDITION_FOR_ONE_IDENTIFIERS, Table.POSITIVE_CONDITIONS);
		addParent(Table.CONDITIONS, Table.POSITIVE_CONDITIONS);
		addParent(Table.ATTRIBUTE_VALUE_TESTS, Table.CONDITION_FOR_ONE_IDENTIFIERS);
		addParent(Table.ATTRIBUTE_VALUE_TESTS, Table.VALUE_TESTS);
		addParent(Table.ATTRIBUTE_TESTS, Table.ATTRIBUTE_VALUE_TESTS);
		addParent(Table.VALUE_TESTS, Table.ATTRIBUTE_VALUE_TESTS);
		addParent(Table.VALUE_TESTS, Table.ATTRIBUTE_TESTS);
		addParent(Table.TESTS, Table.VALUE_TESTS);
		addParent(Table.TESTS, Table.ATTRIBUTE_TESTS);
		addParent(Table.CONJUNCTIVE_TESTS, Table.TESTS);
		addParent(Table.SIMPLE_TESTS, Table.CONJUNCTIVE_TESTS);
		addParent(Table.SIMPLE_TESTS, Table.TESTS);
		addParent(Table.DISJUNCTION_TESTS, Table.SIMPLE_TESTS);
		addParent(Table.RELATIONAL_TESTS, Table.SIMPLE_TESTS);
		
		//not used
		//addParent(Table.RELATIONS, Table.RELATIONAL_TESTS);
		
		addParent(Table.SINGLE_TESTS, Table.RELATIONAL_TESTS);
		addParent(Table.CONSTANTS, Table.SINGLE_TESTS);
		addParent(Table.CONSTANTS, Table.DISJUNCTION_TESTS);
		addParent(Table.CONSTANTS, Table.RHS_VALUES);
		addParent(Table.ACTIONS, Table.RULES);
		addParent(Table.VAR_ATTR_VAL_MAKES, Table.ACTIONS);
		addParent(Table.ATTRIBUTE_VALUE_MAKES, Table.VAR_ATTR_VAL_MAKES);
		addParent(Table.FUNCTION_CALLS, Table.ACTIONS);
		addParent(Table.FUNCTION_NAMES, Table.FUNCTION_CALLS);
		addParent(Table.FUNCTION_CALLS, Table.RHS_VALUES);
		addParent(Table.RHS_VALUES, Table.FUNCTION_CALLS);
		addParent(Table.RHS_VALUES, Table.VALUE_MAKES);
		addParent(Table.RHS_VALUES, Table.ATTRIBUTE_VALUE_MAKES);
		addParent(Table.VALUE_MAKES, Table.ATTRIBUTE_VALUE_MAKES);
		//addParent(Table.PREFERENCES, Table.VALUE_MAKES);
		addParent(Table.PREFERENCE_SPECIFIERS, Table.VALUE_MAKES);
		//addParent(Table.NATURALLY_UNARY_PREFERENCES, Table.PREFERENCE_SPECIFIERS);
		//addParent(Table.BINARY_PREFERENCES, Table.PREFERENCE_SPECIFIERS);
		//addParent(Table.BINARY_PREFERENCES, Table.FORCED_UNARY_PREFERENCES);
		//addParent(Table.FORCED_UNARY_PREFERENCES, Table.PREFERENCE_SPECIFIERS);

		// datamap structure
		addParent(Table.DATAMAP_IDENTIFIERS, Table.PROBLEM_SPACES);
		addParent(Table.DATAMAP_ENUMERATION_VALUES, Table.DATAMAP_ENUMERATIONS);

		// Automatic children
		ArrayList<Table> children = new ArrayList<Table>();
		children.add(Table.DATAMAP_IDENTIFIERS);
		automaticChildren.put(Table.PROBLEM_SPACES, children);

		// Table / ast object pairs
		tableForAstNode.put(SoarProductionAst.class, Table.RULES);
		tableForAstNode.put(Condition.class, Table.CONDITIONS);
		tableForAstNode.put(PositiveCondition.class, Table.POSITIVE_CONDITIONS);
		tableForAstNode.put(ConditionForOneIdentifier.class, Table.CONDITION_FOR_ONE_IDENTIFIERS);
		tableForAstNode.put(AttributeValueTest.class, Table.ATTRIBUTE_VALUE_TESTS);
		tableForAstNode.put(AttributeTest.class, Table.ATTRIBUTE_TESTS);
		tableForAstNode.put(ValueTest.class, Table.VALUE_TESTS);
		tableForAstNode.put(Test.class, Table.TESTS);
		tableForAstNode.put(ConjunctiveTest.class, Table.CONJUNCTIVE_TESTS);
		tableForAstNode.put(SimpleTest.class, Table.SIMPLE_TESTS);
		tableForAstNode.put(DisjunctionTest.class, Table.DISJUNCTION_TESTS);
		tableForAstNode.put(RelationalTest.class, Table.RELATIONAL_TESTS);

		// No ast class 'Relation'
		// tableForAstElement.put(Relation.class, Table.RELTAIONS);

		tableForAstNode.put(SingleTest.class, Table.SINGLE_TESTS);
		tableForAstNode.put(Constant.class, Table.CONSTANTS);
		tableForAstNode.put(Action.class, Table.ACTIONS);
		tableForAstNode.put(VarAttrValMake.class, Table.VAR_ATTR_VAL_MAKES);
		tableForAstNode.put(AttributeValueMake.class, Table.ATTRIBUTE_VALUE_MAKES);
		tableForAstNode.put(FunctionCall.class, Table.FUNCTION_CALLS);

		// no ast class 'FunctionName'
		// tableForAstElement.put(FunctionName.class, Table.FUNCTION_NAMES);

		tableForAstNode.put(RHSValue.class, Table.RHS_VALUES);
		tableForAstNode.put(ValueMake.class, Table.VALUE_MAKES);

		// No ast class 'Preference'
		// ValueMake goes right to a list of PreferenceSpecifiers 
		// tableForAstElement.put(Preference.class, Table.PREFERENCES);

		tableForAstNode.put(PreferenceSpecifier.class,
				Table.PREFERENCE_SPECIFIERS);
		tableForAstNode.put(NaturallyUnaryPreference.class,
				Table.PREFERENCE_SPECIFIERS);
		tableForAstNode.put(BinaryPreference.class, Table.PREFERENCE_SPECIFIERS);
		tableForAstNode.put(ForcedUnaryPreference.class,
				Table.PREFERENCE_SPECIFIERS);

		initted = true;
	}

	/**
	 * Indicate that table child has foreign key parent_id
	 * 
	 * @param child
	 *            The child table
	 * @param parent
	 *            The parent table
	 */
	private static void addParent(Table child, Table parent) {
		if (!parentTables.keySet().contains(child)) {
			ArrayList<Table> newList = new ArrayList<Table>();
			parentTables.put(child, newList);
		}
		ArrayList<Table> parents = parentTables.get(child);
		parents.add(parent);
		addChild(parent, child);
	}

	/**
	 * Indicate that table child has foreign key parent_id
	 * 
	 * @param child
	 *            The child table
	 * @param parent
	 *            The parent table
	 */
	private static void addChild(Table parent, Table child) {
		if (!childTables.keySet().contains(parent)) {
			ArrayList<Table> newList = new ArrayList<Table>();
			childTables.put(parent, newList);
		}
		ArrayList<Table> children = childTables.get(parent);
		children.add(child);
	}

	/**
	 * Indicate that the two tables are joined.
	 * Order of parameters should match the order in the name of the sql table.
	 * 
	 * @param first
	 * @param second
	 */
	private static void joinTables(Table first, Table second) {
		ArrayList<Table> list;

		if (joinedTables.containsKey(first)) {
			list = joinedTables.get(first);
		} else {
			list = new ArrayList<Table>();
		}
		if (!list.contains(second)) {
			list.add(second);
		}
		joinedTables.put(first, list);
	}

	/**
	 * Order of parameters shouldn't matter.
	 * 
	 * @param first
	 * @param second
	 * @return True if the two tables are undirected-joined.
	 */
	public static boolean tablesAreJoined(Table first, Table second) {
		if (joinedTables.containsKey(first)) {
			ArrayList<Table> list = joinedTables.get(first);
			if (list.contains(second)) {
				return true;
			}
		}
		if (joinedTables.containsKey(second)) {
			ArrayList<Table> list = joinedTables.get(second);
			if (list.contains(first)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @param parent
	 * @param child
	 * @return True if the tables are directed-joined.
	 */
	public static boolean tablesAreDirectedJoined(Table parent, Table child) {
		if (directedJoinedTables.containsKey(parent)) {
			if (directedJoinedTables.get(parent).contains(child)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Indicates that the two tables are directed-joined.
	 * Order of parameters should match the order in the name of the sql table.
	 * 
	 * @param first
	 * @param second
	 */
	private static void directedJoinTables(Table parent, Table child) {
		ArrayList<Table> list;

		if (directedJoinedTables.containsKey(parent)) {
			list = directedJoinedTables.get(parent);
		} else {
			list = new ArrayList<Table>();
		}
		if (!list.contains(child)) {
			list.add(child);
		}
		directedJoinedTables.put(parent, list);
	}

	/**
	 * Returns a list of all tables that are undirected-joined to the given table.
	 * 
	 * @param table
	 * @return
	 */
	public static ArrayList<Table> getTablesJoinedToTable(Table table) {
		ArrayList<Table> ret = new ArrayList<Table>();
		for (Table key : joinedTables.keySet()) {
			ArrayList<Table> list = joinedTables.get(key);
			if (key == table) {
				ret.addAll(list);
			} else if (list.contains(table)) {
				ret.add(key);
			}
		}
		return ret;
	}
	
	/**
	 * Returns a list of all tables that are directed-joined children of this table.
	 * 
	 * @param table
	 * @return
	 */
	public static ArrayList<Table> getChildTablesDirectedJoinedToTable(Table table) {
		if (directedJoinedTables.containsKey(table)) {
			ArrayList<Table> ret = directedJoinedTables.get(table);
			return ret;
		}
		return new ArrayList<Table>();
	}
	 
	/**
	 * I'm counting on directedJoinedTables being pretty small, so the inefficiency of this
	 * method won't hurt too much.
	 * @return A list of tables that are directed-joined parents of the given table.
	 */
	public static ArrayList<Table> getDirectedJoinParentTables(Table table) {
		ArrayList<Table> ret = new ArrayList<Table>();
		for (Table t : directedJoinedTables.keySet()) {
			if (directedJoinedTables.get(t).contains(table)) {
				ret.add(t);
			}
		}
		return ret;
	}

	/**
	 * 
	 * @param first
	 * @param second
	 * @param db
	 * @return True if the two rows are undirected-joined.
	 */
	public static boolean rowsAreJoined(SoarDatabaseRow first,
			SoarDatabaseRow second, SoarDatabaseConnection db) {

		boolean ret = false;
		
		String joinTableName = joinTableName(first.table, second.table);
		if (joinTableName == null) {
			// The tables are not joined.
			return ret;
		}
		
		Table[] orderedTables = orderJoinedTables(first.table, second.table);
		String firstTableIdName = orderedTables[0] == first.table ? "first_id" : "second_id";
		String secondTableIdName = orderedTables[0] == first.table ? "second_id" : "first_id";

		boolean sameTable = first.getTable() == second.getTable();

		String sql = null;

		if (!sameTable) {
			sql = "select * from " + joinTableName + " where " + firstTableIdName + "=? and " + secondTableIdName + "=?";
		} else {
			sql = "select * from " + joinTableName + " where (" + firstTableIdName + "=? and " + secondTableIdName + "=?) or (" + firstTableIdName + "=? and " + secondTableIdName + "=?)";
		}
		StatementWrapper ps = db.prepareStatement(sql);

		try {
			ps.setInt(1, first.id);
			ps.setInt(2, second.id);
			if (sameTable) {
				ps.setInt(3, second.id);
				ps.setInt(4, first.id);
			}
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				ret = true;
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return ret;
	}
	
	/**
	 * 
	 * @param parent
	 * @param child
	 * @param db
	 * @return True if the rows are directed-joined.
	 */
	public static boolean rowsAreDirectedJoined(SoarDatabaseRow parent,
			SoarDatabaseRow child, SoarDatabaseConnection db) {

		boolean ret = false;
		
		if (!tablesAreDirectedJoined(parent.getTable(), child.getTable())) {
			return false;
		}
		
		String tableName = directedJoinTableName(parent.getTable(), child.getTable());

		String sql = "select * from " + tableName + " where parent_id=? and child_id=?";
		
		StatementWrapper ps = db.prepareStatement(sql);

		try {
			ps.setInt(1, parent.id);
			ps.setInt(2, child.id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				ret = true;
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return ret;
	}

	/**
	 * 
	 * @param first
	 * @param second
	 * @return The given tables in the order they have been declared joined (the order
	 * their sql join table is named), for use in constructing sql queries, or null
	 * if the tables aren't joined.
	 */
	public static Table[] orderJoinedTables(Table first, Table second) {
		if (joinedTables.containsKey(first)) {
			if (joinedTables.get(first).contains(second)) {
				return new Table[] { first, second };
			}
		}
		if (joinedTables.containsKey(second)) {
			if (joinedTables.get(second).contains(first)) {
				return new Table[] { second, first };
			}
		}
		return null;
	}

	/**
	 * 
	 * @param first
	 * @param second
	 * @return The properly ordered name of the undirected-join table.
	 */
	public static String joinTableName(Table first, Table second) {
		Table[] tables = orderJoinedTables(first, second);

		// This should only be called on two tables that are joinable.
		if (tables == null) {
			return null;
		}

		return "join_" + tables[0].tableName() + "_" + tables[1].tableName();
	}
	
	/**
	 * 
	 * @param parent
	 * @param child
	 * @return The name of the join table for the given parent and child tables.
	 */
	public static String directedJoinTableName(Table parent, Table child) {
		return "directed_join_" + parent.tableName() + "_" + child.tableName();
	}
	
	/**
	 * Indicates that the given table has the given editable column.
	 * @param table
	 * @param column
	 */
	public static void addEditableColumnToTable(Table table, EditableColumn column) {
		ArrayList<EditableColumn> columns = null;
		if (editableColumns.containsKey(table)) {
			columns = editableColumns.get(table);
		}
		if (columns == null) {
			columns = new ArrayList<EditableColumn>();
			editableColumns.put(table, columns);
		}
		columns.add(column);
	}

	/**
	 * 
	 * @return True if the two rows share the same table and id.
	 */
	@Override
	public boolean equals(Object other) {
		if (other instanceof SoarDatabaseRow) {
			SoarDatabaseRow otherRow = (SoarDatabaseRow) other;
			if (otherRow.table == this.table && otherRow.id == this.id) {
					//&& otherRow.joinType == this.joinType) {
				// It's really fair to compare 'joinType', because two row objects
				// might point to the same row in the database but have different joinTypes
				// depending on their context in the IDE. However, there's some kind of problem
				// with name cacheing in trees where as long as two rows ofjects are .equal()
				// it will resuse the same label for all objects.
				return true;
			}
		}
		return false;
	}

	/**
	 * @return This row's id. Could implement a more sophisticated
	 * hashing algorithm by incorperating the table too, but this seems
	 * to work pretty well.
	 */
	@Override
	public int hashCode() {
		return id;
	}

	/**
	 * For use with text editors.
	 * @return This row's SoarDatabaseEditorInput object.
	 */
	public IEditorInput getEditorInput() {
		if (editorInput == null) {
			editorInput = new SoarDatabaseEditorInput(this);
		}
		return editorInput;
	}

	/**
	 * 
	 * @return True if this row exists in the database, i.e., it hasn't been deleted.
	 */
	public boolean exists() {
		String sql = "select * from " + getTable().tableName() + " where id=?";
		StatementWrapper ps = db.prepareStatement(sql);
		boolean ret = false;
		try {
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			ret = rs.next();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	/**
	 * 
	 * @return The test of this row, i.e., the contents of the column <code>raw_text</code>.
	 */
	public String getText() {
		String ret = null;
		if (table == Table.RULES || table == Table.AGENTS) {
			String sql = "select (raw_text) from " + table.tableName()
					+ " where id=?";
			StatementWrapper ps = db.prepareStatement(sql);
			try {
				ps.setInt(1, id);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					ret = rs.getString("raw_text");
				}
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (ret == null) {
			ret = "";
		}
		return ret;
	}

	/**
	 * Sets the text of this row (the contents of the column <code>raw_text</code>).
	 * @param text
	 * @param suppressEvents
	 */
	public void setText(String text, boolean suppressEvents) {
		if (table == Table.RULES || table == Table.AGENTS) {
			String sql = "update " + table.tableName()
					+ " set raw_text=? where id=?";
			StatementWrapper ps = db.prepareStatement(sql);
			ps.setString(1, text);
			ps.setInt(2, id);
			boolean eventsWereSuppresed = db.getSupressEvents();
			db.setSupressEvents(suppressEvents);
			ps.execute();
			db.setSupressEvents(eventsWereSuppresed);
			db.fireEvent(new SoarDatabaseEvent(Type.DATABASE_CHANGED, this));
		}
	}

	/**
	 * Saves this row.
	 * If this row is of type Table.RULES, parses the rule into a syntax tree
	 * and Triples.
	 * @param doc
	 * @param problems Output parameter
	 * @param monitor
	 */
	public void save(IDocument doc, ArrayList<SoarProblem> problems, IProgressMonitor monitor) {
		save(doc.get(), problems, monitor);
	}
	
	/**
	 * Saves this row.
	 * If this row is of type Table.RULES, parses the rule into a syntax tree
	 * and Triples.
	 * @param text
	 * @param input
	 * @param monitor Progress Monitor, or null if none exists.
	 * @return List of errors
	 */
	public ArrayList<String> save(String text, ArrayList<SoarProblem> problems, IProgressMonitor monitor) {
		ArrayList<String> errors = new ArrayList<String>();
		if (table == Table.RULES) {

			// Update raw text.
			String sql = "update " + table.tableName() + " set raw_text=? where id=?";
			StatementWrapper ps = db.prepareStatement(sql);

			ps.setString(1, text);
			ps.setInt(2, id);
			ps.setRow(this);
			ps.execute();
			
			// Remove comments
			text = removeComments(text);

			// Try to get AST from the text.
			// Make sure text starts with "sp {" and ends with "}",
			// but parse only the text inside that.
			// <hacky>
			boolean error = false;
			String trimmed = text.trim();
			if (trimmed.length() > 0) {
				int beginIndex = 0;
				int endIndex = 0;
				if (!trimmed.startsWith("sp")
						&& !trimmed.startsWith("gp")) {
					error = true;
				} else {
					beginIndex = text.indexOf("sp") + 2;
					if (beginIndex == 1) beginIndex = text.indexOf("gp") + 2;
					trimmed = text.substring(beginIndex).trim();
					if (!trimmed.startsWith("{")) {
						error = true;
					} else {
						beginIndex = text.indexOf("{", beginIndex) + 1;
						trimmed = text.substring(beginIndex).trim();
						if (!trimmed.endsWith("}")) {
							error = true;
						} else {
							endIndex = text.lastIndexOf("}");
						}
					}
				}
				// </hacky>

				if (!error) {
					// Parse the rule into an AST.
					String parseText = text.substring(beginIndex, endIndex);
					StringReader reader = new StringReader(parseText);
					SoarParser parser = new SoarParser(reader);
					try {
						SoarProductionAst ast = parser.soarProduction();
						// System.out.println("Parsed rule:\n" + ast);

						// insert into database
						boolean eventsWereSupresssed = db.getSupressEvents();
						db.setSupressEvents(true);
						deleteAllChildren(false, null);
						try {
							createChildrenFromAstNode(ast);
						} catch (Exception e) {
							e.printStackTrace();
						}
						db.setSupressEvents(eventsWereSupresssed);
						db.fireEvent(new SoarDatabaseEvent(Type.DATABASE_CHANGED, this));
					} catch (ParseException e) {
						// e.printStackTrace();
						String message = e.getLocalizedMessage();
						Token currentToken = e.currentToken;
						Token errorToken = currentToken.next;

						// Get the range of the error, based on the string
						// being parsed and the given column and row
						int start = 0;
						for (int i = 1; i < errorToken.beginLine;) {
							char c = parseText.charAt(start);
							if (c == '\n') {
								++i;
							}
							++start;
						}

						start += beginIndex;
						// -1 for columns counting starting with 1
						start += errorToken.beginColumn - 1;

						int length = 2; // (errorToken.endOffset -
										// errorToken.beginOffset) + 1;
						if (problems != null) {
							problems.add(SoarProblem.createError(message, start, length));
						}
					} catch (TokenMgrError e) {
						e.printStackTrace();
						/*
						 * String message = e.getLocalizedMessage();
						 * 
						 * // Get the range of the error, based on the string //
						 * being parsed and the given column and row int start =
						 * 0; for (int i = 1; i < e.getErrorLine();) { char c =
						 * parseText.charAt(start); if (c == '\n') { ++i; }
						 * ++start; }
						 * 
						 * start += beginIndex; // -1 for columns counting
						 * starting with 1 start += e.getErrorColumn() - 1;
						 * 
						 * int length = 2; // (errorToken.endOffset - //
						 * errorToken.beginOffset) + 1; if (input != null) {
						 * input.addProblem(SoarProblem.createError(message,
						 * start, length)); }
						 */
					}
					
					// Add child triples to this rule.
					// First delete existing triples.
					ArrayList<SoarDatabaseRow> triples = getChildrenOfType(Table.TRIPLES);
					for (SoarDatabaseRow triple : triples) {
						triple.delete();
					}
					ArrayList<Triple> newTriples = TraversalUtil.buildTriplesForRule(this);
					if (monitor != null) {
						monitor.beginTask("Writing triples for rule: " + this.getName(), newTriples.size());
					}
					boolean eventsWereSupresssed = db.getSupressEvents();
					db.setSupressEvents(true);
					for (Triple triple : newTriples) {
						triple.getOrCreateTripleRow();
						if (monitor != null) {
							monitor.worked(1);
						}
					}
					db.setSupressEvents(eventsWereSupresssed);
					db.fireEvent(new SoarDatabaseEvent(Type.DATABASE_CHANGED));
					if (monitor != null) {
						monitor.done();
					}
				} else {
					String errorMessage = "Production doesn't begin with \"sp {\" or doesn't end with \"}\" Rule: " + getName();
					errors.add(errorMessage);
				}
			}
		} else if (table == Table.AGENTS) {
			// Update raw text.
			String sql = "update " + table.tableName() + " set raw_text=? where id=?";
			StatementWrapper ps = db.prepareStatement(sql);

			ps.setString(1, text);
			ps.setInt(2, id);
			ps.execute();
		}
		return errors;
	}
	
	/**
	 * 
	 * @param text
	 * @return The given String, with comments removed.
	 */
	public static String removeComments(String text) {
		StringBuilder builder = new StringBuilder();
		StringReader reader = new StringReader(text);
		int i;
		char c;
		char lastChar = ' ';
		boolean inComment = false;
		boolean inString = false;
		try {
			while ((i = reader.read()) != -1) {
				c = (char) i;
				
				if (c == '\n') {
					inString = false;
					inComment = false;
				}
				
				if (c == '|' && (!inString || lastChar != '\\')) {
					inString = !inString;
				}
				
				if (!inString && c == '#') {
					inComment = true;
				}
								
				if (!inComment) {
					builder.append(c);
				}
				
				lastChar = c;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return builder.toString();
	}
	
	/**
	 * Sets the join type between the given rows
	 * (i.e. either JoinType.NONE or the type of
	 * impasse between problem spaces).
	 * @param parent
	 * @param child
	 * @param joinType
	 */
	public static void setDirectedJoinType(SoarDatabaseRow parent, SoarDatabaseRow child, JoinType joinType) {
		SoarDatabaseConnection db = parent.getDatabaseConnection();
		if (!rowsAreDirectedJoined(parent, child, db)) {
			return;
		}
		
		Table parentTable = parent.getTable();
		Table childTable = child.getTable();
		String joinTableName = directedJoinTableName(parentTable, childTable);
		String sql = "update " + joinTableName + " set join_type=" + joinType.ordinal() + " where parent_id=? and child_id=?";
		StatementWrapper ps = db.prepareStatement(sql);
		ps.setInt(1, parent.id);
		ps.setInt(2, child.id);
		ps.execute();
	}
	
	/**
	 * 
	 * @param parent
	 * @param child
	 * @return The join type between the given rows.
	 */
	public static JoinType getDirectedJoinType(SoarDatabaseRow parent, SoarDatabaseRow child) {
		SoarDatabaseConnection db = parent.getDatabaseConnection();
		if (!rowsAreDirectedJoined(parent, child, db)) {
			return JoinType.NONE;
		}
		
		Table parentTable = parent.getTable();
		Table childTable = child.getTable();
		String joinTableName = directedJoinTableName(parentTable, childTable);
		String sql = "select join_type from " + joinTableName + " where parent_id=? and child_id=?";
		StatementWrapper ps = db.prepareStatement(sql);
		
		JoinType ret = JoinType.NONE;
		
		try {
			ps.setInt(1, parent.id);
			ps.setInt(2, child.id);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Integer i = rs.getInt("join_type");
				ret = JoinType.typeForInt(i);
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return ret;
	}
	
	/**
	 * Setter method.
	 * @param type
	 */
	public void setJoinType(JoinType type) {
		joinType = type;
	}
	
	/**
	 * Getter method.
	 * @return
	 */
	public JoinType getJoinType() {
		return joinType;
	}
	
	/**
	 * 
	 * @return True if this is of type Table.PROBLEM_SPACES and it has been marked as root.
	 */
	public boolean isRootProblemSpace() {
		if (getTable() != Table.PROBLEM_SPACES) {
			return false;
		}
		Integer isRoot = (Integer) getColumnValue("is_root");
		if (isRoot == null) {
			return false;
		}
		return isRoot != 0;
	}
	
	/**
	 * Setter method.
	 * @param isRoot
	 */
	public void setIsRootProblemSpace(boolean isRoot) {
		if (getTable() != Table.PROBLEM_SPACES) {
			return;
		}
		updateValue("is_root", isRoot ? "1" : "0");
		SoarDatabaseEvent event = new SoarDatabaseEvent(Type.DATABASE_CHANGED, this);
		getDatabaseConnection().fireEvent(event);
	}

	/**
	 * Getter method.
	 * @return
	 */
	public SoarDatabaseConnection getDatabaseConnection() {
		return db;
	}
	
	/**
	 * Setter method.
	 * Tree conent providers can use this as a flag to know
	 * not to get more children.
	 * @param terminal
	 */
	boolean terminal = false;
	public void setTerminal(boolean terminal) {
		this.terminal = terminal;
	}
	
	/**
	 * Getter method.
	 * @return
	 */
	public boolean isTerminal() {
		return terminal;
	}
	
	/**
	 * Required by ISoarDatabaseTreeItem.
	 * @return This row.
	 */
	@Override
	public SoarDatabaseRow getRow() {
		return this;
	}
}