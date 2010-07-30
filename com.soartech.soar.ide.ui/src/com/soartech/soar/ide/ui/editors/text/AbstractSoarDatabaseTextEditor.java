package com.soartech.soar.ide.ui.editors.text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.PaintManager;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.MatchingCharacterPainter;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import com.soartech.soar.ide.core.sql.SoarDatabaseEditorInput;

public class AbstractSoarDatabaseTextEditor extends TextEditor implements ISoarDatabaseTextEditor {

	SoarDatabaseEditorInput input;
	SoarDatabaseTextEditorConfiguration configuration;
	
	public AbstractSoarDatabaseTextEditor() {
		super();
		configuration = new SoarDatabaseTextEditorConfiguration(this);
		setSourceViewerConfiguration(configuration);
	}
	
	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {		
		super.doSetInput(input);
		if (input instanceof SoarDatabaseEditorInput) {
			this.input = (SoarDatabaseEditorInput) input;
			configuration.setRow(this.input.getRow());
		}
	}
	
	@Override
	public SoarDatabaseEditorInput getInput() {
		return input;
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
	
	@Override
    protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles)
    {
        ISourceViewer viewer = new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);
        getSourceViewerDecorationSupport(viewer);
        return viewer;
    }
	
	@Override
    protected void createActions() {
        super.createActions();
        ISourceViewer sourceViewer = getSourceViewer();
        PaintManager paintManager = new PaintManager(sourceViewer);
        MatchingCharacterPainter bracketPainter = new MatchingCharacterPainter(sourceViewer, new SoarPairMatcher());
        bracketPainter.setColor(new Color(Display.getCurrent(), new RGB(200, 200, 200)));
        paintManager.addPainter(bracketPainter);
        
        // content assist action
        final String actionName = "ContentAssistProposal";
        ListResourceBundle bundle = new ListResourceBundle() {
			@Override
			protected Object[][] getContents() {
				return new Object[][] {};
			}
        };
        Action action = new ContentAssistAction(bundle, actionName, this);
        String id = ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS;
        action.setActionDefinitionId(id);
        action.setEnabled(true);
        setAction(actionName, action);
        markAsStateDependentAction(actionName, true);
        
        // comment actions
        /*
        action= new TextOperationAction(SoarEditorMessages.getBundleForConstructedKeys(), "Comment.", this, ITextOperationTarget.PREFIX); //$NON-NLS-1$
        action.setActionDefinitionId( ISoarEditorActionDefinitionIds.COMMENT );
        setAction( "Comment", action ); //$NON-NLS-1$
        markAsStateDependentAction( "Comment", true ); //$NON-NLS-1$
        
        action= new TextOperationAction( SoarEditorMessages.getBundleForConstructedKeys(), "Uncomment.", this, ITextOperationTarget.STRIP_PREFIX ); //$NON-NLS-1$
        action.setActionDefinitionId( ISoarEditorActionDefinitionIds.UNCOMMENT );
        setAction( "Uncomment", action ); //$NON-NLS-1$
        markAsStateDependentAction( "Uncomment", true ); //$NON-NLS-1$
        
        action = new ToggleCommentAction( SoarEditorMessages.getBundleForConstructedKeys(), "ToggleComment.", this );
        action.setActionDefinitionId( ISoarEditorActionDefinitionIds.TOGGLE_COMMENT );
        setAction( "ToggleComment", action) ; //$NON-NLS-1$
        markAsStateDependentAction( "ToggleComment", true ); //$NON-NLS-1$
        SourceViewerConfiguration configuration = getSourceViewerConfiguration();
        ((ToggleCommentAction) action).configure(sourceViewer, configuration);
        */
	}

}
