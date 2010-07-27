package com.soartech.soar.ide.ui.actions.soarmenu;

import java.io.IOException;
import java.sql.SQLException;
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
import com.soartech.soar.ide.ui.preferences.SoarEditorPreferencePage;
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
		String path = EditorsUI.getPreferenceStore().getString(SoarEditorPreferencePage.SOAR_PATH);
		SoarCommandLineClient client = new SoarCommandLineClient(path);
		SoarCommandLineMXBean proxy;
		try {
			proxy = client.startDebuggerGetProxy();
			if (proxy != null) {
				SoarCorePlugin.getDefault().getSoarModel().setCommandLineProxy(proxy);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {

	}

}
