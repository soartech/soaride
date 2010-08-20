package com.soartech.soar.ide.ui.actions.explorer;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;

/**
 * Wraps a Problem Space so that when it's displayed it shows the join type between
 * the Problem Space and its parent Problem Space. Also overrides equals() so that
 * it won't match against ProblemSpaceWrappers with the same child Problem Space but
 * different Join Types.
 * @author miller
 *
 */
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
