package com.soartech.soar.ide.ui.actions.soarmenu;

import java.util.ArrayList;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

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
		
		/*
		
		File saveFile = new File(path);
		
		boolean save = true;
		boolean overwriteExisting = false;
		if (saveFile.exists()) {
			save = false;
			MessageBox box = new MessageBox(shell, SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
			box.setText("Overwrite existing file?");
			box.setMessage("The file exists at " + path + "\nOverwrite?");
			int result = box.open();
			if (result == SWT.OK) {
				save = true;
				overwriteExisting = true;
				// saveFile.delete();
			}
		}
		*/
		
		if (path != null && path.length() > 0) {
			ArrayList<String> errors = SoarCorePlugin.getDefault().saveDatabaseAs(path, true);
			if (errors.size() > 0) {
				System.out.println(errors);
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

}
