package com.soartech.soar.ide.core.sql;

import java.util.List;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

/**
 * Displays joined rows from a single table.
 * @author miller
 *
 */
public class SoarDatabaseJoinFolder implements ISoarDatabaseRow {

	// The parent row of this folder.
	private SoarDatabaseRow row;
	
	// The table whose rows this folder contains.
	private Table table;
	
	public SoarDatabaseJoinFolder(SoarDatabaseRow row, Table table) {
		this.row = row;
		this.table = table;
	}
	
	@Override
	public List<ISoarDatabaseRow> getChildren() {
		// TODO Auto-generated method stub
		return row.getJoinedRowsFromTable(table);
	}

	@Override
	public boolean hasChildren() {
		// TODO Auto-generated method stub
		return row.hasJoinedRowsFromTable(table);
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "Joined " + table.tableName();
	}
	
	public SoarDatabaseRow getRow() {
		return row;
	}
	
	public Table getTable() {
		return table;
	}
	

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SoarDatabaseJoinFolder)) {
			return false;
		}
		SoarDatabaseJoinFolder other = (SoarDatabaseJoinFolder) obj;
		return this.row == other.row && this.table == other.table;
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return 0;
	}

}
