package com.soartech.soar.ide.ui.actions.explorer;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Scanner;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
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
		
		// New agent
		int lastSlashIndex = path.lastIndexOf(File.separatorChar) + 1;
		int lastDotIndex = path.lastIndexOf('.');
		if (lastDotIndex == -1) {
			lastDotIndex = path.length();
		}
		String agentName = path.substring(lastSlashIndex, lastDotIndex);
		SoarCorePlugin.getDefault().getDatabaseConnection().insert(Table.AGENTS, new String[][] { { "name", "\"" + agentName + "\"" } });
		
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
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		final ArrayList<String> errors = new ArrayList<String>();
		System.out.println("Beginning import of: " + finalFile.getPath());
		try {
			
			try {
				new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						// monitor.setTaskName("New Project From Existing Source");
						monitor.beginTask("Counting Rules", IProgressMonitor.UNKNOWN);
						monitor.beginTask("Parsing Rules", SoarDatabaseUtil.countRulesFromFile(finalFile, errors));
						errors.addAll(SoarDatabaseUtil.importRules(finalFile, finalAgent, monitor));
						System.out.println("Loaded rules for file: " + finalFile.getPath());
						monitor.done();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			new ProgressMonitorDialog(shell).run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					// Create project structure
					GenerateAgentStructureActionDelegate structure = new GenerateAgentStructureActionDelegate();
					structure.forceApplyAll = true;
					structure.runWithAgent(finalAgent, monitor);
					System.out.println("Generated Structure for file: " + finalFile.getPath());
				}
			});
			
			new ProgressMonitorDialog(shell).run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					// Generate datamap structure
					GenerateDatamapsActionDelegate datamaps = new GenerateDatamapsActionDelegate();
					datamaps.forceApplyAll = true;
					datamaps.runWithAgent(finalAgent, monitor);
					System.out.println("Generated Datamaps for file: " + finalFile.getPath());
				}
			});
			
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (errors.size() > 0) {
				String message = "Encountered the following errors during the import operation:\n";
				for (String error : errors) {
					message += "\n" + error;
				}
				MessageDialog dialog = new MessageDialog(shell, "Errors During Import", null, message, MessageDialog.ERROR, new String[] {"Ok"}, 0);
				dialog.open();
				/*
				ErrorDialog errorDialog = new ErrorDialog(shell, "Error", message, Status.OK_STATUS, 0);
				errorDialog.open();
				*/
			}
		}
		agent.getDatabaseConnection().popSuppressEvents();
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
