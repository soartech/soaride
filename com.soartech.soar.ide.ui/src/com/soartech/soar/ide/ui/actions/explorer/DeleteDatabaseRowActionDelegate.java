package com.soartech.soar.ide.ui.actions.explorer;

import java.util.Iterator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class DeleteDatabaseRowActionDelegate implements IObjectActionDelegate {

	StructuredSelection ss;
	
	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run(IAction arg0) {
		// TODO Auto-generated method stub
		Iterator<?> it = ss.iterator();
		while (it.hasNext()) {
			Object obj = it.next();
			if (obj instanceof SoarDatabaseRow) {
				SoarDatabaseRow selectedRow = (SoarDatabaseRow) obj;
				selectedRow.deleteAllChildren(true);
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		ss = null;
		action.setEnabled(false);
		if (selection instanceof StructuredSelection) {
			ss = (StructuredSelection)selection;
			Object obj = ss.getFirstElement();
			if (obj instanceof SoarDatabaseRow) {
				action.setEnabled(true);
			}
		}
	}

}
