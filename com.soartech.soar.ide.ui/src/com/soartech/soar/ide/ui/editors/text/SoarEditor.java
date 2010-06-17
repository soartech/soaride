/*
 *Copyright (c) 2009, Soar Technology, Inc.
 *All rights reserved.
 *
 *Redistribution and use in source and binary forms, with or without modification,   *are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *  * Neither the name of Soar Technology, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 *THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY  *EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED   *WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.   *IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,   *INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT   *NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR   *PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,    *WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)   *ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE    *POSSIBILITY OF SUCH *DAMAGE. 
 *
 * 
 */
package com.soartech.soar.ide.ui.editors.text;

import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.PaintManager;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.MatchingCharacterPainter;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarFileAgentProxy;
import com.soartech.soar.ide.core.model.ISoarModelListener;
import com.soartech.soar.ide.core.model.ISoarProblemReporter;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.SoarModelEvent;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarModelTools;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.editors.text.actions.ToggleCommentAction;
import com.soartech.soar.ide.ui.views.outline.SoarOutlinePage;

/**
 * <code>SoarEditor</code> displays Soar code with keywords
 * highlighted.
 *
 * @author annmarie.steichmann@soartech.com
 * @version $Revision: 578 $ $Date: 2009-06-22 13:05:30 -0400 (Mon, 22 Jun 2009) $
 */
public class SoarEditor extends TextEditor implements IAnnotated
{
    private final SoarEditorDocumentProvider documentProvider = SoarEditorDocumentProvider.getInstance();
    private final SoarSourceEditorConfiguration configuration = new SoarSourceEditorConfiguration(this);
    private final ISoarProblemReporter problemReporter = new SoarEditorProblemReporter(this);
    private final IAnnotationModel defaultAnnotationModel = new AnnotationModel();
    
    private boolean disposed = false;
    private SoarEditorStatusBar statusBar;
    private SoarOutlinePage outline = null;
    private Object workingCopyLock = new Object();
    private ModelListener modelListener = new ModelListener();
    
    private SoarFoldingSupport foldingSupport;

    /**
     * Used in the character matching highlighting.
     */
    protected PaintManager paintManager;
    
    /**
     * Used in the character matching highlighting.
     */
    protected MatchingCharacterPainter bracketPainter;
    
    
    /**
     * Constructor for a <code>SoarEditor</code> object.
     */
    public SoarEditor() 
    {
        super();
        setSourceViewerConfiguration(configuration);
        setDocumentProvider(documentProvider);
        SoarCorePlugin.getDefault().getSoarModel().addListener(modelListener);
    }
    
    /**
     * @return The Soar working copy this file is working on, or 
     *      <code>null</code> if Soar support is not enabled for the project
     */
    public ISoarFile getSoarFileWorkingCopy()
    {
        IFileEditorInput editorInput = (IFileEditorInput) getEditorInput();
        
        ISoarFile soarFile = documentProvider.getSoarFileWorkingCopy(editorInput);
        if(soarFile != null)
        {
            return soarFile;
        }
        
        IFile file = editorInput.getFile();
        try
        {
            // If a Soar file exists for this file, then a first agent was probably just added
            // to the project. Reconnect the document provider and try to get the working
            // copy again.
            if(file != null && SoarEditorDocumentProvider.findSoarFile(file) != null)
            {
                documentProvider.disconnect(editorInput);
                documentProvider.connect(editorInput);
                return documentProvider.getSoarFileWorkingCopy(editorInput);
            }
        }
        catch (CoreException e)
        {
            SoarEditorUIPlugin.log(e);
        }
        return null;
    }
    
    public Object getWorkingCopyLock()
    {
        ISoarFile workingCopy = getSoarFileWorkingCopy();
        return workingCopy != null ? workingCopy.getLock() : workingCopyLock;
    }
    
