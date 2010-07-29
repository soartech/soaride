package com.soartech.soar.ide.ui.actions.soarmenu;

import java.io.File;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.soartech.soar.ide.core.SoarCorePlugin;

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
		String path = dialog.open();
		
		File saveFile = new File(path);
		
		boolean save = true;
		if (saveFile.exists()) {
			save = false;
			MessageBox box = new MessageBox(shell, SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
			box.setText("Overwrite existing file?");
			box.setMessage("The file exists at " + path + "\nOverwrite?");
			int result = box.open();
			if (result == SWT.OK) {
				save = true;
				saveFile.delete();
			}
		}
		
		if (save && path != null && path.length() > 0) {
			SoarCorePlugin.getDefault().saveDatabaseAs(path);
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

}
