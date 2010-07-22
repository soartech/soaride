package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.views.SoarLabelProvider;

public class AddChildRowAction extends Action {

	SoarDatabaseRow parent;
	Table childTable;
	TreeViewer tree;
	ISoarDatabaseTreeItem treeItem;
	boolean directed;

	public AddChildRowAction(SoarDatabaseRow parent, Table childTable, ISoarDatabaseTreeItem treeItem, TreeViewer tree, boolean directed, String displayName) {
		super(displayName);
		this.parent = parent;
		this.tree = tree;
		this.childTable = childTable;
		this.treeItem = treeItem;
		this.directed = directed;
	}
	
	public AddChildRowAction(SoarDatabaseRow parent, Table childTable, ISoarDatabaseTreeItem treeItem, TreeViewer tree, boolean directed) {
		this(parent, childTable, treeItem, tree, directed, "Add New " + childTable.englishName());
	}

	@Override
	public void run() {
		runNew();
	}
	
	private void runNew() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		String title = "New " + childTable.englishName();
		String message = "Enter Name:";
		String initialValue = childTable.soarName() + "-name";
		InputDialog dialog = new InputDialog(shell, title, message, initialValue, null);
		dialog.open();
		String result = dialog.getValue();

		if (result != null && result.length() > 0) {
			SoarDatabaseRow top = parent.getTopLevelRow();
			if (top == parent) {
				parent.createChild(childTable, result);
			} else {
				SoarDatabaseRow child = top.createChild(childTable, result);
				if (directed) {
					SoarDatabaseRow.directedJoinRows(parent, child, parent.getDatabaseConnection());
				} else {
					SoarDatabaseRow.joinRows(parent, child, parent.getDatabaseConnection());
				}
			}

			if (treeItem != null) {
				tree.setExpandedState(treeItem, true);
			}
		}
	}

	/*
	private void runExisting() {
		
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object obj) {
				if (obj instanceof SoarDatabaseRow) {
					SoarDatabaseRow row = (SoarDatabaseRow) obj;
					SoarDatabaseRow topLevelRow = row.getTopLevelRow();
					ArrayList<ISoarDatabaseTreeItem> children = topLevelRow.getChildrenOfType(childTable);
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

		ListDialog dialog = new ListDialog(shell);
		dialog.setContentProvider(contentProvider);
		dialog.setLabelProvider(SoarLabelProvider.createFullLabelProvider(null));
		dialog.setTitle("Select child");
		dialog.setInput(parent);
		dialog.open();
		Object[] result = dialog.getResult();
		if (result[0] instanceof SoarDatabaseRow) {
			SoarDatabaseRow selectedRow = (SoarDatabaseRow) result[0];
			if (selectedRow.getTable() == childTable) {
				// Should be true
				SoarDatabaseRow.joinRows(parent, selectedRow, parent.getDatabaseConnection());
			}
		}
	}
	*/
}
