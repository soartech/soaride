package com.soartech.soar.ide.ui.actions.explorer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;

/**
 * Renames a row.
 * @author miller
 *
 */
public class RenameDatabaseRowAction extends Action {

	SoarDatabaseRow row;
	
	public RenameDatabaseRowAction(SoarDatabaseRow row) {
		super("Rename");
		this.row = row;
	}

	@Override
	public void run() {
		if (row != null) {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow() .getShell();
			String title = "Rename";
			String message = "Enter New Name:";
			String initialValue = row.toString();
			InputDialog dialog = new InputDialog(shell, title, message, initialValue, null);
			dialog.open();
			String result = dialog.getValue();
			if (result != null && result.length() > 0) {
				row.setName(result);
			}
		}
	}
}
