package com.soartech.soar.ide.core.sql;

import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;

import com.soartech.soar.ide.core.model.ast.Action;
import com.soartech.soar.ide.core.model.ast.AttributeTest;
import com.soartech.soar.ide.core.model.ast.AttributeValueMake;
import com.soartech.soar.ide.core.model.ast.AttributeValueTest;
import com.soartech.soar.ide.core.model.ast.BinaryPreference;
import com.soartech.soar.ide.core.model.ast.Condition;
import com.soartech.soar.ide.core.model.ast.ConditionForOneIdentifier;
import com.soartech.soar.ide.core.model.ast.ConjunctiveTest;
import com.soartech.soar.ide.core.model.ast.Constant;
import com.soartech.soar.ide.core.model.ast.DisjunctionTest;
import com.soartech.soar.ide.core.model.ast.ForcedUnaryPreference;
import com.soartech.soar.ide.core.model.ast.FunctionCall;
import com.soartech.soar.ide.core.model.ast.NaturallyUnaryPreference;
import com.soartech.soar.ide.core.model.ast.Pair;
import com.soartech.soar.ide.core.model.ast.ParseException;
import com.soartech.soar.ide.core.model.ast.PositiveCondition;
import com.soartech.soar.ide.core.model.ast.PreferenceSpecifier;
import com.soartech.soar.ide.core.model.ast.RHSValue;
import com.soartech.soar.ide.core.model.ast.RelationalTest;
import com.soartech.soar.ide.core.model.ast.SimpleTest;
import com.soartech.soar.ide.core.model.ast.SingleTest;
import com.soartech.soar.ide.core.model.ast.SoarParser;
import com.soartech.soar.ide.core.model.ast.SoarProductionAst;
import com.soartech.soar.ide.core.model.ast.Test;
import com.soartech.soar.ide.core.model.ast.ValueMake;
import com.soartech.soar.ide.core.model.ast.ValueTest;
import com.soartech.soar.ide.core.model.ast.VarAttrValMake;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent.Type;
import com.sun.org.apache.bcel.internal.generic.ASTORE;

public class SoarDatabaseRow implements ISoarDatabaseRow {
	
	public enum Table {
		AGENTS,
		PROBLEM_SPACES,
		OPERATORS,
		
		// Rules
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
		RELATIONS,
		SINGLE_TESTS,
		CONSTANTS,
		ACTIONS,
		VAR_ATTR_VAL_MAKES,
		ATTRIBUTE_VALUE_MAKES,
		FUNCTION_CALLS,
		FUNCTION_NAMES,
		RHS_VALUES,
		VALUE_MAKES,
		PREFERENCES,
		PREFERENCE_SPECIFIERS,
		NATURALLY_UNARY_PREFERENCES,
		BINARY_PREFERENCES,
		FORCED_UNARY_PREFERENCES,
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
	}
	
	public static Table getTableNamed(String name) {
		for (Table t : Table.values()) {
			if (name.equalsIgnoreCase(t.name())) {
				return t;
			}
		}
		return null;
	}
	
	private static HashMap<Table, ArrayList<Table>> childTables = new HashMap<Table, ArrayList<Table>>();
	private static HashMap<Table, ArrayList<Table>> parentTables = new HashMap<Table, ArrayList<Table>>();
	private static HashMap<Class<?>, Table> tableForAstNode = new HashMap<Class<?>, Table>();
	private static HashMap<Table, ArrayList<Table>> childFolders = new HashMap<Table, ArrayList<Table>>();
	
	private Table table;
	private int id;
	private SoarDatabaseConnection db;
	private String name = null;
	private HashMap<Table, SoarDatabaseRowFolder> folders = new HashMap<Table, SoarDatabaseRowFolder>();
	
