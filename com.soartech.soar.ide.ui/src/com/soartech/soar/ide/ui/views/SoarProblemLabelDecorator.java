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
package com.soartech.soar.ide.ui.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.ui.SoarEditorPluginImages;
import com.soartech.soar.ide.ui.views.explorer.SoarExplorerFullViewContentProvider.SoarFolderHeader;

/**
 * Label decorator for reporting Soar problems. This is registered in 
 * plugin.xml with the org.eclipse.ui.decorators extension.
 * 
 * @author ray
 */
public class SoarProblemLabelDecorator implements ILightweightLabelDecorator
{
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
     */
    public void decorate(Object element, IDecoration decoration)
    {        
        if(element instanceof ISoarElement)
        {
            ISoarElement se = (ISoarElement) element;
            ImageDescriptor overlay = null;
            if(se.hasErrors())
            {
                overlay = SoarEditorPluginImages.getDescriptor(SoarEditorPluginImages.IMG_ERROR_OVERLAY);
            }
            else if(se.hasWarnings())
            {
                overlay = SoarEditorPluginImages.getDescriptor(SoarEditorPluginImages.IMG_WARNING_OVERLAY);
            }
            
            if(overlay != null)
            {
                decoration.addOverlay(overlay, IDecoration.BOTTOM_LEFT);
            }
        }
        else if(element instanceof SoarFolderHeader)
        {
            ImageDescriptor overlay = null;
            boolean warnings = false;
            for(ISoarElement e : ((SoarFolderHeader) element).getChildren())
            {
                if(e.hasErrors())
                {
                    overlay = SoarEditorPluginImages.getDescriptor(SoarEditorPluginImages.IMG_ERROR_OVERLAY);
                }
                else if(e.hasWarnings())
                {
                    warnings = true;
                }
            }
            if(overlay == null && warnings)
            {
                overlay = SoarEditorPluginImages.getDescriptor(SoarEditorPluginImages.IMG_WARNING_OVERLAY);
            }
            
            if(overlay != null)
            {
                decoration.addOverlay(overlay, IDecoration.BOTTOM_LEFT);
            }
        }
    }

    public void addListener(ILabelProviderListener listener)
    {
    }

    public void dispose()
    {
    }

    public boolean isLabelProperty(Object element, String property)
    {
        return false;
    }

    public void removeListener(ILabelProviderListener listener)
    {
    }
    
    
}
