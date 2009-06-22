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
package com.soartech.soar.ide.ui.views.outline;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarFileAgentProxy;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.editors.text.SoarFoldingSupport;

/**
 * <code>SoarOutlineContentProvider</code> provide the content for the
 * <code>SoarOutlinePage</code>.
 *
 * @author annmarie.steichmann@soartech.com
 * @version $Revision: 578 $ $Date: 2009-06-22 13:05:30 -0400 (Mon, 22 Jun 2009) $
 */
public class SoarOutlineContentProvider implements ITreeContentProvider {
    
    private SoarFoldingSupport foldingSupport;
    private Object parent = null;

    /**
     * @param foldingSupport
     */
    public SoarOutlineContentProvider(SoarFoldingSupport foldingSupport)
    {
        this.foldingSupport = foldingSupport;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    public Object[] getChildren( Object parentElement ) 
    {
        try
        {
            if(parentElement instanceof ISoarFile)
            {
                List<Object> children = new ArrayList<Object>(foldingSupport.getRegions());
                
                ISoarFile soarFile = (ISoarFile) parentElement;
                if(soarFile.hasChildren())
                {
                    ISoarFileAgentProxy proxy = (ISoarFileAgentProxy) soarFile.getChildren().get(0);
                    
                    children.addAll(proxy.getChildren());
                    
                    return children.toArray();
                }
            }
            else if (parentElement instanceof ISoarElement) 
            {
                return ((ISoarElement) parentElement).getChildren().toArray();
    		}
        }
        catch (SoarModelException e)
        {
            SoarEditorUIPlugin.log(e.getStatus());
        }
    	
        return new Object[0];
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
     */
    public Object getParent( Object element ) 
    {
        return element == parent ? null : parent;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     */
    public boolean hasChildren( Object element ) {
        
        try
        {
            if(element instanceof ISoarFile)
            {
                if(!foldingSupport.getRegions().isEmpty())
                {
                    return true;
                }
                ISoarFile soarFile = (ISoarFile) element;
                if(soarFile.hasChildren())
                {
                    return soarFile.getChildren().get(0).hasChildren();
                }
            }
        }   
        catch (SoarModelException e)
        {
            SoarEditorUIPlugin.log(e.getStatus());
        }

        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    public Object[] getElements( Object inputElement ) {
        
        return getChildren( parent );
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {}

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    public void inputChanged( Viewer viewer, Object oldInput, Object newInput ) {
        
        parent = newInput;
    }
}