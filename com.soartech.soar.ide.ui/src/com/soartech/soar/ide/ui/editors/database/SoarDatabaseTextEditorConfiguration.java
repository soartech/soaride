package com.soartech.soar.ide.ui.editors.database;

import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.ui.editors.text.SoarRuleScanner;
import com.soartech.soar.ide.ui.editors.text.autoedit.SoarAutoEditStrategy;

public class SoarDatabaseTextEditorConfiguration extends
		TextSourceViewerConfiguration {

	ISoarDatabaseTextEditor editor;
	SoarRuleScanner ruleScanner = new SoarRuleScanner();
	SoarAutoEditStrategy strategy = new SoarAutoEditStrategy();
	SoarDatabaseRow row;
	
	public SoarDatabaseTextEditorConfiguration(ISoarDatabaseTextEditor editor) {
		this.editor = editor;
	}
	
    @Override
    public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
       return new SoarDatabaseTextAnnotationHover(editor);
    }
    
    @Override
    public IPresentationReconciler getPresentationReconciler( ISourceViewer sourceViewer ) {

        PresentationReconciler reconciler = new PresentationReconciler();
        DefaultDamagerRepairer dr = new DefaultDamagerRepairer(ruleScanner);
        reconciler.setDamager( dr, IDocument.DEFAULT_CONTENT_TYPE );
        reconciler.setRepairer( dr, IDocument.DEFAULT_CONTENT_TYPE );
        return reconciler;
    }
    
	@Override
	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) 
	{
		return new IAutoEditStrategy[] {strategy};
	}
	
    @Override
    public IContentAssistant getContentAssistant( ISourceViewer sourceViewer ) {
        return new SoarDatabaseContentAssistant( this );
    }

	public void setRow(SoarDatabaseRow row) {
		this.row = row;
	}
	
	public SoarDatabaseRow getRow() {
		return row;
	}
}
