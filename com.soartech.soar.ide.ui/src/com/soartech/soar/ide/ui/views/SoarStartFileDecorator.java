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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.widgets.Display;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarModelListener;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.SoarModelEvent;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.ui.SoarEditorPluginImages;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;

/**
 * @author ray
 */
public class SoarStartFileDecorator extends LabelProvider implements ILightweightLabelDecorator
{
    private ModelListener listener = new ModelListener();
    
    public SoarStartFileDecorator()
    {
        SoarCorePlugin.getDefault().getSoarModel().addListener(listener);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
     */
    public void decorate(Object element, IDecoration decoration)
    {
        if(element instanceof ISoarFile)
        {
            ISoarFile f = (ISoarFile) element;
            ISoarProject project = f.getSoarProject();
            try
            {
                for(ISoarAgent agent : project.getAgents())
                {
                    if(f.getFile().equals(agent.getStartFile()))
                    {
                        decoration.addOverlay(SoarEditorPluginImages.getDescriptor(SoarEditorPluginImages.IMG_START_FILE_OVERLAY), 
                                IDecoration.TOP_RIGHT);
                        break;
                    }
                }
            }
            catch (SoarModelException e)
            {
                SoarEditorUIPlugin.log(e);
            }
        }

    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
     */
    public void dispose()
    {
        SoarCorePlugin.getDefault().getSoarModel().removeListener(listener);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
     */
    public boolean isLabelProperty(Object element, String property)
    {
        return false;
    }

    private class ModelListener implements ISoarModelListener
    {

        /* (non-Javadoc)
         * @see com.soartech.soar.ide.core.model.ISoarModelListener#onEvent(com.soartech.soar.ide.core.model.SoarModelEvent)
         */
        public void onEvent(SoarModelEvent event)
        {
            // A changed event is fired when a file becomes the startup file.
            // In order to force views displaying soar files to update, namely
            // the Java package explorer, we have to send a label provider
            // changed event with the affected corresponding resources...
            if(event.getType() != SoarModelEvent.ELEMENTS_CHANGED)
            {
                return;
            }
            
            List<IResource> resources = new ArrayList<IResource>();
            for(ISoarElement e : event.getElements())
            {
                IResource r = e.getCorrespondingResource();
                if(r != null && r instanceof IFile)
                {
                    resources.add(r);
                }
            }
            
            if(resources.isEmpty())
            {
                return;
            }
            
            final LabelProviderChangedEvent e = 
                new LabelProviderChangedEvent(SoarStartFileDecorator.this, 
                                              resources.toArray());
            
            Display.getDefault().asyncExec(new Runnable() {

                public void run()
                {
                    fireLabelProviderChanged(e);
                }});
        }
        
    }
}
