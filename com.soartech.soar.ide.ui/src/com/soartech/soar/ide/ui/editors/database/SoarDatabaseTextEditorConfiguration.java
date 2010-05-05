package com.soartech.soar.ide.ui.editors.database;

import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

public class SoarDatabaseTextEditorConfiguration extends
		SourceViewerConfiguration {

	private SoarDatabaseTextEditor editor;
	
	public SoarDatabaseTextEditorConfiguration(SoarDatabaseTextEditor editor) {
		this.editor = editor;
	}
	
    @Override
    public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
       return new SoarDatabaseTextAnnotationHover(editor);
    }
	
}
