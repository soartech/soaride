package com.soartech.soar.ide.ui.actions.soarmenu;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseUtil;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.SoarUiModelTools;
import com.soartech.soar.ide.ui.actions.explorer.GenerateAgentStructureActionDelegate;

public class NewSoarProjectFromSourceActionDelegate implements IWorkbenchWindowActionDelegate {
	
	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow arg0) {
	}

	@Override
	public void run(IAction action) {
		
		boolean savedToDisk = SoarCorePlugin.getDefault().getSoarModel().getDatabase().isSavedToDisk();
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		
		if (!savedToDisk) {
			MessageDialog message = new MessageDialog(shell, "Create new project?", null, "Create new project? Unsaved changes will be lost.", MessageDialog.QUESTION, new String[] {"OK", "Cancel"}, 0);
			int result = message.open();
			if (result != 0) {
				return;
			}
		}
		
		FileDialog dialog = new FileDialog(shell, SWT.OPEN);
		String path = dialog.open();
		if (path == null) {
			return;
		}
		final File file = new File(path);
		if (file == null || !file.exists()) {
			return;
		}
		
		if (savedToDisk) {
			SoarUiModelTools.closeAllEditors(true);
		} else {
			SoarUiModelTools.closeAllEditors(false);
		}
		
		SoarCorePlugin.getDefault().newProject();
		
		// New agent
		int lastSlashIndex = path.lastIndexOf('/') + 1;
		int lastDotIndex = path.lastIndexOf('.');
		if (lastDotIndex == -1) {
			lastDotIndex = path.length();
		}
		String agentName = path.substring(lastSlashIndex, lastDotIndex);
		ISoarModel model = SoarCorePlugin.getDefault().getInternalSoarModel();
		model.getDatabase().insert(Table.AGENTS, new String[][] { { "name", "\"" + agentName + "\"" } });
		
		// TODO
		// expand the agent in the tree view
		/*
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
		*/
		
		// Import rules
		ArrayList<SoarDatabaseRow> agents = model.getDatabase().selectAllFromTable(Table.AGENTS);
		SoarDatabaseRow agent = null;
		for (SoarDatabaseRow row : agents) {
			if (row.getName().equals(agentName)) {
				agent = row;
				break;
			}
		}
		if (agent == null) {
			// shouldn't happen
			return;
		}
		
		final SoarDatabaseRow finalAgent = agent;
		boolean eventsWereSupressed = agent.getDatabaseConnection().getSupressEvents();
		agent.getDatabaseConnection().setSupressEvents(true);
		try {
			new ProgressMonitorDialog(shell).run(false, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					//monitor.setTaskName("New Project From Existing Source");
					monitor.beginTask("Importing Rules", IProgressMonitor.UNKNOWN);
					SoarDatabaseUtil.importRules(file, finalAgent, monitor);
					monitor.done();
				}
			});
			
			new ProgressMonitorDialog(shell).run(false, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					// Create project structure
					GenerateAgentStructureActionDelegate structure = new GenerateAgentStructureActionDelegate();
					structure.forceApplyAll = true;
					structure.runWithAgent(finalAgent, monitor);
				}
			});
			
			new ProgressMonitorDialog(shell).run(false, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					// Generate datamap structure
					GenerateDatamapsActionDelegate datamaps = new GenerateDatamapsActionDelegate();
					datamaps.forceApplyAll = true;
					datamaps.runWithAgent(finalAgent, monitor);
				}
			});
			
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		agent.getDatabaseConnection().setSupressEvents(eventsWereSupressed);
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
	}

}
