package com.soartech.soar.ide.ui.actions;

import java.io.File;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.part.Page;

import com.soartech.soar.ide.core.SoarCorePlugin;

public class OpenSoarDatabaseProjectActionDelegate implements IWorkbenchWindowActionDelegate {

	Shell shell;
	IWorkbenchWindow window;
	
	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow window) {
		shell = window.getShell();
		this.window = window;
	}

	@Override
	public void run(IAction action) {

		FileDialog dialog = new FileDialog(shell, SWT.OPEN);
		dialog.setText("Open Soar Project...");
		dialog.open();
		String path = dialog.getFileName();
		
		if (path != null && path.length() > 0) {
			IWorkbenchPage[] pages = window.getPages();
			for (final IWorkbenchPage page : pages) {
				System.out.println("PAGE: " + page);
				IEditorReference[] editorRefs = page.getEditorReferences();
				for (IEditorReference ref : editorRefs) {
					System.out.println(ref.getName() + " " + ref.getTitle());
					IEditorPart editor = ref.getEditor(false);
					if (editor != null) {
						boolean save = false;
						page.closeEditor(editor, save);
					}
				}
			}
			
			SoarCorePlugin.getDefault().openProject(path);
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

}
