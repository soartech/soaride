package com.soartech.soar.ide.ui.actions.soarmenu;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
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
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseUtil;
import com.soartech.soar.ide.core.sql.TraversalUtil;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent.Type;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.actions.explorer.GenerateAgentStructureActionDelegate;

/**
 * This class is misnamed -- it actually creates a new Agent in the current Soar IDE project,
 * loads rules into that agent from a source file chosen by the user, generates project structure
 * for the agent, and generates datamaps for the problem spaces in the agent.
 * @author miller
 *
 */
public class NewSoarProjectFromSourceActionDelegate implements IWorkbenchWindowActionDelegate {
	
	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow arg0) {
	}

	@Override
	public void run(IAction action) {
		
		if (!new NewSoarProjectActionDelegate().run(true, false)) {
			return;
		}

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		
		/*
		boolean savedToDisk = SoarCorePlugin.getDefault().getDatabaseConnection().isSavedToDisk();
		
		if (!savedToDisk) {
			MessageDialog message = new MessageDialog(shell, "Create new project?", null, "Create new project? Unsaved changes will be lost.", MessageDialog.QUESTION, new String[] {"OK", "Cancel"}, 0);
			int result = message.open();
			if (result != 0) {
				return;
			}
		}
		*/
		
		FileDialog dialog = new FileDialog(shell, SWT.OPEN);
		dialog.setText("Choose Source File To Import");
		String path = dialog.open();
		if (path == null) {
			return;
		}
		final File file = new File(path);
		if (file == null || !file.exists()) {
			return;
		}
		
		/*
		if (savedToDisk) {
			SoarUiModelTools.closeAllEditors(true);
		} else {
			SoarUiModelTools.closeAllEditors(false);
		}
		*/
		
		//SoarCorePlugin.getDefault().newProject();
		
		// New agent
		int lastSlashIndex = path.lastIndexOf(File.separatorChar) + 1;
		int lastDotIndex = path.lastIndexOf('.');
		if (lastDotIndex == -1) {
			lastDotIndex = path.length();
		}
		String agentName = path.substring(lastSlashIndex, lastDotIndex);
		SoarCorePlugin.getDefault().getDatabaseConnection().insert(Table.AGENTS, new String[][] { { "name", "\"" + agentName + "\"" } });
		
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
		ArrayList<SoarDatabaseRow> agents = SoarCorePlugin.getDefault().getDatabaseConnection().selectAllFromTable(Table.AGENTS, "name");
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
		agent.getDatabaseConnection().pushSuppressEvents();
		final ArrayList<String> errors = new ArrayList<String>();
		try {
			try {
				new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						monitor.beginTask("Parsing Rules", SoarDatabaseUtil.countRulesFromFile(file, errors));
						errors.addAll(SoarDatabaseUtil.importRules(file, finalAgent, monitor));
						monitor.done();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}

			new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					// Create project structure
					GenerateAgentStructureActionDelegate structure = new GenerateAgentStructureActionDelegate();
					structure.forceApplyAll = true;
					errors.addAll(structure.runWithAgent(finalAgent, monitor));
				}
			});
			
			new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					// Generate datamap structure
					GenerateDatamapsActionDelegate datamaps = new GenerateDatamapsActionDelegate();
					datamaps.forceApplyAll = true;
					datamaps.runWithAgent(finalAgent, monitor);
				}
			});
			
			// Find problem spaces that match <s>.superstate nil
			// Make them root.
			String[] superstatePath = new String[] {"superstate"};
			for (SoarDatabaseRow problemSpace : agent.getChildrenOfType(Table.PROBLEM_SPACES)) {
				if (TraversalUtil.problemSpaceMatchesAttributePath(problemSpace, superstatePath, "nil")) {
					problemSpace.updateValue("is_root", "1");
				}
			}
			
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			if (errors.size() > 0) {
				String message = "Encountered the following errors during the import operation:";
				for (String error : errors) {
					message += "\n" + error;
				}
				ErrorDialog errorDialog = new ErrorDialog(shell, "Error", message, Status.OK_STATUS, 0);
				errorDialog.open();
			}
		}
		agent.getDatabaseConnection().popSuppressEvents();
		agent.getDatabaseConnection().fireEvent(new SoarDatabaseEvent(Type.DATABASE_CHANGED));
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
	}

}
