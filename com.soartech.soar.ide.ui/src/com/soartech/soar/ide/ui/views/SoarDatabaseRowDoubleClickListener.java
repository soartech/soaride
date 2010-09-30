package com.soartech.soar.ide.ui.views;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.core.sql.datamap.DatamapInconsistency;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.SoarUiModelTools;
import com.soartech.soar.ide.ui.editors.datamap.SoarDatabaseDatamapEditor;

public class SoarDatabaseRowDoubleClickListener implements IDoubleClickListener {

	@Override
	public void doubleClick(DoubleClickEvent event) {
		ISelection selection = event.getSelection();
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}
		IStructuredSelection ss = (IStructuredSelection) selection;
		Object obj = ss.getFirstElement();
		if (!(obj instanceof ISoarDatabaseTreeItem ||
				obj instanceof DatamapInconsistency)) {
			return;
		}
		if (obj instanceof DatamapInconsistency) {
			try {
				IWorkbench workbench = SoarEditorUIPlugin.getDefault().getWorkbench();
				IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
				SoarUiModelTools.showRuleInEditor(page, ((DatamapInconsistency) obj).rule);
			} catch (CoreException e) {
				e.printStackTrace();
			}
			return;
		}
		ISoarDatabaseTreeItem item = (ISoarDatabaseTreeItem) obj;
		
		if (item instanceof SoarDatabaseRow) {
	        IWorkbench workbench = SoarEditorUIPlugin.getDefault().getWorkbench();
	        IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
			SoarDatabaseRow selectedRow = item.getRow();
			Table selectedTable = selectedRow.getTable();
			if (selectedTable == Table.RULES) {
				try {
					SoarUiModelTools.showRuleInEditor(page, selectedRow);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} else if (selectedTable == Table.OPERATORS) {
				try {
					SoarUiModelTools.showOperatorInEditor(page, selectedRow);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} else if (selectedTable == Table.PROBLEM_SPACES) {
				try {
					SoarUiModelTools.showProblemSpaceInEditor(page, selectedRow);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} else if (selectedTable == Table.AGENTS) {
				try {
					SoarUiModelTools.showAgentInEditor(page, selectedRow);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} else if (selectedTable == Table.TAGS) {
				try {
					SoarUiModelTools.showTagInEditor(page, selectedRow);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} else if (selectedTable.isDatamapTable()) {
				SoarDatabaseRow problemSpace = selectedRow.getAncestorRow(Table.PROBLEM_SPACES);
				IEditorPart part = null;
				try {
					part = SoarUiModelTools.showProblemSpaceInEditor(page, problemSpace);
				} catch (CoreException e) {
					e.printStackTrace();
				}
				// TODO
				// open tree in editor to the selected node
				/*
				if (part != null && part instanceof SoarDatabaseDatamapEditor) {
					SoarDatabaseDatamapEditor datamapEditor = (SoarDatabaseDatamapEditor) part;
					TreeViewer tree = datamapEditor.getTree();
				}
				*/
			}
		}
	}
}
