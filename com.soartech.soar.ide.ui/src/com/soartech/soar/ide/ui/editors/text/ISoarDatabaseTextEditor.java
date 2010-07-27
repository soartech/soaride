package com.soartech.soar.ide.ui.editors.text;

import org.eclipse.ui.texteditor.ITextEditor;

import com.soartech.soar.ide.core.sql.SoarDatabaseEditorInput;

public interface ISoarDatabaseTextEditor extends ITextEditor {
	SoarDatabaseEditorInput getInput();
}
