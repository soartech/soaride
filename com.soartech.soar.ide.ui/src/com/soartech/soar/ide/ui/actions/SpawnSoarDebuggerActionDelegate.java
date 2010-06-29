package com.soartech.soar.ide.ui.actions;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.editors.text.SoarEditorPreferencePage;
import com.soartech.soar.ide.ui.views.explorer.SoarExplorerFilter;
import com.soartech.soar.ide.ui.views.explorer.SoarExplorerView;

import edu.umich.soar.debugger.jmx.SoarCommandLineClient;
import edu.umich.soar.debugger.jmx.SoarCommandLineMXBean;


public class SpawnSoarDebuggerActionDelegate implements IWorkbenchWindowActionDelegate {

	@Override
	public void dispose() {
		
	}

	@Override
	public void init(IWorkbenchWindow arg0) {

	}

	@Override
	public void run(IAction action) {
        try {
        	SoarCommandLineClient scli = new SoarCommandLineClient("/home/miller/Applications/Soar/svn");
        } catch (Exception e) {
        	e.printStackTrace();
        }
		String path = EditorsUI.getPreferenceStore().getString(SoarEditorPreferencePage.SOAR_PATH);
		try {
		SoarCommandLineClient client = new SoarCommandLineClient("/home/miller/Applications/Soar/svn");
		SoarCommandLineMXBean proxy;
			proxy = client.startDebuggerGetProxy();
			if (proxy != null) {
				SoarCorePlugin.getDefault().getSoarModel().setCommandLineProxy(proxy);
				SoarExplorerView explorerView = (SoarExplorerView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(SoarExplorerView.ID);
				Tree tree = explorerView.getTreeViewer().getTree();
				StructuredSelection ss = (StructuredSelection) explorerView.getTreeViewer().getSelection();
				if (ss.size() > 0) {
					ISoarDatabaseTreeItem treeItem = (ISoarDatabaseTreeItem) ss.getFirstElement();
					SoarDatabaseRow treeRow = treeItem.getRow();
					SoarDatabaseRow agent = treeRow.getAncestorRow(Table.AGENTS);
					ArrayList<ISoarDatabaseTreeItem> rules = agent.getJoinedRowsFromTable(Table.RULES);
					for (ISoarDatabaseTreeItem rule : rules) {
						SoarDatabaseRow row = (SoarDatabaseRow) rule;
						if (row.getTable() == Table.RULES) {
							String text = row.getText();
							proxy.executeCommandLine(text);
						}
					}
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {

	}

}
