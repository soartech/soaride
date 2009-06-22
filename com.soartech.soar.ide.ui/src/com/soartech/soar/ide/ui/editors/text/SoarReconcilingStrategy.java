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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;

import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;

/**
 * <code>SoarReconcilingStrategy</code> is responsible for reconciling the 
 * {@link ISoarFile} working copy when changes are made to the editor. The
 * working copy is responsible for firing event changes to the model.
 * 
 * @author annmarie.steichmann
 * @author ray
 */
public class SoarReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension
{
	private SoarEditor editor = null;
	private IDocument document = null;
	
	/**
	 * Constructor for <code>SoarReconcilingStrategy</code>
	 * @param editor the associated <code>SoarEditor</code>
	 */
	public SoarReconcilingStrategy(SoarEditor editor) 
    {
		this.editor = editor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.IRegion)
	 */
	public void reconcile(IRegion partition) 
    {
		if ( editor == null ) return;
		
        ISoarFile workingCopy = editor.getSoarFileWorkingCopy();
        if(workingCopy == null)
        {
            return;
        }
        
        try
        {
            workingCopy.makeConsistent(new NullProgressMonitor() /*, 
                                       editor.getProblemReporter()*/);
        }
        catch (SoarModelException e)
        {
            SoarEditorUIPlugin.log(e.getStatus());
        }
        
        editor.workingCopyReconciled();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.reconciler.DirtyRegion, org.eclipse.jface.text.IRegion)
	 */
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) 
    {
        throw new UnsupportedOperationException("Reconciling strategy does not support incremental reconcile operations");
    }

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org.eclipse.jface.text.IDocument)
	 */
	public void setDocument(IDocument document) 
    {
		this.document = document;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#initialReconcile()
	 */
	public void initialReconcile()
    {
        // This is called after the document is set when the reconciler thread
        // is started. This gives us a chnace to build the initial working
        // copy.
        reconcile(null);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void setProgressMonitor(IProgressMonitor monitor)
    {
    }

    /**
	 * @return The current document
	 */
	public IDocument getDocument() {
		
		return document;
	}
}
