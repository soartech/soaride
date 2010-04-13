package com.soartech.soar.ide.ui.actions.explorer;

import java.awt.Window;

import org.eclipse.core.runtime.internal.adaptor.EclipseEnvironmentInfo;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class RenameDatabaseRowActionDelegate implements IObjectActionDelegate{

	private SoarDatabaseRow selectedRow = null;
	
	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void run(IAction arg0) {
		// TODO Auto-generated method stub
		if (selectedRow != null && selectedRow instanceof SoarDatabaseRow) {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow() .getShell();
			String title = "Rename";
			String message = "Enter New Name:";
			String initialValue = ((SoarDatabaseRow) selectedRow).toString();
			InputDialog dialog = new InputDialog(shell, title, message, initialValue, null);
			dialog.open();
			String result = dialog.getValue();
			if (result != null && result.length() > 0) {
				((SoarDatabaseRow) selectedRow).setName(result);
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		selectedRow = null;
		action.setEnabled(false);
		if (selection instanceof StructuredSelection) {
			StructuredSelection ss = (StructuredSelection)selection;
			Object obj = ss.getFirstElement();
			if (obj instanceof SoarDatabaseRow) {
				selectedRow = (SoarDatabaseRow) obj;
				action.setEnabled(true);
			}
		}
	}

}
