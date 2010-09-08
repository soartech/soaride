package com.soartech.soar.ide.ui.actions.soarmenu;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import com.soartech.soar.ide.core.SoarCorePlugin;

/**
 * Saves the current Soar IDE project in a new file.
 * @author miller
 *
 */
public class SaveSoarDatabaseProjectActionDelegate implements IWorkbenchWindowActionDelegate {

	Shell shell;
	
	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow window) {
		shell = window.getShell();
	}

	@Override
	public void run(IAction action) {

		FileDialog dialog = new FileDialog(shell, SWT.SAVE);
		dialog.setText("Save Soar Project As...");
		dialog.setOverwrite(true);
		String path = dialog.open();
		
		if (path != null && path.length() > 0) {
			final ArrayList<String> errors = SoarCorePlugin.getDefault().saveDatabaseAs(path, true);
			if (errors.size() > 0) {
				System.out.println(errors);
				String message = "";
				for (String error : errors) {
					message += "\n" + error;
				}
				shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				ErrorDialog errorDialog = new ErrorDialog(shell, "Error", message, Status.OK_STATUS, 0);
				errorDialog.open();
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

}
