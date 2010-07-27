package com.soartech.soar.ide.ui.editors.database.dragdrop;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.ui.actions.explorer.LinkDatamapRowsAction;

public class SoarDatabaseDatamapDropAdapter extends ViewerDropAdapter {

	SoarDatabaseRow target;
	
	public SoarDatabaseDatamapDropAdapter(Viewer viewer) {
		super(viewer);
	}

	@Override
	public boolean performDrop(Object data) {
		if (!(data instanceof StructuredSelection)) {
			return false;
		}
		
		StructuredSelection ss = (StructuredSelection) data;
		Object dataObj = ss.getFirstElement();
		
		if (!(dataObj instanceof TreeItem)) {
			return false;
		}
		
		TreeItem treeItem = (TreeItem) dataObj;
		
		if (!(treeItem.getData() instanceof SoarDatabaseRow)) {
			return false;
		}
		
		SoarDatabaseRow dataRow = (SoarDatabaseRow) treeItem.getData();
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		String title = "Link items?";
		org.eclipse.swt.graphics.Image image = shell.getDisplay().getSystemImage(SWT.ICON_QUESTION);
		String message = "Link items \"" + dataRow.getName() + "\" and \"" + target.getName() + "\"?"; 
		String[] labels = new String[] { "OK", "Cancel" };
		MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.QUESTION, labels, 0);
		int result = dialog.open();
		if (result == 1) {
			return false;
		}
		
		LinkDatamapRowsAction action = new LinkDatamapRowsAction(dataRow, target);
		action.run();
		
		return true;
	}

	@Override
	public boolean validateDrop(Object target, int operation, TransferData targetType) {
		this.target = null;

		Object obj = super.getSelectedObject();
		if (!(obj instanceof SoarDatabaseRow)) {
			return false;
		}
		SoarDatabaseRow first = (SoarDatabaseRow) obj;
		if (!first.getTable().isDatamapTable()) {
			return false;
		}
		if (!(target instanceof SoarDatabaseRow)) {
			return false;
		}
		SoarDatabaseRow second = (SoarDatabaseRow) target;
		if (first.getTable() != second.getTable()) {
			return false;
		}
		this.target = second;
		return true;
	}
}
