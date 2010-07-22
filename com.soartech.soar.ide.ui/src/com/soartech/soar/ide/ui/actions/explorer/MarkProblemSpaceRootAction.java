package com.soartech.soar.ide.ui.actions.explorer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.TreeViewer;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;

public class MarkProblemSpaceRootAction extends Action {

	SoarDatabaseRow row;
	TreeViewer tree;
	
	public MarkProblemSpaceRootAction(SoarDatabaseRow row, TreeViewer tree) {
		super("Root Problem Space", IAction.AS_CHECK_BOX);
		setChecked(row.isRootProblemSpace());
		this.row = row;
	}
	
	@Override
	public void run() {
		row.setIsRootProblemSpace(!row.isRootProblemSpace());
		tree.refresh();
	}
}
