package com.soartech.soar.ide.ui.actions.explorer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;

public class RemoveJoinFromParentAction extends Action {

	public SoarDatabaseRow parent = null;
	public SoarDatabaseRow row;
	
	public RemoveJoinFromParentAction(TreeSelection selection) {
		super();
		Object obj = selection.getFirstElement();
		if (obj instanceof SoarDatabaseRow) {
			row = (SoarDatabaseRow) obj;
		}
		TreePath[] paths = selection.getPathsFor(row);
		if (paths.length > 0) {
			TreePath path = paths[0];
			int pathLength = path.getSegmentCount();
			if (pathLength > 2) {
				Object parentObj = path.getSegment(pathLength - 2);
				if (parentObj instanceof SoarDatabaseRow) {
					parent = (SoarDatabaseRow) parentObj;
					super.setText("Remove From \"" + parent.getName() + "\"");
				}
			}
		}
	}
	
	@Override
	public void run() {
		super.run();
		if (parent != null) {
			
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			String title = "Remove item?";
			org.eclipse.swt.graphics.Image image = shell.getDisplay().getSystemImage(SWT.ICON_QUESTION);
			String message = "Are you sure you want to remove \"" + row.getName() + "\" from \"" + parent.getName() + "\"?";
			String[] labels = new String[] { "OK", "Cancel" };
			MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.QUESTION, labels, 0);
			int result = dialog.open();
			if (result == 1) {
				return;
			}
			
			if (SoarDatabaseRow.rowsAreJoined(parent, row, parent.getDatabaseConnection())) {
				SoarDatabaseRow.unjoinRows(parent, row, parent.getDatabaseConnection());
			} else if (SoarDatabaseRow.rowsAreDirectedJoined(parent, row, parent.getDatabaseConnection())) {
				SoarDatabaseRow.directedUnjoinRows(parent, row, parent.getDatabaseConnection());
			}
		}
	}
	
	public boolean isRunnable() {
		return parent != null;
	}
}
