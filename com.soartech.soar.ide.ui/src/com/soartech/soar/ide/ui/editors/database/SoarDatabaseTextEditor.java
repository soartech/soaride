package com.soartech.soar.ide.ui.editors.database;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;

import com.soartech.soar.ide.core.sql.SoarDatabaseEditorInput;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;

public class SoarDatabaseTextEditor extends TextEditor {

	public static final String ID = "com.soartech.soar.ide.ui.editors.database.SoarDatabaseTextEditor";
	
	private SoarDatabaseEditorInput input;
	
	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {
		// TODO Auto-generated method stub
		super.doSetInput(input);
		if (input instanceof SoarDatabaseEditorInput) {
			this.input = (SoarDatabaseEditorInput) input;
		}
	}
	
	@Override
	public void doSave(IProgressMonitor progressMonitor) {
		// TODO Auto-generated method stub
		super.doSave(progressMonitor);
		if (input != null) {
			SoarDatabaseRow row = input.getSoarDatabaseStorage().getRow();
			String text = getDocumentProvider().getDocument(input).get();
			row.setText(text);
		}
	}
	
}
