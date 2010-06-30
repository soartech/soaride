package com.soartech.soar.ide.ui.actions.explorer;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.views.explorer.SoarExplorerView;

public class AddAgentActionDelegate implements IWorkbenchWindowActionDelegate {

	SoarExplorerView explorer;
	
	@Override
	public void run(IAction action) {
		
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		String title = "New Agent";
		String message = "Enter Name:";
		String initialValue = "agent";
		InputDialog dialog = new InputDialog(shell, title, message, initialValue, null);
		dialog.open();
		String result = dialog.getValue();

		if (result != null && result.length() > 0) {
			ISoarModel model = SoarCorePlugin.getDefault().getInternalSoarModel();
			model.getDatabase().insert(Table.AGENTS, new String[][] { { "name", "\"" + result + "\"" } });
			TreeViewer viewer = explorer.getTreeViewer(); 
			Tree tree = viewer.getTree();
			TreeItem[] items = tree.getItems();
			for (TreeItem item : items) {
				Object obj = item.getData();
				if (obj instanceof SoarDatabaseRow) {
					SoarDatabaseRow row = (SoarDatabaseRow) obj;
					if (row.getTable() == Table.AGENTS && row.getName().equals(result)) {
						tree.setSelection(item);
						viewer.setExpandedState(obj, true);
					}
				}
			}
		}
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow arg0) {
	}
}
