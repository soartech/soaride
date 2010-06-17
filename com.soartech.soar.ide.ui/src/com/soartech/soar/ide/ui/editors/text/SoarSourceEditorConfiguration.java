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

import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.ISourceViewer;

import com.soartech.soar.ide.ui.editors.text.autoedit.SoarAutoEditStrategy;
import com.soartech.soar.ide.ui.linking.SoarHyperlinkDetector;

/**
 * <code>SoarSourceEditorConfiguration</code> sets up the <code>SoarRuleScanner</code> and
 * reconciler.
 *
 * @author annmarie.steichmann@soartech.com
 * @version $Revision: 578 $ $Date: 2009-06-22 13:05:30 -0400 (Mon, 22 Jun 2009) $
 */
public class SoarSourceEditorConfiguration extends SoarSourceViewerConfiguration 
{
    private SoarEditor editor;
    private SoarReconcilingStrategy reconcilingStrategy;
    private SoarReconciler reconciler;
    private SoarAutoEditStrategy autoEditStrategy;

    /**
     * Constructor for a <code>SoarSourceEditorConfiguration</code> object.
     * @param model The associated <code>SoarEditor</code>
     */
    public SoarSourceEditorConfiguration( SoarEditor editor ) {

        super();
        
        this.editor = editor;
        
        reconcilingStrategy = new SoarReconcilingStrategy( editor );
        reconciler = new SoarReconciler(reconcilingStrategy);
        autoEditStrategy = new SoarAutoEditStrategy();
    }

    public SoarEditor getEditor()
    {
        return editor;
    }

    /**
     * Force the reconciler to run even if there haven't been any modifications
     * to the text of the editor.
     */
    public void forceReconcile()
    {
        reconciler.forceReconcile();
    }
    
    public void forceSynchronousReconcile()
    {
        reconcilingStrategy.reconcile(null);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getContentAssistant(org.eclipse.jface.text.source.ISourceViewer)
     */
    @Override
    public IContentAssistant getContentAssistant( ISourceViewer sourceViewer ) {
        return new SoarContentAssistant( this );
    }

   /* (non-Javadoc)
    * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getDefaultPrefixes(
    *   org.eclipse.jface.text.source.ISourceViewer, java.lang.String )
    */
   @Override
   public String[] getDefaultPrefixes( ISourceViewer sourceViewer,
                                       String contentType ) {

	   return ( IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) ?
               new String[] {"#"} : null );
   }

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getReconciler(org.eclipse.jface.text.source.ISourceViewer)
	 */
	@Override
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
        return reconciler;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getTextHover(org.eclipse.jface.text.source.ISourceViewer, java.lang.String)
	 */
	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) 
	{
		return new SoarTextHover(this.editor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getHyperlinkDetectors(org.eclipse.jface.text.source.ISourceViewer)
	 */
	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) 
	{
		IHyperlinkDetector[] inheritedDetectors = super.getHyperlinkDetectors(sourceViewer);
		
		if (editor == null)
        {
            return inheritedDetectors;
        }
		
		int length = inheritedDetectors != null ? inheritedDetectors.length : 0;

        IHyperlinkDetector[] detectors = new IHyperlinkDetector[length + 1];

        detectors[0] = new SoarHyperlinkDetector(this.editor);

        for(int i= 0; i < length; i++)
        {
            detectors[i+1] = inheritedDetectors[i];
        }

        return detectors;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getAutoEditStrategies(org.eclipse.jface.text.source.ISourceViewer, java.lang.String)
	 */
	@Override
	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) 
	{
		return new IAutoEditStrategy[] {getSoarAutoEditStrategy()};
	}
	
	/**
	 * Return the SoarAutoEditStrategy.
	 * 
	 * @return
	 */
	private SoarAutoEditStrategy getSoarAutoEditStrategy()
	{
		return autoEditStrategy;
	}
	
}
