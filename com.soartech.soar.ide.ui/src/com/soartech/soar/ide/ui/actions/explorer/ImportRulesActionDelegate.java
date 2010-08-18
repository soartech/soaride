package com.soartech.soar.ide.ui.actions.explorer;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseUtil;
import com.soartech.soar.ide.ui.SoarUiModelTools;

public class ImportRulesActionDelegate implements IWorkbenchWindowActionDelegate {

	@Override
	public void run(IAction action) {
		SoarDatabaseRow agent = SoarUiModelTools.selectAgent();
		if (agent != null) {
			Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
			FileDialog dialog = new FileDialog(shell);
			dialog.setText("Import Rules From Soar Source File");
			String path = dialog.open();
			File file = new File(path);
			ArrayList<String> errors = SoarDatabaseUtil.importRules(file, agent, null);
			if (errors.size() > 0) {
				String message = "Encountered the following errors during the import operation:";
				for (String error : errors) {
					message += "\n" + error;
				}
				ErrorDialog errorDialog = new ErrorDialog(shell, "Error", message, Status.OK_STATUS, 0);
				errorDialog.open();
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow arg0) {
	}
}
