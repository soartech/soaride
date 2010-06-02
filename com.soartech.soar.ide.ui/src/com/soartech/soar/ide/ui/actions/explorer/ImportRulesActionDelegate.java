package com.soartech.soar.ide.ui.actions.explorer;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.soartech.soar.ide.core.sql.SoarDatabaseJoinFolder;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRowFolder;
import com.soartech.soar.ide.core.sql.SoarDatabaseUtil;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;

public class ImportRulesActionDelegate implements IObjectActionDelegate {

	StructuredSelection ss;
	IWorkbenchPart targetPart;
	SoarDatabaseRowFolder selectedFolder = null;
	SoarDatabaseJoinFolder selectedJoinFolder = null;
	Shell shell;
	
	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
		shell = targetPart.getSite().getShell();
	}

	@Override
	public void run(IAction action) {
		assert action.isEnabled();
		FileDialog dialog = new FileDialog(shell);
		dialog.setText("Choose a Soar file to import");
		String path = dialog.open();
		File file = new File(path);
		if (file.exists() && file.canRead()) {
			SoarDatabaseRow agent = null;
			if (selectedFolder != null) {
				agent = selectedFolder.getRow().getAncestorRow(Table.AGENTS);
			}
			else if (selectedJoinFolder != null) {
				agent = selectedJoinFolder.getRow().getAncestorRow(Table.AGENTS);
			}
			if (agent != null) {
				ArrayList<SoarDatabaseRow> newRows = SoarDatabaseUtil.importRules(file, agent);
				if (selectedJoinFolder != null) {
					for (SoarDatabaseRow row : newRows) {
						SoarDatabaseRow.joinRows(selectedJoinFolder.getRow(), row, row.getDatabaseConnection());
					}
				}
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		selectedFolder = null;
		selectedJoinFolder = null;
		action.setEnabled(false);
		if (selection instanceof StructuredSelection) {
			ss = (StructuredSelection)selection;
			Object obj = ss.getFirstElement();
			if (obj instanceof SoarDatabaseRowFolder) {
				selectedFolder = (SoarDatabaseRowFolder) obj;
				if (selectedFolder.getTable() == Table.RULES) {
					action.setEnabled(true);
				}
			}
			if (obj instanceof SoarDatabaseJoinFolder) {
				selectedJoinFolder = (SoarDatabaseJoinFolder) obj;
				if (selectedJoinFolder.getTable() == Table.RULES) {
					action.setEnabled(true);
				}
			}
		}
	}
}
