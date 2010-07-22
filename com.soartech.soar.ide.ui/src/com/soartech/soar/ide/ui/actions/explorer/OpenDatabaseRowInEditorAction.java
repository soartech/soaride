package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.SoarUiModelTools;

public class OpenDatabaseRowInEditorAction extends Action {

	SoarDatabaseRow row;
    
    public OpenDatabaseRowInEditorAction(SoarDatabaseRow row) {
    	super(row.getTable() == Table.PROBLEM_SPACES ? "Show Datamap in Editor" : "Show in Editor");
    	this.row = row;
    }

	@Override
	public void run() {
		IWorkbenchPage page = SoarEditorUIPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (row != null) {
			Table selectedTable = row.getTable();
			if (selectedTable == Table.RULES) {
				try {
					SoarUiModelTools.showRuleInEditor(page, row);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} else if (selectedTable == Table.PROBLEM_SPACES) {
				try {
					SoarUiModelTools.showProblemSpaceInEditor(page, row);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} else if (selectedTable == Table.OPERATORS) {
				try {
					SoarUiModelTools.showOperatorInEditor(page, row);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} else if (selectedTable == Table.AGENTS) {
				try {
					SoarUiModelTools.showAgentInEditor(page, row);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
			
		}
	}
}
