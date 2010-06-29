package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class GenerateDatamapsActionDelegate implements IViewActionDelegate {

	StructuredSelection ss;
	boolean applyAll;
	SoarDatabaseRow row;
	Shell shell;
	
	@Override
	public void init(IViewPart part) {
		
	}

	@Override
	public void run(IAction action) {
		applyAll = false;
		shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
		if (ss != null) {
			Object element = ss.getFirstElement();
			if (element != null) {
				if (element instanceof ISoarDatabaseTreeItem) {
					ISoarDatabaseTreeItem item = (ISoarDatabaseTreeItem) element;
					row = item.getRow();
					SoarDatabaseRow agent = row.getAncestorRow(Table.AGENTS);
					ArrayList<ISoarDatabaseTreeItem> problemSpaces = agent.getChildrenOfType(Table.PROBLEM_SPACES);
					for (ISoarDatabaseTreeItem psItem : problemSpaces) {
						assert psItem instanceof SoarDatabaseRow;
						SoarDatabaseRow ps = (SoarDatabaseRow) psItem;
						assert ps.getTable() == Table.PROBLEM_SPACES;
						GenerateDatamapAction generateAction = new GenerateDatamapAction(ps);
						generateAction.applyAll = applyAll;
						generateAction.run();
						applyAll = generateAction.applyAll;
					}
					return;
				}
			}
		}
		String title = "No Agent Selected";
		org.eclipse.swt.graphics.Image image = shell.getDisplay().getSystemImage(SWT.ICON_QUESTION);
		String message = "Cannot generate agent structure: No agent selected";
		String[] labels = new String[] { "OK" };
		MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.ERROR, labels, 0);
		dialog.open();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof StructuredSelection) {
			this.ss = (StructuredSelection) selection;
		}
	}

}
