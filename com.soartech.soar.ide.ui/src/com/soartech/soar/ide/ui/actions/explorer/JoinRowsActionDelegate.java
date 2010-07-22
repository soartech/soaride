package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseJoinFolder;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRowFolder;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.views.SoarLabelProvider;
import com.soartech.soar.ide.ui.views.explorer.SoarExplorerView;

public class JoinRowsActionDelegate implements IObjectActionDelegate {

	SoarDatabaseJoinFolder selectedJoinFolder = null;
	IWorkbenchPart targetPart;
	IStructuredSelection ss;
	
	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
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
						SoarDatabaseRow topLevelRow = joinFolder.getRow().getTopLevelRow();
						ArrayList<SoarDatabaseRow> children = topLevelRow.getChildrenOfType(selectedJoinFolder.getTable());
						ArrayList<SoarDatabaseRow> filteredChildren = new ArrayList<SoarDatabaseRow>();
						for (SoarDatabaseRow child : children) {
							filteredChildren.add(child);
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
			ListSelectionDialog dialog = new ListSelectionDialog(shell,
					selectedJoinFolder,
					contentProvider,
					SoarLabelProvider.createFullLabelProvider(null),
					"Select items to add");
			dialog.open();
			Object[] result = dialog.getResult();

			SoarDatabaseRow thisRow = selectedJoinFolder.getRow();
			for (int i = 0; i < result.length; ++i) {
				if (result[i] instanceof SoarDatabaseRow) {
					SoarDatabaseRow selectedRow = (SoarDatabaseRow) result[i];
					SoarDatabaseRow.joinRows(thisRow, selectedRow, thisRow.getDatabaseConnection());
				}
			}
			
			if (targetPart instanceof SoarExplorerView) {
				Object element = ss.getFirstElement();
				((SoarExplorerView) targetPart).getTreeViewer().setExpandedState(element, true);
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		selectedJoinFolder = null;
		action.setEnabled(false);
		if (selection instanceof StructuredSelection) {
			ss = (StructuredSelection)selection;
			Object obj = ss.getFirstElement();
			if (obj instanceof SoarDatabaseJoinFolder) {
				selectedJoinFolder = (SoarDatabaseJoinFolder) obj;
				action.setEnabled(true);
				action.setText("Add Existing " + selectedJoinFolder.getTable().shortName() + "...");
			}
		}
	}
}
