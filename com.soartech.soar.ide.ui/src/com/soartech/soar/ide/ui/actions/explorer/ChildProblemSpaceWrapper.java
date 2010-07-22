package com.soartech.soar.ide.ui.actions.explorer;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;

public class ChildProblemSpaceWrapper extends SoarDatabaseRow {

	//SoarDatabaseRow parent;
	SoarDatabaseRow child;
	
	public ChildProblemSpaceWrapper(SoarDatabaseRow parent, SoarDatabaseRow child) {
		super(child.getTable(), child.getID(), child.getDatabaseConnection());
		//this.parent = parent;
		this.child = child;
		joinType = SoarDatabaseRow.getDirectedJoinType(parent, child);
	}
	
	@Override
	public String toString() {
		if (joinType == JoinType.NONE) return super.toString();
		return super.toString() + " (" + joinType.englishName() + ")";
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof SoarDatabaseRow) {
			SoarDatabaseRow otherRow = (SoarDatabaseRow) other;
			if (otherRow.getTable() == this.getTable() && otherRow.getID() == this.getID()
					&& otherRow.getJoinType() == this.getJoinType()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public SoarDatabaseRow getRow() {
		return child;
	}
}
