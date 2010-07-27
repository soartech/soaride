package com.soartech.soar.ide.core.sql;

import java.util.ArrayList;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class SoarDatabaseRowFolder implements ISoarDatabaseTreeItem {

	SoarDatabaseRow row;
	Table table;
	
	public SoarDatabaseRowFolder(SoarDatabaseRow row, Table table) {
		this.row = row;
		this.table = table;
	}
	
	public ArrayList<ISoarDatabaseTreeItem> getChildren(boolean includeFolders,
			boolean includeChildrenInFolders,
			boolean includeJoinedItems, 
			boolean includeDirectionalJoinedItems, 
			boolean putDirectionalJoinedItemsInFolders, 
			boolean includeDatamapNodes) {
		return new ArrayList<ISoarDatabaseTreeItem>(row.getChildrenOfType(table));
	}
	
	@Override
	public String toString() {
		return table.pluralEnglishName();
	}

	@Override
	public boolean hasChildren() {
		return row.hasChildrenOfType(table);
	}
	
	public SoarDatabaseRow getRow() {
		return row;
	}
	
	public Table getTable() {
		return table;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SoarDatabaseRowFolder)) {
			return false;
		}
		SoarDatabaseRowFolder other = (SoarDatabaseRowFolder) obj;
		if (this.row.equals(other.row) && this.table == other.table) {
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return row.getID();
	}
}