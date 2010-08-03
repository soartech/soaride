package com.soartech.soar.ide.ui.actions.soarmenu;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.SoarUiModelTools;
import com.soartech.soar.ide.ui.actions.explorer.GenerateDatamapAction;

public class GenerateDatamapsActionDelegate implements IWorkbenchWindowActionDelegate {

	StructuredSelection ss;
	boolean applyAll;
	SoarDatabaseRow row;
	Shell shell;
	public boolean forceApplyAll = false;

	@Override
	public void run(IAction action) {
		SoarDatabaseRow agent = SoarUiModelTools.selectAgent();
		if (agent != null) {
			runWithAgent(agent, null);
		}
	}

	public void runWithAgent(SoarDatabaseRow agent, IProgressMonitor monitor) {
		assert agent.getTable() == Table.AGENTS;
		applyAll = forceApplyAll;
		ArrayList<SoarDatabaseRow> problemSpaces = agent.getChildrenOfType(Table.PROBLEM_SPACES);
		ArrayList<GenerateDatamapAction> actions = new ArrayList<GenerateDatamapAction>();
		int totalRules = 0;
		for (SoarDatabaseRow ps : problemSpaces) {
			assert ps.getTable() == Table.PROBLEM_SPACES;
			GenerateDatamapAction generateAction = new GenerateDatamapAction(ps, applyAll);
			actions.add(generateAction);
			totalRules += generateAction.getJoinedRulesSize();
		}
		
		if (monitor != null) {
			monitor.beginTask("Generating Datamaps", totalRules);
		}
		
		for (GenerateDatamapAction generateAction : actions) {
			if (monitor != null) {
				monitor.subTask("Problem space: " + generateAction.getProblemSpace().getName());
			}
			generateAction.run(monitor);
		}
		
		if (monitor != null) {
			monitor.done();
		}
			
	}
	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof StructuredSelection) {
			this.ss = (StructuredSelection) selection;
		}
	}

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow arg0) {
	}

}
