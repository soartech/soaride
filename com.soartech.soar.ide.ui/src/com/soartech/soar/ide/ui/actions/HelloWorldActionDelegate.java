package com.soartech.soar.ide.ui.actions;

import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class HelloWorldActionDelegate implements IObjectActionDelegate {

	SoarDatabaseRow selectedRow = null;
	Table childTable = null;
	
	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	@Override
	public void run(IAction action) {
		selectedRow.createChild(childTable, "New " + childTable.shortName());
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		selectedRow = null;
		childTable = null;
		action.setEnabled(false);
		if (selection instanceof StructuredSelection) {
			StructuredSelection ss = (StructuredSelection)selection;
			Object obj = ss.getFirstElement();
			if (obj instanceof SoarDatabaseRow) {
				selectedRow = (SoarDatabaseRow) obj;
				ArrayList<Table> childTables = selectedRow.getChildTables();
				if (childTables.size() > 0) {
					childTable = childTables.get(0);
					action.setText("Add child " + childTable.shortName());
					action.setEnabled(true);
				} else {
					action.setText("Add child element");
				}
			}
		}
	}

}
