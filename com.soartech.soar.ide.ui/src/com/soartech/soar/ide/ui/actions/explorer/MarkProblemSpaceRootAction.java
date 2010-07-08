package com.soartech.soar.ide.ui.actions.explorer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;

public class MarkProblemSpaceRootAction extends Action {

	SoarDatabaseRow row;
	TreeViewer tree;
	
	public MarkProblemSpaceRootAction(SoarDatabaseRow row, TreeViewer tree) {
		super(row.isRootProblemSpace() ? "Make Non-Root" : "Make Root");
		this.row = row;
	}
	
	@Override
	public void run() {
		row.setIsRootProblemSpace(!row.isRootProblemSpace());
		tree.refresh();
	}
}
