package com.soartech.soar.ide.ui.actions.explorer;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Scanner;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
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
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.core.sql.SoarDatabaseUtil;
import com.soartech.soar.ide.ui.SoarUiModelTools;
import com.soartech.soar.ide.ui.actions.soarmenu.GenerateDatamapsActionDelegate;

public class LoadMultipleProjectsActionDelegate implements IWorkbenchWindowActionDelegate {
	
	@Override
	public void run(IAction action) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		FileDialog dialog = new FileDialog(shell, SWT.OPEN);
		String path = dialog.open();
		if (path == null || path.length() == 0) return;
		File file = new File(path);
		if (!file.exists()) {
			return;
		}
		String rootPath = file.getParent();
		Scanner scanner = null;
		ArrayList<String> errors = new ArrayList<String>();
		try {
			scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.length() == 0 || line.trim().startsWith("#")) {
					continue;
				}
				File testFile = new File(rootPath + "/" + line);
				if (!file.exists()) {
					errors.add("Could not load test file, file not found: " + rootPath + "/" + line);
					continue;
				}
				String filePath = rootPath + "/" + line;
				copiedPart(filePath, testFile);
				System.out.println("Sucessfully loaded project at " + filePath);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		for (String error : errors) {
			System.out.println(error);
		}
		if (scanner != null) {
			scanner.close();
		}
	}
	
	/**
	 * Copied from NewSoarProjectFromSourceActionDelegate
	 */
	private void copiedPart(String path, File file) {
		final File finalFile = file;
		boolean savedToDisk = SoarCorePlugin.getDefault().getDatabaseConnection().isSavedToDisk();
		if (savedToDisk) {
			SoarUiModelTools.closeAllEditors(true);
		} else {
			SoarUiModelTools.closeAllEditors(false);
		}
		
		SoarCorePlugin.getDefault().newProject();
		
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
		ArrayList<SoarDatabaseRow> agents = SoarCorePlugin.getDefault().getDatabaseConnection().selectAllFromTable(Table.AGENTS);
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
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		try {
			new ProgressMonitorDialog(shell).run(false, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					//monitor.setTaskName("New Project From Existing Source");
					monitor.beginTask("Importing Rules", IProgressMonitor.UNKNOWN);
					System.out.println("About to import rules from file: " + finalFile.getPath());
					ArrayList<String> errors = SoarDatabaseUtil.importRules(finalFile, finalAgent, monitor);
					reportErrors(errors);
					monitor.done();
				}
			});
			
			/*
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
			*/
			
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		agent.getDatabaseConnection().setSupressEvents(eventsWereSupressed);
	}
	
	private void reportErrors(ArrayList<String> errors) {
		for (String error : errors) {
			System.out.println(error);
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub
		
	}
}
