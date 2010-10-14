package com.soartech.soar.ide.ui.actions.explorer;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.SoarUiModelTools;

/**
 * Deletes a row.
 * @author miller
 *
 */
public class DeleteDatabaseRowAction extends Action {

	SoarDatabaseRow row;
	
	public DeleteDatabaseRowAction(SoarDatabaseRow row) {
		super("Delete");
		this.row = row;
	}

	@Override
	public void run() {
		run(true, false);
	}
	
	/**
	 * 
	 * @param deleteAllOption Whether the option "Delete all" should be displayed.
	 * @return True if the user selected "Delete all"
	 */
	public boolean run(boolean prompt, boolean deleteAllOption) {
		boolean ret = false;
		if (prompt) {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			String title = "Delete item?";
			org.eclipse.swt.graphics.Image image = shell.getDisplay().getSystemImage(SWT.ICON_QUESTION);
			String message = "Are you sure you want to delete \"" + row.getName() + "\"?\nThis action cannot be undone.";
			String[] labels = null;
			int cancelIndex;
			int deleteAllIndex;
			if (!deleteAllOption) {
				labels = new String[] { "OK", "Cancel" };
				cancelIndex = 1;
				deleteAllIndex = -1;
			} else {
				labels = new String[] { "OK", "Delete All", "Cancel" };
				cancelIndex = 2;
				deleteAllIndex = 1;
			}
			MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.QUESTION, labels, 0);
			int result = dialog.open();
			if (result == cancelIndex) {
				return false;
			}
			if (result == deleteAllIndex) {
				ret = true;
			}
		}
		
		IWorkbench workbench = SoarEditorUIPlugin.getDefault().getWorkbench();
        IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
		SoarUiModelTools.closeEditorsForInput(page, row, false);
		
		try {
			new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()).run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					row.deleteAllChildren(true, monitor);
				}
			});
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return ret;
	}
}
