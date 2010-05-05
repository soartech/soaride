package com.soartech.soar.ide.ui.editors.database;

import java.awt.Paint;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.PaintManager;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.AnnotationPainter;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.MatchingCharacterPainter;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.DocumentProviderRegistry;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;

import com.soartech.soar.ide.core.model.ISoarProblemReporter;
import com.soartech.soar.ide.core.model.SoarProblem;
import com.soartech.soar.ide.core.sql.SoarDatabaseEditorInput;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.ui.editors.text.IAnnotated;
import com.soartech.soar.ide.ui.editors.text.SoarEditorProblemReporter;
import com.soartech.soar.ide.ui.editors.text.SoarPairMatcher;
import com.soartech.soar.ide.ui.editors.text.SoarSourceEditorConfiguration;
import com.soartech.soar.ide.ui.editors.text.SoarSourceViewerConfiguration;
import com.soartech.soar.ide.ui.editors.text.autoedit.SoarAutoEditStrategy;

public class SoarDatabaseTextEditor extends TextEditor {

	public static final String ID = "com.soartech.soar.ide.ui.editors.database.SoarDatabaseTextEditor";
	
	private SoarDatabaseEditorInput input;

	public SoarDatabaseTextEditor() {
		super();
		SourceViewerConfiguration configuration = new SoarDatabaseTextEditorConfiguration(this);
		setSourceViewerConfiguration(configuration);
		//ISourceViewer sourceViewer = getSourceViewer();
		//sourceViewer.showAnnotations(true);
	}
	
	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {		
		//super.setDocumentProvider(input);
		super.doSetInput(input);
		if (input instanceof SoarDatabaseEditorInput) {
			this.input = (SoarDatabaseEditorInput) input;
		}
	}
	
	public SoarDatabaseEditorInput getInput() {
		return input;
	}
	
	/*
	@Override
	protected void setDocumentProvider(IEditorInput input) {
		if (input instanceof SoarDatabaseEditorInput) {
			IDocumentProvider provider = new SoarDatabaseTextDocumentProvider((SoarDatabaseEditorInput) input);
			super.setDocumentProvider(provider);
		}
	}
	*/
	
	
	@Override
	public void doSave(IProgressMonitor progressMonitor) {
		super.doSave(progressMonitor);
		if (input != null) {
			input.clearProblems();
			clearAnnotations();
			SoarDatabaseRow row = input.getSoarDatabaseStorage().getRow();
			IDocument doc = getDocumentProvider().getDocument(input);
			row.save(doc, input);
			ArrayList<SoarProblem> problems = input.getProblems();
			for (SoarProblem problem : problems) {
				SoarDatabaseTextAnnotation annotation = new SoarDatabaseTextAnnotation();
				Position position = new Position(problem.start, problem.length);
				addAnnotation(annotation, position);
			}
			getVerticalRuler().update();
		}
	}
	
	public void clearAnnotations() {
		IAnnotationModel model = getVerticalRulerModel();
		Iterator<?> it = model.getAnnotationIterator();
		ArrayList<Annotation> remove = new ArrayList<Annotation>();
		while (it.hasNext()) {
			Object obj = it.next();
			if (obj instanceof Annotation) {
				remove.add((Annotation) obj);
			}
		}
		for (Annotation annotation : remove) {
			model.removeAnnotation(annotation);
		}
	}
	
	public void addAnnotation(SoarDatabaseTextAnnotation annotation, Position position) {
		IAnnotationModel model = getVerticalRulerModel();
		model.addAnnotation(annotation, position);
	}
	
	private IAnnotationModel getVerticalRulerModel() {
		IVerticalRuler ruler = getVerticalRuler();
		IAnnotationModel model = ruler.getModel();
		if (model == null) {
			model = new AnnotationModel();
			ruler.setModel(model);
		}
		return model;
	}
	
	/*
	@Override
    protected void createActions() {
        super.createActions();
        ISourceViewer sourceViewer = getSourceViewer();
        PaintManager paintManager = new PaintManager(sourceViewer);
        MatchingCharacterPainter bracketPainter = new MatchingCharacterPainter(sourceViewer, new SoarPairMatcher());
        bracketPainter.setColor(new Color(Display.getCurrent(), new RGB(200, 200, 200)));
        paintManager.addPainter(bracketPainter);
	}
	*/
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.texteditor.AbstractTextEditor#createSourceViewer(org.eclipse.swt.widgets.Composite,
     *      org.eclipse.jface.text.source.IVerticalRuler, int)
     */
    /*
	@Override
    protected ISourceViewer createSourceViewer(Composite parent,
            IVerticalRuler ruler, int styles)
    {
		ISourceViewer sourceViewer= new SourceViewer(parent, ruler, fOverviewRuler, isOverviewRulerVisible(), styles);
		fSourceViewerDecorationSupport = new SourceViewerDecorationSupport(sourceViewer, fOverviewRuler, fAnnotationAccess, getSharedColors());
		configureSourceViewerDecorationSupport(fSourceViewerDecorationSupport);
		return sourceViewer;
    }
	*/
    /*
    @Override
    public IAnnotationModel getAnnotationModel()
    {

    	ISourceViewer viewer = getSourceViewer();
        if (viewer != null) {
        	IAnnotationModel ret = viewer.getAnnotationModel();
        	return ret;
        }
        return  defaultAnnotationModel;
        
    }
    */
	
	/*
    /**
     * @return the problemReporter
     
    @Override
    public ISoarProblemReporter getProblemReporter()
    {
        return problemReporter;
    }
    */
}
