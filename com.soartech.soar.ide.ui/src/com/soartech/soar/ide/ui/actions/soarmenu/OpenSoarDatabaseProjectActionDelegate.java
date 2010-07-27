package com.soartech.soar.ide.ui.actions.soarmenu;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.ui.SoarUiModelTools;

public class OpenSoarDatabaseProjectActionDelegate implements IWorkbenchWindowActionDelegate {

	Shell shell;
	IWorkbenchWindow window;
	
	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow window) {
		shell = window.getShell();
		this.window = window;
	}

	@Override
	public void run(IAction action) {
		
		boolean savedToDisk = SoarCorePlugin.getDefault().getDatabaseConnection().isSavedToDisk();
		
		if (!savedToDisk) {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			MessageDialog message = new MessageDialog(shell, "Open project?", null, "Open project? Unsaved changes will be lost.", MessageDialog.QUESTION, new String[] {"OK", "Cancel"}, 0);
			int result = message.open();
			if (result != 0) {
				return;
			}
		}
		
		FileDialog dialog = new FileDialog(shell, SWT.OPEN);
		dialog.setText("Open Soar Project...");
		dialog.open();
		String path = dialog.getFileName();
		
		if (savedToDisk) {
			SoarUiModelTools.closeAllEditors(true);
		} else {
			SoarUiModelTools.closeAllEditors(false);
		}
		
		if (path != null && path.length() > 0) {

			SoarCorePlugin.getDefault().openProject(path);
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

}
