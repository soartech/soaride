package com.soartech.soar.ide.ui.actions.explorer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.SoarUiModelTools;

public class DeleteDatabaseRowAction extends Action {

	SoarDatabaseRow row;
	
	public DeleteDatabaseRowAction(SoarDatabaseRow row) {
		super("Delete");
		this.row = row;
	}

	@Override
	public void run() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		String title = "Delete item?";
		org.eclipse.swt.graphics.Image image = shell.getDisplay().getSystemImage(SWT.ICON_QUESTION);
		String message = "Are you sure you want to delete \"" + row.getName() + "\"?\nThis action cannot be undone.";
		String[] labels = new String[] { "OK", "Cancel" };
		MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.QUESTION, labels, 0);
		int result = dialog.open();
		if (result == 1) {
			return;
		}
		
		IWorkbench workbench = SoarEditorUIPlugin.getDefault().getWorkbench();
        IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
		SoarUiModelTools.closeEditorsForInput(page, row, false);
		
		row.deleteAllChildren(true);
	}
}
