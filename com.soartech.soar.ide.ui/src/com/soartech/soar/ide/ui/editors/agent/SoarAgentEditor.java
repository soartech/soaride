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
package com.soartech.soar.ide.ui.editors.agent;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.IDE;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;

/**
 * @author ray
 */
public class SoarAgentEditor extends FormEditor
{
    private ISoarAgent agent;
    private boolean dirty = false;
//    private SoarAgentOverviewPage overviewPage;
    private ProjectListener projectListener = new ProjectListener();
    
    public SoarAgentEditor()
    {
        // Make sure to close the editor when the parent project is closed or
        // deleted. This functionality is only built-in to Eclipse text editors.
        ResourcesPlugin.getWorkspace().addResourceChangeListener(projectListener, 
                IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE |
                IResourceChangeEvent.POST_CHANGE);
    }

    public String getAgentName()
    {
        return agent != null ? agent.getName() : "?";
    }
    
    public ISoarAgent getAgent()
    {
        return agent;
    }
    
    void revert()
    {
        if(agent == null || !isDirty())
        {
            return;
        }
        
        // Close and reopen the editor to update
        IFile file = agent.getFile();
        IWorkbenchPage page = getSite().getPage();
        page.closeEditor(this, false);
        try
        {
            IDE.openEditor(page, file);
        }
        catch (PartInitException e)
        {
            SoarEditorUIPlugin.log(e);
        }
        
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
     */
    @Override
    protected void setInput(IEditorInput input)
    {
        super.setInput(input);
        
        IFileEditorInput fileInput = (IFileEditorInput) input;

        try
        {
            ISoarAgent primaryAgent = SoarCorePlugin.getDefault().getSoarModel().getAgent(fileInput.getFile());
            if(primaryAgent != null)
            {
                agent = primaryAgent.getWorkingCopy();
            }
        }
        catch (SoarModelException e)
        {
            SoarEditorUIPlugin.log(e);
        }
        
        updateTitle();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.forms.editor.FormEditor#dispose()
     */
    @Override
    public void dispose()
    {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(projectListener);
        if(agent != null)
        {
            agent.discardWorkingCopy();
            agent = null;
        }
        super.dispose();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.forms.editor.FormEditor#createToolkit(org.eclipse.swt.widgets.Display)
     */
    @Override
    protected FormToolkit createToolkit(Display display)
    {
        return new FormToolkit(SoarEditorUIPlugin.getDefault().getFormColors(display));
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.forms.editor.FormEditor#addPages()
     */
    @Override
    protected void addPages()
    {
        try
        {
            addPage(new SoarAgentOverviewPage(this));
        }
        catch (PartInitException e)
        {
            SoarEditorUIPlugin.log(e);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave(IProgressMonitor monitor)
    {
        if(agent != null)
        {
            try
            {
                agent.save(monitor);
            }
            catch (SoarModelException e)
            {
                SoarEditorUIPlugin.log(e);
            }
        }
        dirty = false;
        editorDirtyStateChanged();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs()
    {

    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#getTitle()
     */
    @Override
    public String getTitle()
    {
        return agent != null ? agent.getName() : super.getTitle();
    }

    public void updateTitle()
    {
        firePropertyChange(IWorkbenchPart.PROP_TITLE);
    }

    @Override
    public boolean isDirty()
    {
        return dirty;
    }
    
    public void setDirty(boolean dirty)
    {
        this.dirty = dirty;
        firePropertyChange(PROP_DIRTY);
    }
    
    
    private class ProjectListener implements IResourceChangeListener
    {
        /* (non-Javadoc)
         * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
         */
        public void resourceChanged(IResourceChangeEvent event)
        {
            if(agent == null)
            {
                return;
            }
            if(event.getType() == IResourceChangeEvent.POST_CHANGE)
            {
                IResourceDelta delta = event.getDelta().findMember(agent.getFile().getFullPath());
                if(delta != null && delta.getKind() == IResourceDelta.REMOVED)
                {
                    close(false);
                }
            }
            else
            {
                ISoarProject soarProject = agent.getSoarProject();
                if(event.getResource() == soarProject.getProject())
                {
                    close(false);
                }
            }
            
        }
        
    }
}
