package com.soartech.soar.ide.ui.actions.explorer;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class AddAgentActionDelegate implements IViewActionDelegate {

	@Override
	public void run(IAction action) {
		
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow() .getShell();
		String title = "New Agent";
		String message = "Enter Name:";
		String initialValue = "New Agent";
		InputDialog dialog = new InputDialog(shell, title, message, initialValue, null);
		dialog.open();
		String result = dialog.getValue();

		if (result != null && result.length() > 0) {
			ISoarModel model = SoarCorePlugin.getDefault() .getInternalSoarModel();
			model.getDatabase().insert(Table.AGENTS, new String[][] { { "name", "\"" + result + "\"" } });
		}
	}

	@Override
	public void init(IViewPart arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
		// TODO Auto-generated method stub
		
	}
}
