package com.soartech.soar.ide.ui.actions.soarmenu;

import java.io.IOException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
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
		boolean doit = true;
		boolean replacedPath = false;
		String path = EditorsUI.getPreferenceStore().getString(SoarEditorPreferencePage.SOAR_PATH);
		while (doit) {
			try {
				//-Djava.rmi.server.hostname=localhost
				SoarCommandLineClient client = new SoarCommandLineClient(path);
				SoarCommandLineMXBean proxy;
				proxy = client.startDebuggerGetProxy();
				if (proxy != null) {
					SoarCorePlugin.getDefault().setCommandLineProxy(proxy);
				}
				if (replacedPath) {
					EditorsUI.getPreferenceStore().setValue(SoarEditorPreferencePage.SOAR_PATH, path);
				}
				doit = false;
			} catch (IOException e) {
				// e.printStackTrace();
				final StringBuffer newPath = new StringBuffer();
				Display.getDefault().syncExec(new Runnable() {

					@Override
					public void run() {
						Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

						MessageDialog message = new MessageDialog(shell,
								"Select Soar Installation Location",
								null,
								"Select the location of your Soar installation. Select the directory that contains the 'bin' directory.",
								MessageDialog.INFORMATION,
								new String[] { "OK", "Cancel" }, 0);
						
						int result = message.open();
						if (result == 0) {
							DirectoryDialog dialog = new DirectoryDialog(shell, SWT.OPEN);
							dialog.setText("Select Soar Installation Location");
							String resultPath = dialog.open();
							if (resultPath != null) {
								newPath.append(resultPath);
							}
						}
					}
				});
				
				if (newPath.length() > 0) {
					path = newPath.toString();
					replacedPath = true;
				} else {
					doit = false;
				}
			}
		}
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {

	}

}
