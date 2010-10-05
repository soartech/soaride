package com.soartech.soar.ide.ui.actions.soarmenu;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.ProgressMonitor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.core.sql.datamap.DatamapInconsistency;
import com.soartech.soar.ide.core.sql.datamap.DatamapUtil;
import com.soartech.soar.ide.ui.SoarUiModelTools;
import com.soartech.soar.ide.ui.views.search.SoarDatabaseSearchResultsView;

public class CheckRulesAgainstDatamapsActionDelegate implements IWorkbenchWindowActionDelegate {

	@Override
	public void run(IAction action) {
		final SoarDatabaseRow agent = SoarUiModelTools.selectAgent();
		if (agent == null) return;
		final ArrayList<DatamapInconsistency> errors = new ArrayList<DatamapInconsistency>();
		
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		try {
			new ProgressMonitorDialog(shell).run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					ArrayList<SoarDatabaseRow> rules = agent.getChildrenOfType(Table.RULES);
					monitor.beginTask("Checking rules against datamaps", rules.size());
					for (SoarDatabaseRow rule : rules) {
						monitor.subTask(rule.getName());
						errors.addAll(DatamapUtil.getInconsistancies(rule));
						monitor.worked(1);
					}
					monitor.done();
				}
			});
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (errors.size() > 0) {
			SoarDatabaseSearchResultsView.setResults(errors.toArray());
		} else {
			MessageDialog dialog = new MessageDialog(shell,
					"No Inconsistencies Found",
					null,
					"All rules check out against datamaps.",
					MessageDialog.INFORMATION,
					new String[] { "OK" }, 0);
			dialog.open();
			SoarDatabaseSearchResultsView.setResults(new Object[0]);
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow window) {
	}

}
