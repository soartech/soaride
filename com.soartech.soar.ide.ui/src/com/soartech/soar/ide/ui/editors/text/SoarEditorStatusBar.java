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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.IDE;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.actions.NewSoarAgentWizardActionDelegate;

/**
 * Implementation of the status bar at the top of the Soar file editor.
 * 
 * @author ray
 */
public class SoarEditorStatusBar
{
    private static final String NEW_AGENT_LINK = "?newAgent";
    private static final String ADD_TO_AGENT_LINK = "?addToAgent";
    private static final String REMOVE_FROM_AGENT_LINK = "?removeFromAgent";
    
    private SoarEditor editor;
    private Link agentsLink;
    
    /**
     * Constructor
     * 
     * @param editor The owning editor
     * @param parent The parent composite
     * @param rulerWidth The width of the editor's vertical ruler to allow 
     *      proper alignment.
     */
    public SoarEditorStatusBar(SoarEditor editor, Composite parent, int rulerWidth)
    {
        this.editor = editor;
        agentsLink = new Link(parent, SWT.NONE);
        
        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = GridData.FILL;
        gd.horizontalIndent = rulerWidth;
        agentsLink.setLayoutData(gd);
        
        agentsLink.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e)
            {
                agentClicked(e);
            }});
        
    }
    
    /**
     * Update the status bar after changes in the editor or model. 
     */
    public void update()
    {
        updateAgentsLink();
    }
    
    private String getNewAgentLink()
    {
        return "There are no agents in this project. " +
            "<a href=\"" + NEW_AGENT_LINK + "\">Click here</a> " +
            "to create a new agent.";
    }
    
    private String getAddToAgentLink(ISoarFile wc) throws SoarModelException
    {
        List<ISoarAgent> agents = wc.getSoarProject().getAgents();
        if(agents.isEmpty())
        {
            return getNewAgentLink();
        }
        
        StringBuilder b = new StringBuilder("This file is not a member of any agent. Add to agent: ");
        boolean first = true;
        for(ISoarAgent agent : agents)
        {
            if(!first)
            {
                b.append(", ");
            }
            b.append("<a href=\"" + ADD_TO_AGENT_LINK + agent.getFile().getFullPath().toPortableString() + "\">" +
                     agent.getName() + " (+)</a>");
            first = false;
        }
        
        return b.toString();
    }
    
    private String getAgentsLink(ISoarFile wc)
    {
        IFile file = wc.getFile();
        List<ISoarAgent> agents = new ArrayList<ISoarAgent>();
        try
        {
            for(ISoarAgent agent : wc.getSoarProject().getAgents())
            {
                if(agent.contains(file))
                {
                    agents.add(agent);
                }
            }
            
            if(agents.isEmpty())
            {
                return getAddToAgentLink(wc);
            }
            
            boolean first = true;
            StringBuilder b = new StringBuilder(agents.size() == 1 ? "Member of: " : "Member of: ");
            for(ISoarAgent agent : agents)
            {
                if(!first)
                {
                    b.append(", ");
                }
                else
                {
                    b.append("[");
                }
                final String agentPath = agent.getFile().getFullPath().toPortableString();
                b.append("<a href=\"" + agentPath + "\">" + agent.getName() + "</a> ");
                b.append("(<a href=\"" + REMOVE_FROM_AGENT_LINK + agentPath + "\">-</a>)");
                if(first)
                {
                    b.append("]");
                }
                first = false;
            }
            return b.toString();
        }
        catch (SoarModelException e)
        {
            SoarEditorUIPlugin.log(e);
            return "Error: " + e.getMessage();
        }
    }
    
    private void updateAgentsLink()
    {
        if(editor.isDisposed() || agentsLink == null)
        {
            return;
        }
        
        synchronized (editor.getWorkingCopyLock())
        {
            ISoarFile wc = editor.getSoarFileWorkingCopy();
            String link = "";
            if(wc == null)
            {
                link = getNewAgentLink();
            }
            else
            {
                link = getAgentsLink(wc);
            }
            
            agentsLink.setText(link);
        }
    }
    
    private void agentClicked(SelectionEvent event)
    {
        String link = event.text;
        
        if(NEW_AGENT_LINK.equals(link))
        {
            IFile thisFile = ((IFileEditorInput) editor.getEditorInput()).getFile();
            NewSoarAgentWizardActionDelegate.showWizard(editor.getEditorSite().getShell(),
                    thisFile != null ? new StructuredSelection(thisFile.getProject()) : null);
        }
        else if(link.startsWith(ADD_TO_AGENT_LINK))
        {
            modifyAgent(link, ADD_TO_AGENT_LINK);
        }
        else if(link.startsWith(REMOVE_FROM_AGENT_LINK))
        {
            modifyAgent(link, REMOVE_FROM_AGENT_LINK);
        }
        else
        {
            openAgent(link);
        }
    }
    
    private void modifyAgent(final String link, final String prefix)
    {
        WorkspaceModifyOperation operation = new WorkspaceModifyOperation()
        {
            @Override
            protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException
            {
                doModifyAgent(link, prefix);
            }
        };
    
        try
        {
            new ProgressMonitorDialog(editor.getEditorSite().getShell()).run(false, true, operation);
        }
        catch (InvocationTargetException e)
        {
            SoarEditorUIPlugin.log(e);
        }
        catch (InterruptedException e)
        {
        }
    }
    
    private void doModifyAgent(String link, String prefix) throws SoarModelException
    {
        IFile thisFile = ((IFileEditorInput) editor.getEditorInput()).getFile();
        if(thisFile == null)
        {
            return;
        }
        
        String agentPathString = link.substring(prefix.length());
        IPath path = Path.fromPortableString(agentPathString);
        IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
        if(resource == null || !(resource instanceof IFile))
        {
            return;
        }
        
        IFile agentFile = (IFile) resource;
        ISoarAgent agent = SoarCorePlugin.getDefault().getSoarModel().getAgent(agentFile);
        if(agent != null)
        {
            ISoarAgent wc = agent.getWorkingCopy();
            try
            {
                boolean add = ADD_TO_AGENT_LINK.equals(prefix);
                if(add)
                {
                    wc.addFile(thisFile);
                }
                else
                {
                    wc.removeFile(thisFile);
                }
                wc.save(new NullProgressMonitor());
            }
            finally
            {
                if(wc != null)
                {
                    wc.discardWorkingCopy();
                }
            }
        }
    }
    
    /**
     * @param link
     */
    private void openAgent(String link)
    {
        IPath path = Path.fromPortableString(link);
        IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
        if(resource == null || !(resource instanceof IFile))
        {
            return;
        }
        
        try
        {
            IDE.openEditor(editor.getEditorSite().getPage(), (IFile) resource);
        }
        catch (PartInitException e)
        {
            SoarEditorUIPlugin.log(e);
        }
    }}