	private static boolean initted = false;
	
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
				SoarDatabaseRowFolder folder = new SoarDatabaseRowFolder(this, type);
				folders.put(type, folder);
			}
		}
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	public String getName() {
		if (name != null) {
			return name;
		}
		String sql = "select (name) from " + table.tableName() + " where id=" + id;
		ResultSet rs = db.getResultSet(sql);
		try {
			if (!rs.next()) {
				rs.getStatement().close();
				name = table.tableName() + ": NO ROW WITH ID " + id;
			}
			name = rs.getString("name");
			rs.getStatement().close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return name;
	}
	
	public void setName(String name) {
		this.name = null;
		String sql = "update " + table.tableName() + " set name=\"" + name + "\" where id=" + id;
		db.execute(sql);
	}
	
	public Table getTable() {
		return table;
	}
	
	public int getID() {
		return id;
	}
	
	public void delete() {
		String sql = "delete from " + table.tableName() + " where id = " + id;
		db.execute(sql);
	}
	
	public ArrayList<Table> getChildTables() {
		if (childTables.containsKey(table)) {
			ArrayList<Table> children = childTables.get(table);
			return children;
		}
		return new ArrayList<Table>();
	}
	
	/**
	 * Returns rows and folders (but not rows which are contained by child folders)
	 */
	public ArrayList<ISoarDatabaseRow> getChildren() {
		ArrayList<ISoarDatabaseRow> ret = new ArrayList<ISoarDatabaseRow>();
		ArrayList<Table> children = getChildTables();
		for (Table t : children) {
			if (folders.containsKey(t)) {
				ret.add(folders.get(t));
			} else {
				String sql = "select * from " + t.tableName() + " where " + table.idName() + " = " + id;
				ResultSet rs = db.getResultSet(sql);
				try {
					while (rs.next()) {
						SoarDatabaseRow row = new SoarDatabaseRow(t, rs.getInt("id"), db);
						ret.add(row);
					}
					rs.getStatement().close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return ret;
    }
	
	/**
	 * Doesn't return folders.
	 * @param type
	 * @return
	 */
	public ArrayList<ISoarDatabaseRow> getChildrenOfType(Table type) {
		ArrayList<ISoarDatabaseRow> ret = new ArrayList<ISoarDatabaseRow>();
		ArrayList<Table> children = getChildTables();
		for (Table t : children) {
			if (t == type) {
				String sql = "select * from " + t.tableName() + " where " + table.idName() + " = " + id;
				ResultSet rs = db.getResultSet(sql);
				try {
					while (rs.next()) {
						SoarDatabaseRow row = new SoarDatabaseRow(t, rs.getInt("id"), db);
						ret.add(row);
					}
					rs.getStatement().close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return ret;
	}
	
	public boolean hasChildren() {
		ArrayList<Table> children = getChildTables();
		for (Table t : children) {
			String sql = "select * from " + t.tableName() + " where " + table.idName() + " = " + id;
			ResultSet rs = db.getResultSet(sql);
			try {
				if (rs.next()) {
					rs.getStatement().close();
					return true;
				}
				rs.getStatement().close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}

	public boolean hasChildrenOfType(Table type) {
		String sql = "select * from " + type.tableName() + " where " + table.idName() + " = " + id;
		ResultSet rs = db.getResultSet(sql);
		try {
			if (rs.next()) {
				rs.getStatement().close();
				return true;
			}
			rs.getStatement().close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public ArrayList<Table> getParentTables() {
		if (parentTables.containsKey(table)) {
			ArrayList<Table> parents = parentTables.get(table);
			return parents;
		}
		return new ArrayList<Table>();
	}
	
	public ArrayList<SoarDatabaseRow> getParentRow() {
		ArrayList<SoarDatabaseRow> ret = new ArrayList<SoarDatabaseRow>();
		ArrayList<Table> parents = getParentTables();
		for (Table t : parents) {
			String sql = "select * from " + t.tableName() + " where id = (select " + t.idName() + " from " + table.tableName() + " where id = " + id + ")";
			ResultSet rs = db.getResultSet(sql);
			try {
				while (rs.next()) {
					SoarDatabaseRow row = new SoarDatabaseRow(t, rs.getInt("id"), db);
					ret.add(row);
				}
				rs.getStatement().close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ret;
    }
	
	// Create new entries
	
	public void createChild(Table childTable, String name) {
		db.createChild(this, childTable, name);
	}
	
	public void createChildrenFromAstNode(Object node) throws Exception {
		ArrayList<Table> childTables = getChildTables();
		Table childTable = tableForAstNode.get(node.getClass());
		SqlArgsAndChildNodes argsAndNodes = sqlArgsAndChildNodes(node, childTable);
		SoarDatabaseRow childRow;
		
		if (childTable == this.table) {
			// This should only happen when 'node' is a SoarProductionAST and this is already a 'Rule'
			childRow = this;
		} else if (childTables.contains(childTable)) {  
			// create databse entry
			String[][] sqlArgs = argsAndNodes.sqlArgs;
			db.createChild(this, childTable, sqlArgs);
			
			// Get created row
			String sql = "select * from " + childTable.tableName() + " where id=(last_insert_rowid())";
			ResultSet rs = db.getResultSet(sql);
			int id = -1;
			if (rs.next()) {
				id = rs.getInt("id");
			}
			rs.close();
			if (id == -1) {
				throw new Exception("Insert failed");
			}
			childRow = new SoarDatabaseRow(childTable, id, db);
		} else  {
			throw new Exception("No child table of current type \"" + table.shortName() + "\" for AST node \"" + childTable + "\"");
		}
		
		// recursively create new entries
		Object[] childNodes = argsAndNodes.childNodes;
		for (Object childNode : childNodes) {
			childRow.createChildrenFromAstNode(childNode);
		}
	}
	
	public void deleteAllChildren(boolean alsoDeleteThis) {
		ArrayList<ISoarDatabaseRow> childRows = getChildren();
		for (ISoarDatabaseRow child : childRows) {
			if (child instanceof SoarDatabaseRow) {
				((SoarDatabaseRow)child).deleteAllChildren(true);
			}
		}
		if (alsoDeleteThis) {
			delete();
		}
	}
	
	private class SqlArgsAndChildNodes {
		public String[][] sqlArgs;
		public Object[] childNodes;
		public SqlArgsAndChildNodes(ArrayList<String[]> sqlArgs, Object[] childNodes) {
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
	
	private SqlArgsAndChildNodes sqlArgsAndChildNodes(Object node, Table nodeTable) {
		
		ArrayList<String[]> sqlArgs = new ArrayList<String[]>();
		Object[] childNodes = new Object[0]; // = null;
		
		Class<?> nodeClass = node.getClass();
		String name = nodeTable.shortName();
		
		String sqlTrue = "1";
		String sqlFalse = "0";
		
		if (nodeClass == SoarProductionAst.class) {
			Object[] conditions = ((SoarProductionAst) node).getConditions().toArray();
			Object[] actions = ((SoarProductionAst) node).getActions().toArray();
			childNodes = new Object[conditions.length + actions.length];
			System.arraycopy(conditions, 0, childNodes, 0, conditions.length);
			System.arraycopy(actions, 0, childNodes, conditions.length, actions.length);
		}
		else if (nodeClass == Condition.class) {
			Object positiveCondition = ((Condition)node).getPositiveCondition();
			childNodes = new Object[] { positiveCondition };
			boolean isNegated = ((Condition)node).isNegated();
			sqlArgs.add(new String[] { "is_negated", isNegated ? sqlTrue : sqlFalse });
			if (isNegated) name += " (negated)";
		}
		else if (nodeClass == PositiveCondition.class) {
			boolean isConjunction = ((PositiveCondition)node).isConjunction();
			sqlArgs.add(new String[] { "is_conjunction", isConjunction ? sqlTrue : sqlFalse });
			if (isConjunction) {
				childNodes = ((PositiveCondition)node).getConjunction().toArray();
			} else {
				childNodes = new Object[] { ((PositiveCondition)node).getConditionForOneIdentifier() };
			}
			if (isConjunction) name += " (conjunction)";
		}
		else if (nodeClass == ConditionForOneIdentifier.class) {
			childNodes = ((ConditionForOneIdentifier)node).getAttributeValueTests().toArray();
			boolean hasState = ((ConditionForOneIdentifier)node).hasState();
			sqlArgs.add(new String[] { "has_state", hasState ? sqlTrue : sqlFalse });
			Pair pair = ((ConditionForOneIdentifier)node).getVariable();
			String variable = pair.getString();
			sqlArgs.add(new String[] { "variable", "\"" + variable + "\"" });
			name += " (variable: " + variable + ")";
			if (hasState) name += " (has state)";
		}
		else if (nodeClass == AttributeValueTest.class) {
			Object[] attributeTests = ((AttributeValueTest) node).getAttributeTests().toArray();
			Object[] valueTests = ((AttributeValueTest) node).getValueTests().toArray();
			childNodes = new Object[attributeTests.length + valueTests.length];
			System.arraycopy(attributeTests, 0, childNodes, 0, attributeTests.length);
			System.arraycopy(valueTests, 0, childNodes, attributeTests.length, valueTests.length);
			sqlArgs.add(new String[] { "is_negated", ((AttributeValueTest)node).isNegated() ? sqlTrue : sqlFalse });
		}
		else if (nodeClass == AttributeTest.class) {
			childNodes = new Object[] { ((AttributeTest)node).getTest() };
		}
		else if (nodeClass == Test.class) {
			boolean isConjunctiveTest = ((Test)node).isConjunctiveTest();
			sqlArgs.add(new String[] { "is_conjunctive_test", isConjunctiveTest ? sqlTrue : sqlFalse });
			if (isConjunctiveTest) {
				childNodes = new Object[] { ((Test)node).getConjunctiveTest() };
			} else {
				childNodes = new Object[] { ((Test)node).getSimpleTest() };
			}
			name += isConjunctiveTest ? " (conjunctive test)" : " (simple test)";
		}
		else if (nodeClass == SimpleTest.class) {
			boolean isDisjunctionTest = ((SimpleTest)node).isDisjunctionTest();
			sqlArgs.add(new String[] { "is_disjunction_test", isDisjunctionTest ? sqlTrue : sqlFalse });
			if (isDisjunctionTest) {
				childNodes = new Object[] { ((SimpleTest)node).getDisjunctionTest() };
			} else {
				childNodes = new Object[] { ((SimpleTest)node).getRelationalTest() };
			}
			name += isDisjunctionTest ? " (disjunction test)" : " (relational test)";
		}
		else if (nodeClass == DisjunctionTest.class) {
			childNodes = ((DisjunctionTest)node).getConstants().toArray();
		}
		else if (nodeClass == RelationalTest.class) {
			childNodes = new Object[] { ((RelationalTest)node).getSingleTest() };
			int relation = ((RelationalTest)node).getRelation();
			sqlArgs.add(new String[] { "relation", "" + relation });
			name += " (" + RelationalTest.RELATIONS[relation] + ")";
		}
		else if (nodeClass == SingleTest.class) {
			boolean isConstant = ((SingleTest)node).isConstant();
			sqlArgs.add(new String[] { "is_constant", isConstant ? sqlTrue : sqlFalse });
			if (isConstant) {
				childNodes = new Object[] { ((SingleTest)node).getConstant() };
				name += " (constant)";
			} else {
				Pair pair = ((SingleTest)node).getVariable();
				String variable = pair.getString();
				sqlArgs.add(new String[] { "variable", "\"" + variable + "\"" });
				name += " (variable: " + variable + ")";
			}
		}
		else if (nodeClass == ConjunctiveTest.class) {
			childNodes = ((ConjunctiveTest)node).getSimpleTests().toArray();
		}
		else if (nodeClass == ValueTest.class) {
			childNodes = new Object[] { ((ValueTest)node).getTest() };
			boolean hasAcceptablePreference = ((ValueTest)node).hasAcceptablePreference();
			sqlArgs.add(new String[] { "has_acceptable_preference", hasAcceptablePreference ? sqlTrue : sqlFalse });
			if (hasAcceptablePreference) name += " (has acceptable preference)";
		}
		else if (nodeClass == Action.class) {
			boolean isVarAttrValMake = ((Action)node).isVarAttrValMake();
			sqlArgs.add(new String[] { "is_var_attr_val_make", isVarAttrValMake ? sqlTrue : sqlFalse });
			if (isVarAttrValMake) {
				childNodes = new Object[] { ((Action)node).getVarAttrValMake() };
			} else {
				childNodes = new Object[] { ((Action)node).getFunctionCall() };
			}
			name += isVarAttrValMake ? " (variable attribute make)" : " (function call)";
		}
		else if (nodeClass == VarAttrValMake.class) {
			childNodes = ((VarAttrValMake)node).getAttributeValueMakes().toArray();
			Pair pair = ((VarAttrValMake)node).getVariable();
			String variable = pair.getString();
			sqlArgs.add(new String[] { "variable", "\"" + variable + "\"" });
			name += " (variable: " + variable + ")";
		}
		else if (nodeClass == AttributeValueMake.class) {
			Object[] rhsValues = ((AttributeValueMake) node).getRHSValues().toArray();
			Object[] valueMakes = ((AttributeValueMake) node).getValueMakes().toArray();
			childNodes = new Object[rhsValues.length + valueMakes.length];
			System.arraycopy(rhsValues, 0, childNodes, 0, rhsValues.length);
			System.arraycopy(valueMakes, 0, childNodes, rhsValues.length, valueMakes.length);
		}
		else if (nodeClass == RHSValue.class) {
			boolean isConstant = ((RHSValue) node).isConstant();
			boolean isFunctionCall = ((RHSValue) node).isConstant();
			boolean isVariable = ((RHSValue) node).isConstant();
			sqlArgs.add(new String[] { "is_constant", isConstant ? sqlTrue : sqlFalse });
			sqlArgs.add(new String[] { "is_function_call", isFunctionCall ? sqlTrue : sqlFalse });
			sqlArgs.add(new String[] { "is_variable", isVariable ? sqlTrue : sqlFalse });
			if (isConstant) {
				childNodes = new Object[] { ((RHSValue)node).getConstant() };
				name += " (constant)";
			}
			else if (isFunctionCall) {
				childNodes = new Object[] { ((RHSValue)node).getFunctionCall() };
				name += " (function call)";
			}
			else if (isVariable) {
				Pair pair = ((RHSValue)node).getVariable();
				String variable = pair.getString();
				sqlArgs.add(new String[] { "variable", "\"" + variable + "\"" });
				name += " (variable: " + variable + ")";
			}
		} else if (nodeClass == Constant.class) {
			int constantType = ((Constant)node).getConstantType();
			sqlArgs.add(new String[] { "constant_type", "" + constantType });
			if (constantType == Constant.FLOATING_CONST) {
				float floatingConst = ((Constant)node).getFloatConst();
				sqlArgs.add(new String[] { "floating_const", "" + floatingConst });
				name += " (floating constant: " + floatingConst + ")";
			} else if (constantType == Constant.INTEGER_CONST) {
				int intConst = ((Constant)node).getIntConst();
				sqlArgs.add(new String[] { "integer_const", "" + intConst });
				name += " (integer constant: " + intConst + ")";
			} else if (constantType == Constant.SYMBOLIC_CONST) {
				String symConst = ((Constant)node).getSymConst();
				sqlArgs.add(new String[] { "symbolic_const", "\"" + symConst + "\"" });
				name += " (symbolic constant: " + symConst + ")";
			}
		}
		else if (nodeClass == FunctionCall.class) {
			childNodes = ((FunctionCall)node).getRHSValues().toArray();
			Pair pair = ((FunctionCall)node).getFunctionName();
			String variable = pair.getString();
			sqlArgs.add(new String[] { "function_name", variable });
			name += " (variable: " + variable + ")";
		}
		else if (nodeClass == ValueMake.class) {
			Object[] preferenceSpecifiers = objectsArrayFromIterator(((ValueMake) node).getPreferenceSpecifiers());
			Object[] rhsValue = new Object[] { ((ValueMake) node).getRHSValue() };
			childNodes = new Object[preferenceSpecifiers.length + rhsValue.length];
			System.arraycopy(preferenceSpecifiers, 0, childNodes, 0, preferenceSpecifiers.length);
			System.arraycopy(rhsValue, 0, childNodes, preferenceSpecifiers.length, rhsValue.length);
		}
		else if (nodeClass == PreferenceSpecifier.class) {
			childNodes = new Object[] { ((PreferenceSpecifier)node).getRHS() };
			boolean isUnaryPreference = ((PreferenceSpecifier)node).isUnaryPreference();
			int preferenceSpecifierType = ((PreferenceSpecifier)node).getPreferenceSpecifierType();
			sqlArgs.add(new String[] { "is_unary_preference", isUnaryPreference ? sqlTrue : sqlFalse });
			sqlArgs.add(new String[] { "preference_specifier_type", "" + preferenceSpecifierType });
			if (isUnaryPreference) name += " (unary preference)";
			name += " (preference: " + PreferenceSpecifier.PREFERENCES[preferenceSpecifierType] + ")";
		}

		// Every row has a name -- this could change later
		sqlArgs.add(new String[] { "name", "\"" + name + "\"" });
		
		SqlArgsAndChildNodes ret = new SqlArgsAndChildNodes(sqlArgs, childNodes);
		return ret;
	}
	
	private <T> Object[] objectsArrayFromIterator(Iterator<T> it) {
		ArrayList<T> list = new ArrayList<T>();
		while (it.hasNext()) {
			list.add(it.next());
		}
		return list.toArray();
	}
	
	// Static initialization methods:
	
	private static void init() {
		
		// table problem spaces has foreign key agent_id:
	    addParent(Table.PROBLEM_SPACES, Table.AGENTS);
	    addParent(Table.OPERATORS, Table.AGENTS);
	    addParent(Table.RULES, Table.AGENTS);
	    
	    // Which tables have folders
	    ArrayList<Table> agentFolders = new ArrayList<Table>();
	    agentFolders.add(Table.PROBLEM_SPACES);
	    agentFolders.add(Table.OPERATORS);
	    agentFolders.add(Table.RULES);
	    childFolders.put(Table.AGENTS, agentFolders);
	    
	    // rule structure
	    addParent(Table.CONDITIONS, Table.RULES);
	    addParent(Table.POSITIVE_CONDITIONS, Table.CONDITIONS);
	    addParent(Table.CONDITION_FOR_ONE_IDENTIFIERS, Table.POSITIVE_CONDITIONS);
	    addParent(Table.ATTRIBUTE_VALUE_TESTS, Table.CONDITION_FOR_ONE_IDENTIFIERS);
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
	    addParent(Table.RELATIONS, Table.RELATIONAL_TESTS);
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
	    addParent(Table.PREFERENCES, Table.VALUE_MAKES);
	    addParent(Table.PREFERENCE_SPECIFIERS, Table.PREFERENCES);
	    addParent(Table.NATURALLY_UNARY_PREFERENCES, Table.PREFERENCE_SPECIFIERS);
	    addParent(Table.BINARY_PREFERENCES, Table.PREFERENCE_SPECIFIERS);
	    addParent(Table.BINARY_PREFERENCES, Table.FORCED_UNARY_PREFERENCES);
	    addParent(Table.FORCED_UNARY_PREFERENCES, Table.PREFERENCE_SPECIFIERS);
	    
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
	    // tableForAstElement.put(Preference.class, Table.PREFERENCES);
	    
	    tableForAstNode.put(PreferenceSpecifier.class, Table.PREFERENCE_SPECIFIERS);
	    tableForAstNode.put(NaturallyUnaryPreference.class, Table.NATURALLY_UNARY_PREFERENCES);
	    tableForAstNode.put(BinaryPreference.class, Table.BINARY_PREFERENCES);
	    tableForAstNode.put(ForcedUnaryPreference.class, Table.FORCED_UNARY_PREFERENCES);
	    
		initted = true;
	}
	
	/**
	 * Indicate that table child has foreign key parent_id
	 * @param child The child table
	 * @param parent The parent table
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
	
	private static void addChild(Table parent, Table child) {
		if (!childTables.keySet().contains(parent)) {
			ArrayList<Table> newList = new ArrayList<Table>();
			childTables.put(parent, newList);
		}
		ArrayList<Table> children = childTables.get(parent);
		children.add(child);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof SoarDatabaseRow) {
			SoarDatabaseRow otherRow = (SoarDatabaseRow) other;
			if (otherRow.table == this.table && otherRow.id == this.id) {
				return true;
			}
		}
		return false;
	}

	public IEditorInput getEditorInput() {
		return new SoarDatabaseEditorInput(this);
	}

	public boolean exists() {
		String sql = "select * from " + getTable().tableName() + " where id=" + id;
		ResultSet rs = db.getResultSet(sql);
		boolean ret = false;
		try {
			ret = rs.next();
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

	public String getText() {
		String ret = null;
		if (table == Table.RULES) {
			String sql = "select (raw_text) from " + table.tableName() + " where id=" + id;
			ResultSet rs = db.getResultSet(sql);
			try {
				if (rs.next()) {
					ret = rs.getString("raw_text");
				}
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (ret == null) {
			ret = "";
		}
		return ret;
	}

	public void save(IDocument doc) {
		// TODO Auto-generated method stub
		if (table == Table.RULES) {
			
			// Update raw text.
			String text = doc.get();
			String sql = "update " + table.tableName() + " set raw_text=\"" + text + "\" where id=" + id;
			db.execute(sql);
			
			// Try to get AST from the text.
			// Make sure text starts with "sp {" and ends with "}",
			// but parse only the text inside that.
			// <hacky>
			boolean error = false;
			text = text.trim();
			if (!text.startsWith("sp")) {
				error = true;
			}
			else {
				text = text.substring(2).trim();
				if (!text.startsWith("{")) {
					error = true;
				}
				
				else {
					text = text.substring(1).trim();
					if (!text.endsWith("}")) {
						error = true;
					} else {
						int endIndex = text.length() - 1;
						text = text.substring(0, endIndex);
					}
				}
			}
			// </hacky>

			if (!error) {
				// Parse the rule into an AST.
				StringReader reader = new StringReader(text);
				SoarParser parser = new SoarParser(reader);
				try {
					SoarProductionAst ast = parser.soarProduction();
					System.out.println("Parsed rule:\n" + ast);
					
					// insert into database
					boolean eventsWereSupresssed = db.getSupressEvents();
					db.setSupressEvents(true);
					deleteAllChildren(false);
					try {
						createChildrenFromAstNode(ast);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					db.setSupressEvents(eventsWereSupresssed);
					db.fireEvent(new SoarDatabaseEvent(Type.DATABASE_CHANGED));
					
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				System.out
						.println("Production doesn't begin with \"sp {\" or doesn't end with \"}\"");
			}
		}
	}
}