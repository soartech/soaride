package com.soartech.soar.ide.ui.actions.soarmenu;


import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.sql.SoarDatabaseConnection;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.SoarUiModelTools;
import com.soartech.soar.ide.ui.actions.explorer.AddAgentActionDelegate;

/**
 * Creates a new Soar IDE project.
 * @author miller
 *
 */
public class NewSoarProjectActionDelegate implements IWorkbenchWindowActionDelegate {

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow arg0) {
	}
	
	/**
	 * 
	 * @param warning
	 * @param addAgent
	 * @return True on success
	 */
	public boolean run(boolean warning, boolean addAgent) {
		SoarDatabaseConnection conn = SoarCorePlugin.getDefault().getDatabaseConnection();
		boolean hasAgents = conn.selectAllFromTable(Table.AGENTS, null).size() > 0;

		if (conn.isSavedToDisk() || !warning || !hasAgents) {
			SoarUiModelTools.closeAllEditors(true);
		} else {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			MessageDialog message = new MessageDialog(shell, "Create new project?", null, "Create new project? Unsaved changes will be lost.", MessageDialog.QUESTION, new String[] {"OK", "Cancel"}, 0);
			int result = message.open();
			if (result == 0) {
				SoarUiModelTools.closeAllEditors(false);
			} else {
				return false;
			}
		}
		SoarCorePlugin.getDefault().newProject();
		if (addAgent) {
			new AddAgentActionDelegate().run();
			ArrayList<SoarDatabaseRow> agents = SoarCorePlugin.getDefault().getDatabaseConnection().selectAllFromTable(Table.AGENTS, null);
			if (agents.size() > 0) {
				SoarDatabaseRow agent = agents.get(0);
				SoarDatabaseRow problemSpace = agent.createChild(Table.PROBLEM_SPACES, agent.getName());
				problemSpace.setIsRootProblemSpace(true);
			}
		}
		return true;
	}

	@Override
	public void run(IAction action) {
		run(true, true);
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

}
