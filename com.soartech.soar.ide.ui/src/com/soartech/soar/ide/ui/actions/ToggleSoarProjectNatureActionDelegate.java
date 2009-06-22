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
package com.soartech.soar.ide.ui.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.soartech.soar.ide.core.SoarProjectNature;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;

/**
 * Object action for IProjects that toggles the Soar project nature.
 * 
 * @author ray
 */
public class ToggleSoarProjectNatureActionDelegate implements
        IObjectActionDelegate
{
    private ISelection selection;
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
     */
    public void setActivePart(IAction action, IWorkbenchPart targetPart)
    {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action)
    {       
        IProject project = getProject(selection);
        if(project == null)
        {
            return;
        }
        
        try
        {
            if(project.hasNature(SoarProjectNature.NATURE_ID))
            {
                SoarProjectNature.removeFromProject(project);
            }
            else
            {
                SoarProjectNature.addToProject(project);
            }
        }
        catch(CoreException e)
        {
            SoarEditorUIPlugin.log(e.getStatus());
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection)
    {
        this.selection = selection;
        
        IProject project = getProject(selection);
        
        action.setEnabled(project != null);
        if(project != null)
        {
            try
            {
                action.setChecked(project.hasNature(SoarProjectNature.NATURE_ID));
            }
            catch (CoreException e)
            {
                SoarEditorUIPlugin.log(e.getStatus());
            }
        }
    }

    /**
     * Retrieve a project from the selection
     * 
     * @param selection The selection
     * @return The project, or null if not available
     */
    private IProject getProject(ISelection selection)
    {
        if(selection.isEmpty())
        {
            return null;
        }
        if(!(selection instanceof IStructuredSelection))
        {
            return null;
        }
        IStructuredSelection ss = (IStructuredSelection) selection;
        
        Object element = ss.getFirstElement();
        if(!(element instanceof IProject))
        {
            return null;
        }
        IProject project = (IProject) element;
        if(!project.exists() || !project.isOpen())
        {
            return null;
        }
        return project;
    }
}
