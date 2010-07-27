package com.soartech.soar.ide.ui.actions.soarmenu;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.sql.SoarDatabaseConnection;
import com.soartech.soar.ide.ui.SoarUiModelTools;

public class NewSoarProjectActionDelegate implements IWorkbenchWindowActionDelegate {

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow arg0) {
	}

	@Override
	public void run(IAction action) {
		if (SoarCorePlugin.getDefault().getSoarModel().getDatabase().isSavedToDisk()) {
			SoarUiModelTools.closeAllEditors(true);
		} else {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			MessageDialog message = new MessageDialog(shell, "Create new project?", null, "Create new project? Unsaved changes will be lost.", MessageDialog.QUESTION, new String[] {"OK", "Cancel"}, 0);
			int result = message.open();
			if (result == 1) {
				return;
			}
			SoarUiModelTools.closeAllEditors(false);
		}
		SoarCorePlugin.getDefault().newProject();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

}
