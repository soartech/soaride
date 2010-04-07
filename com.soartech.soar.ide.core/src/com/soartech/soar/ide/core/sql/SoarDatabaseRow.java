package com.soartech.soar.ide.core.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.impl.SoarModel;
import com.soartech.soar.ide.core.model.impl.SoarProject;

public class SoarDatabaseRow {
	
	public enum Table {
		AGENTS,
		PROBLEM_SPACES,
		RULES;
		
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
	
	private Table table;
	private int id;
	private SoarDatabaseConnection db;
	private String name = null;
	
	private static boolean initted = false;
	
	public SoarDatabaseRow(Table table, int id, SoarDatabaseConnection db) {
		
		if (!initted) {
			init();
		}
		
		this.table = table;
		this.id = id;
		this.db = db;
	}
	
	@Override
	public String toString() {
		if (name != null) {
			return name;
		}
		String sql = "select (name) from " + table.tableName() + " where id = " + id;
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
	
	public ArrayList<SoarDatabaseRow> getChildren() {
		ArrayList<SoarDatabaseRow> ret = new ArrayList<SoarDatabaseRow>();
		ArrayList<Table> children = getChildTables();
		for (Table t : children) {
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
		return ret;
    }
	
	public boolean hasChildren() {
		ArrayList<SoarDatabaseRow> ret = new ArrayList<SoarDatabaseRow>();
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
	
	// Static initialization methods:
	
	private static void init() {
		addChild(Table.AGENTS, Table.PROBLEM_SPACES);
		addChild(Table.PROBLEM_SPACES, Table.RULES);
	    addParent(Table.RULES, Table.PROBLEM_SPACES);
	    addParent(Table.PROBLEM_SPACES, Table.AGENTS);
		initted = true;
	}
	
	private static void addChild(Table parent, Table child) {
		if (!childTables.keySet().contains(parent)) {
			ArrayList<Table> newList = new ArrayList<Table>();
			childTables.put(parent, newList);
		}
		ArrayList<Table> children = childTables.get(parent);
		children.add(child);
	}
	
	private static void addParent(Table child, Table parent) {
		if (!parentTables.keySet().contains(child)) {
			ArrayList<Table> newList = new ArrayList<Table>();
			parentTables.put(child, newList);
		}
		ArrayList<Table> parents = parentTables.get(child);
		parents.add(parent);
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

	public void setText(String text) {
		// TODO Auto-generated method stub
		if (table == Table.RULES) {
			String sql = "update " + table.tableName() + " set raw_text=\"" + text + "\" where id=" + id;
			db.execute(sql);
		}
	}
}