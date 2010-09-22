package com.soartech.soar.ide.ui.actions.soarmenu;

import java.io.IOException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.ui.preferences.SoarEditorPreferencePage;

import edu.umich.soar.debugger.jmx.SoarCommandLineClient;
import edu.umich.soar.debugger.jmx.SoarCommandLineMXBean;

/**
 * Spawns an instance of the Soar Debugger and connects it to the IDE.
 * @author miller
 *
 */
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
			String path = EditorsUI.getPreferenceStore().getString(SoarEditorPreferencePage.SOAR_PATH);
			SoarCommandLineClient client = new SoarCommandLineClient(path);
			SoarCommandLineMXBean proxy;		
			proxy = client.startDebuggerGetProxy();
			if (proxy != null) {
				SoarCorePlugin.getDefault().setCommandLineProxy(proxy);
			}
		} catch (IOException e) {
			//e.printStackTrace();
			MessageDialog dialog = new MessageDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					"Unable to Spawn Debugger",
					null,
					"Check that the path to your Soar installation is set correctly under"
					+ " \"Window > Preferences ... Soar Editor ... Path to Soar Installation\"."
					+ "\n\nThe path you specify should contain the bin folder.",
					MessageDialog.ERROR,
					new String[] {"Ok"},
					0);
			dialog.open();
		}
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {

	}

}
