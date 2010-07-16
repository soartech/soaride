package com.soartech.soar.ide.ui.actions.explorer;

import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.SoarUiModelTools;
import com.soartech.soar.ide.ui.actions.NewGenerateDatamapAction;

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
			runWithAgent(agent);
		}
	}

	public void runWithAgent(SoarDatabaseRow agent) {
		assert agent.getTable() == Table.AGENTS;
		applyAll = forceApplyAll;
		ArrayList<ISoarDatabaseTreeItem> problemSpaces = agent.getChildrenOfType(Table.PROBLEM_SPACES);
		for (ISoarDatabaseTreeItem psItem : problemSpaces) {
			assert psItem instanceof SoarDatabaseRow;
			SoarDatabaseRow ps = (SoarDatabaseRow) psItem;
			assert ps.getTable() == Table.PROBLEM_SPACES;
			NewGenerateDatamapAction generateAction = new NewGenerateDatamapAction(ps, applyAll);
			generateAction.run();
			applyAll = generateAction.applyAll;
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