    /**
     * @return The document this editor is working on
     */
    public IDocument getDocument()
    {
        return documentProvider.getDocument(getEditorInput());
    }
    
    /**
     * @return the problemReporter
     */
    @Override
    public ISoarProblemReporter getProblemReporter()
    {
        return problemReporter;
    }
    
    /**
     * @return the foldingSupport
     */
    public SoarFoldingSupport getFoldingSupport()
    {
        return foldingSupport;
    }

    @Override
    public IAnnotationModel getAnnotationModel()
    {
        ISourceViewer viewer = getSourceViewer();
        if (disposed || viewer != null) { 
           IAnnotationModel ret = viewer.getAnnotationModel();
           return ret;
        }
        return defaultAnnotationModel;
    }

    public boolean isDisposed()
    {
        return disposed;
    }
    
    /**
     * @return the configuration
     */
    public SoarSourceEditorConfiguration getConfiguration()
    {
        return configuration;
    }

    /**
     * Called by {@link SoarReconcilingStrategy} when it has finished 
     * reconciling the working copy.
     */
    void workingCopyReconciled()
    {
        Display.getDefault().syncExec(new Runnable() {

            public void run()
            {
                if(disposed)
                {
                    return;
                }
                
                synchronized(getWorkingCopyLock())
                {
                    foldingSupport.updateFoldingStructure();
                    
                    if(outline != null)
                    {
                        outline.workingCopyReconciled();
                    }
                   
                    // Force another selection event so that selection listeners
                    // get a chance to update after the reconciler has rebuilt
                    // the model.
                    getSelectionProvider().setSelection(getSelectionProvider().getSelection());
                }
            }});
    }
    

    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.StatusTextEditor#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent)
    {
        // Create a new composite that will hold our status bar and the main 
        // editor component.
        Composite newComposite = new Composite(parent, SWT.NONE);
        
        GridLayout grid = new GridLayout();
        grid.horizontalSpacing = 0;
        grid.marginWidth = 0;
        grid.marginHeight = 0;
        newComposite.setLayout(grid);
        
        statusBar = new SoarEditorStatusBar(this, newComposite, VERTICAL_RULER_WIDTH);
        
        // Wrap the editor in another composite to make sure that it expands to
        // fill all of the available space. Ugg.
        Composite editorComposite = new Composite(newComposite, SWT.NONE);
        GridData gd = new GridData(GridData.FILL, GridData.FILL, true, true);
        editorComposite.setLayoutData(gd);
        
        FillLayout fillLayout = new FillLayout();
        editorComposite.setLayout(fillLayout);
        
        super.createPartControl(editorComposite);
        
        foldingSupport = new SoarFoldingSupport(this, (ProjectionViewer) getSourceViewer(), getAnnotationAccess(), getSharedColors());
        
        statusBar.update();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.texteditor.AbstractTextEditor#createSourceViewer(org.eclipse.swt.widgets.Composite,
     *      org.eclipse.jface.text.source.IVerticalRuler, int)
     */
    @Override
    protected ISourceViewer createSourceViewer(Composite parent,
            IVerticalRuler ruler, int styles)
    {
        ISourceViewer viewer = new ProjectionViewer(parent, ruler,
                getOverviewRuler(), isOverviewRulerVisible(), styles);

        // ensure decoration support has been created and configured.
        getSourceViewerDecorationSupport(viewer);

        return viewer;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.editors.text.TextEditor#dispose()
     */
    @Override
    public void dispose()
    {
        disposed = true;
        SoarCorePlugin.getDefault().getSoarModel().removeListener(modelListener);
        
        if(foldingSupport != null)
        {
            foldingSupport.dispose();
            foldingSupport = null;
        }
        super.dispose();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.editors.text.TextEditor#createActions()
     */
    @Override
    protected void createActions() {

        super.createActions();
        
        getSite().getKeyBindingService().setScopes(new String[]{
                ISoarEditorContextIds.SOAR_EDITOR_CONTEXT });
        
        installContentAssistAction();
        
        IAction action= new TextOperationAction( SoarEditorMessages.getBundleForConstructedKeys(), "Comment.", this, ITextOperationTarget.PREFIX); //$NON-NLS-1$
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
        configureToggleCommentAction();
        
//        action = new FormatSoarDocumentAction( SoarEditorMessages.getBundleForConstructedKeys(), "FormatSoarDocument.", this );
//        action.setActionDefinitionId( ISoarEditorActionDefinitionIds.FORMAT_SOAR_DOC );
//        setAction( "FormatSoarDocument", action) ; //$NON-NLS-1$
//        markAsStateDependentAction( "FormatSoarDocument", true ); //$NON-NLS-1$
//        configureFormatSoarDocumentAction();
        
        ISourceViewer sourceViewer = getSourceViewer();
        if(sourceViewer != null)
        {
            paintManager = new PaintManager(sourceViewer);
            startBracketHighlighting();
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.editors.text.TextEditor#doSetInput(org.eclipse.ui.IEditorInput)
     */
    @Override
    protected void doSetInput(IEditorInput input) throws CoreException
    {
        super.doSetInput(input);

        configureToggleCommentAction();
        if(statusBar != null)
        {
            statusBar.update();
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.AbstractTextEditor#handleCursorPositionChanged()
     */
    @Override
    protected void handleCursorPositionChanged()
    {
        super.handleCursorPositionChanged();
        
        Display.getDefault().asyncExec(new Runnable() {

            public void run()
            {
                // Sync the outline view with the editor.
                ISoarElement e = getElementAtCaretOffset();
                if(e != null && outline != null)
                {
                    outline.setElementSelection(e);
                }
            }});
    }
    
    /**
     * @return The element currently under the caret, or <code>null</code>
     *      for none.
     */
    public ISoarElement getElementAtCaretOffset()
    {
        synchronized (getWorkingCopyLock())
        {
            ISourceViewer sourceViewer = getSourceViewer();
            if(sourceViewer == null)
            {
                return null;
            }
            
            StyledText styledText = sourceViewer.getTextWidget();
            if(styledText == null || styledText.isDisposed())
            {
                return null;
            }
            
            // Use extension interface to map caret offset to model offset.
            ITextViewerExtension5 ext5 = (ITextViewerExtension5) sourceViewer;
            int caretOffset = ext5.widgetOffset2ModelOffset(styledText.getCaretOffset());
            
            ISoarFile wc = getSoarFileWorkingCopy();
            if(wc == null)
            {
                return null;
            }
            
            try
            {
                ISoarFileAgentProxy agentProxy = wc.getPrimaryAgentProxy();
                return agentProxy != null ? SoarModelTools.getChildAtOffset(agentProxy, caretOffset) : null;
            }
            catch (SoarModelException e)
            {
                return null;
            }
        }
    }

    /**
     * @return The SINGLE element existing within the bounds of this offset, or <code>null</code>
     *      for none.
     */
    public ISoarProduction getProductionWithinOffsets(int start, int end)
    {
        synchronized (getWorkingCopyLock())
        {         
            ISoarFile wc = getSoarFileWorkingCopy();
            if(wc == null)
            {
                return null;
            }
            
            try
            {
                ISoarFileAgentProxy agentProxy = wc.getPrimaryAgentProxy();
                if(agentProxy!=null)
                {
                	//get every production in the file (almost certain faster than checking every offset in the range)
                	List<ISoarProduction> list = agentProxy.getProductions();
            		ISoarProduction element=null;
                	for(ISoarProduction prod : list)
                	{
                		//if any part of the production is in range
                		if((prod.getSourceRange().getOffset() >= start && prod.getSourceRange().getOffset() <= end) ||
                				(prod.getSourceRange().getEnd() >= start && prod.getSourceRange().getEnd() <=end))
                		{
                			// keep it, if we havn't already found one
                			if(element==null)element=prod;
                			else return null; // or return null if this is the second production in this range
                		}
                	}
            		//if there was exactly one production in range
            		if(element!=null)
            		{
            			//make sure it is all within the range
            			if(element.getSourceRange().getOffset()<start || element.getSourceRange().getEnd()>end)
            				element=null;
            			
            		}
                	return element;
                }
                else return null;
            }
            catch (SoarModelException e)
            {
                return null;
            }
        }
    }

    private void configureToggleCommentAction() {
      IAction action = getAction( "ToggleComment" ); //$NON-NLS-1$
      if( action instanceof ToggleCommentAction ) {
         ISourceViewer sourceViewer = getSourceViewer();
         SourceViewerConfiguration configuration = getSourceViewerConfiguration();
         ( (ToggleCommentAction) action ).configure( sourceViewer,
                                                     configuration );
      }
   }
    
//    private void configureFormatSoarDocumentAction() {
//        IAction action = getAction( "FormatSoarDocument" ); //$NON-NLS-1$
//        if( action instanceof FormatSoarDocumentAction ) 
//        {
//           ISourceViewer sourceViewer = getSourceViewer();
//           SourceViewerConfiguration configuration = getSourceViewerConfiguration();
//           ((FormatSoarDocumentAction) action).configure(sourceViewer, configuration);
//        }
//     }

   /**
     * This method is required because the default text editor does
     * not have the Ctrl + Space support for content assist enabled and
     * I don't know of any other way to enable it.
     */
    private void installContentAssistAction() {
        
        final String actionName = "ContentAssistProposal";

        Action action = new ContentAssistAction(
                SoarEditorUIPlugin.getDefault().getResourceBundle(),
                actionName, this );
        String id = 
            ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS;
        action.setActionDefinitionId( id );
        action.setEnabled( true );
        setAction( actionName, action );
        markAsStateDependentAction( actionName, true );
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.editors.text.TextEditor#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter( Class required ) {
        
        if ( required.equals(IContentOutlinePage.class)) 
        {
            return outline = new SoarOutlinePage( this );
        }
        return super.getAdapter( required );
    }
     
    /* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#handlePreferenceStoreChanged(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	@Override
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		
		super.handlePreferenceStoreChanged(event);
		
		SoarSourceEditorConfiguration configuration = (SoarSourceEditorConfiguration)getSourceViewerConfiguration();
        if(configuration != null)
        {
            configuration.reinitializeScanner();
        }
        if(getSourceViewer() != null)
        {
            getSourceViewer().invalidateTextPresentation();
        }
	}
	
    private void updateOnModelChanged()
    {
        if(disposed)
        {
            return;
        }
        
        configuration.forceReconcile();
        statusBar.update();
    }
    
    private void safeUpdateOnModelChanged()
    {
        Display.getDefault().asyncExec(new Runnable() {

            public void run()
            {
                updateOnModelChanged();
            }});
    }
    
    private void startBracketHighlighting() 
    {
        if (bracketPainter == null) 
        {
            ISourceViewer sourceViewer = getSourceViewer();
            bracketPainter = new MatchingCharacterPainter(sourceViewer, new SoarPairMatcher());
            bracketPainter.setColor(new Color(Display.getCurrent(), new RGB(200, 200, 200)));
            paintManager.addPainter(bracketPainter);
        }
    }
    
    private class ModelListener implements ISoarModelListener
    {
        /* (non-Javadoc)
         * @see com.soartech.soar.ide.core.model.ISoarModelListener#onEvent(com.soartech.soar.ide.core.model.SoarModelEvent)
         */
        public void onEvent(SoarModelEvent event)
        {
            for(ISoarElement e : event.getElements())
            {
                if(e instanceof ISoarAgent || e instanceof ISoarProject)
                {
                    safeUpdateOnModelChanged();
                }
            }
        }
        
    }
}