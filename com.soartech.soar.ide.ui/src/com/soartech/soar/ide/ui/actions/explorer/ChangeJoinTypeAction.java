package com.soartech.soar.ide.ui.actions.explorer;

import org.eclipse.jface.action.Action;

import com.soartech.soar.ide.core.sql.SoarDatabaseEvent;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent.Type;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.JoinType;

public class ChangeJoinTypeAction extends Action {
	
	SoarDatabaseRow parent;
	SoarDatabaseRow child;
	JoinType newType;
	
	public ChangeJoinTypeAction(SoarDatabaseRow parent, SoarDatabaseRow child, JoinType newType) {
		super(newType.shortEnglishName());
		this.parent = parent;
		this.child = child;
		this.newType = newType;
	}
	
	@Override
	public void run() {
		super.run();
		SoarDatabaseRow.setDirectedJoinType(parent, child, newType);
		child.setJoinType(newType);
		child.getDatabaseConnection().fireEvent(new SoarDatabaseEvent(Type.DATABASE_CHANGED, child));
	}
}
