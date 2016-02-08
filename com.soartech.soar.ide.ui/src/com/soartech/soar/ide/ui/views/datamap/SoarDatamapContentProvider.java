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
package com.soartech.soar.ide.ui.views.datamap;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamap;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapNode;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;

/**
 * @author ray
 */
public class SoarDatamapContentProvider implements ITreeContentProvider
{
    public static final Object[] EMPTY_ARRAY = new Object[0];

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    public Object[] getChildren(Object parentElement)
    {
        if(parentElement instanceof ISoarModel)
        {
            try
            {
                List<ISoarAgent> agents = new ArrayList<ISoarAgent>();
                for(ISoarProject project : ((ISoarModel) parentElement).getProjects())
                {
                    agents.addAll(project.getAgents());
                }
                return agents.toArray();
                
                // This line will show the projects at the top level
                //return ((ISoarModel) parentElement).getProjects().toArray();
            }
            catch (SoarModelException e)
            {
                SoarEditorUIPlugin.log(e.getStatus());
            }
        }
        else if(parentElement instanceof ISoarProject)
        {
            ISoarProject project = (ISoarProject) parentElement;
            try
            {
                return project.getAgents().toArray();
            }
            catch (SoarModelException e)
            {
                SoarEditorUIPlugin.log(e);
                return new Object[0];
            }
        }
        else if(parentElement instanceof ISoarAgent)
        {
            ISoarAgent agent = (ISoarAgent) parentElement;
            
            ISoarDatamap datamap = agent.getDatamap();
            
            return datamap.getState().getAttributes().toArray();
        }
        else if(parentElement instanceof ISoarDatamap)
        {
            ISoarDatamap datamap = (ISoarDatamap) parentElement;
            
            return datamap.getState().getAttributes().toArray();
        }
        else if(parentElement instanceof ISoarDatamapAttribute)
        {
            ISoarDatamapAttribute a = (ISoarDatamapAttribute) parentElement;
            ISoarDatamapNode target = a.getTarget();
            if(target != null)
            {
                return target.getAttributes().toArray();
            }
            else
            {
                return new Object[0];
            }
        }
        return new Object[0];
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
     */
    public Object getParent(Object element)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     */
    public boolean hasChildren(Object element)
    {
        if(element instanceof ISoarModel)
        {
            try
            {
                return !((ISoarModel) element).getProjects().isEmpty();
            }
            catch (SoarModelException e)
            {
                SoarEditorUIPlugin.log(e);
            }
        }
        else if(element instanceof ISoarProject)
        {
            ISoarProject project = (ISoarProject) element;
            
            try
            {
                return !project.getAgents().isEmpty();
            }
            catch (SoarModelException e)
            {
                SoarEditorUIPlugin.log(e);
            }
        }
        else if(element instanceof ISoarAgent)
        {
            return true;
        }
        else if(element instanceof ISoarDatamapAttribute)
        {
            ISoarDatamapAttribute a = (ISoarDatamapAttribute) element;
            ISoarDatamapNode target = a.getTarget();
            if(target != null)
            {
                return target.hasAttributes();
            }
            else
            {
                return false;
            }
        }        
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    public Object[] getElements(Object inputElement)
    {
        return getChildren(inputElement);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose()
    {
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
    }

}
