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
				SoarCommandLineClient client = new SoarCommandLineClient(path);
				SoarCommandLineMXBean proxy;
				proxy = client.startDebuggerGetProxy();
				if (proxy != null) {
					SoarCorePlugin.getDefault().setCommandLineProxy(proxy);
				}
				if (replacedPath) {
					EditorsUI.getPreferenceStore().setValue(SoarEditorPreferencePage.SOAR_PATH, path);
				}
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
								new String[] { "OK" }, 0);
						
						message.open();
						
						DirectoryDialog dialog = new DirectoryDialog(shell, SWT.OPEN);
						dialog.setText("Select Soar Installation Location");
						
						/*
						MessageDialog dialog = new MessageDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Unable to Spawn Debugger", null,
								"Check that the path to your Soar installation is set correctly under" + " \"Window > Preferences ... Soar Editor ... Path to Soar Installation\"."
										+ "\n\nThe path you specify should contain the bin folder.", MessageDialog.ERROR, new String[] { "Ok" }, 0);
										*/
						newPath.append(dialog.open());
					}
				});
				
				if (newPath.length() > 0) {
					path = newPath.toString();
					replacedPath = true;
				} else {
					doit = false;
				}
			} finally {
				doit = false;
			}
		}
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {

	}

}
