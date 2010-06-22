package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.SoarUiModelTools;

public class OpenDatabaseRowInEditorActionDelegate implements
		IObjectActionDelegate {

	private SoarDatabaseRow selectedRow = null;
    private IWorkbenchPart targetPart;

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		action.setText("Show in editor");
        this.targetPart = targetPart;
	}

	@Override
	public void run(IAction action) {
		// TODO Auto-generated method stub
		if (selectedRow != null) {
			Table selectedTable = selectedRow.getTable();
			if (selectedTable == Table.RULES) {
				try {
					SoarUiModelTools.showRuleInEditor(targetPart.getSite().getPage(), selectedRow);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} else if (selectedTable == Table.PROBLEM_SPACES) {
				try {
					SoarUiModelTools.showProblemSpaceInEditor(targetPart.getSite().getPage(), selectedRow);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} else if (selectedTable == Table.OPERATORS) {
				try {
					SoarUiModelTools.showOperatorInEditor(targetPart.getSite().getPage(), selectedRow);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} else if (selectedTable == Table.AGENTS) {
				try {
					SoarUiModelTools.showAgentInEditor(targetPart.getSite().getPage(), selectedRow);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
			
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		selectedRow = null;
		action.setEnabled(false);
		if (selection instanceof StructuredSelection) {
			StructuredSelection ss = (StructuredSelection)selection;
			Object obj = ss.getFirstElement();
			if (obj instanceof SoarDatabaseRow) {
				selectedRow = (SoarDatabaseRow) obj;
				Table selectedTable = selectedRow.getTable();
				if (selectedTable == Table.RULES
						|| selectedTable == Table.PROBLEM_SPACES
						|| selectedTable == Table.OPERATORS
						|| selectedTable == Table.AGENTS) {
					action.setEnabled(true);
					if (selectedTable == Table.PROBLEM_SPACES) {
						action.setText("Show datamap in editor");
					} else {
						action.setText("Show " + selectedTable.englishName() + " in editor");
					}
				}
			}
			else {
				selectedRow = null;
			}
		}
	}
}
