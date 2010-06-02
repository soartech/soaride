package com.soartech.soar.ide.core.sql;

import java.util.ArrayList;
import java.util.List;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

/**
 * Displays joined rows from a single table.
 * @author miller
 *
 */
public class SoarDatabaseJoinFolder implements ISoarDatabaseTreeItem {

	// The parent row of this folder.
	private SoarDatabaseRow row;
	
	// The table whose rows this folder contains.
	private Table table;
	
	public SoarDatabaseJoinFolder(SoarDatabaseRow row, Table table) {
		this.row = row;
		this.table = table;
	}
	
	@Override
	public List<ISoarDatabaseTreeItem> getChildren(boolean includeFolders,
			boolean includeChildrenInFolders,
			boolean includeJoinedItems,
			boolean includeDirectionalJoinedItems,
			boolean putDirectionalJoinedItemsInFolders,
			boolean includeDatamapNodes) {
		if (includeJoinedItems && includeDirectionalJoinedItems) {
			return row.getJoinedRowsFromTable(table);			
		}
		if (includeJoinedItems) {
			return row.getJoinedRowsFromTable(table);
		}
		if (includeDirectionalJoinedItems) {
			return row.getDirectedJoinedRowsFromTable(table);
		}
		return new ArrayList<ISoarDatabaseTreeItem>();
	}

	@Override
	public boolean hasChildren() {
		return row.hasJoinedRowsFromTable(table);
	}
	
	@Override
	public String toString() {
		return table.tableName();
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
		return this.row.equals(other.row) && this.table == other.table;
	}
	
	@Override
	public int hashCode() {
		return row.getID();
	}

}
