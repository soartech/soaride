package com.soartech.soar.ide.core.sql;

import java.util.ArrayList;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class SoarDatabaseRowFolder implements ISoarDatabaseRow {

	SoarDatabaseRow parent;
	Table type;
	
	public SoarDatabaseRowFolder(SoarDatabaseRow parent, Table type) {
		this.parent = parent;
		this.type = type;
	}
	
	public ArrayList<ISoarDatabaseRow> getChildren() {
		// TODO Auto-generated method stub
		return parent.getChildrenOfType(type);
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "Folder of " + type.tableName();
	}

	@Override
	public boolean hasChildren() {
		return parent.hasChildrenOfType(type);
	}
}