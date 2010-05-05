package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseJoinFolder;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.views.SoarLabelProvider;

public class RemoveJoinedRowActionDelegate implements IObjectActionDelegate {

	SoarDatabaseJoinFolder selectedJoinFolder = null;
	
	@Override
	public void setActivePart(IAction action, IWorkbenchPart part) {

	}

	@Override
	public void run(IAction action) {
		if (selectedJoinFolder != null) {

			IStructuredContentProvider contentProvider = new IStructuredContentProvider() {

				@Override
				public Object[] getElements(Object obj) {
					if (obj instanceof SoarDatabaseJoinFolder) {
						SoarDatabaseJoinFolder joinFolder = (SoarDatabaseJoinFolder) obj;
						Table table = joinFolder.getTable();
						ArrayList<ISoarDatabaseTreeItem> children = joinFolder.getRow().getJoinedRowsFromTable(table);
						ArrayList<SoarDatabaseRow> filteredChildren = new ArrayList<SoarDatabaseRow>();
						for (ISoarDatabaseTreeItem child : children) {
							if (child instanceof SoarDatabaseRow) {
								filteredChildren.add((SoarDatabaseRow) child);
							}
						}
						return filteredChildren.toArray();
					}
					return new Object[0];
				}

				@Override
				public void dispose() {
				}

				@Override
				public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
				}
				
			};

			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			ListDialog dialog = new ListDialog(shell);
			dialog.setContentProvider(contentProvider);
			dialog.setLabelProvider(SoarLabelProvider.createFullLabelProvider(null));
			dialog.setInput(selectedJoinFolder);
			dialog.open();
			Object[] result = dialog.getResult();
			if (result.length > 0 && result[0] instanceof SoarDatabaseRow) {
				SoarDatabaseRow thisRow = selectedJoinFolder.getRow();
				SoarDatabaseRow selectedRow = (SoarDatabaseRow) result[0];
				SoarDatabaseRow.unjoinRows(thisRow, selectedRow, thisRow.getDatabaseConnection());
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		selectedJoinFolder = null;
		action.setEnabled(false);
		if (selection instanceof StructuredSelection) {
			StructuredSelection ss = (StructuredSelection)selection;
			Object obj = ss.getFirstElement();
			if (obj instanceof SoarDatabaseJoinFolder) {
				selectedJoinFolder = (SoarDatabaseJoinFolder) obj;
				action.setEnabled(true);
				action.setText("Remove Existing " + selectedJoinFolder.getTable().shortName() + "...");
			}
		}
	}

}
