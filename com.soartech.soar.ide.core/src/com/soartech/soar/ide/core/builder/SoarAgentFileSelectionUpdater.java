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
package com.soartech.soar.ide.core.builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarModelTools;
import com.soartech.soar.ide.core.model.impl.SoarProject;

/**
 * This class listens for resource changes and in the workspace (added and
 * removed files and folders) and updates Soar agents appropriately. This makes
 * the agent file selection behave the way a user would expect it to. That is,
 * if they've selected the root of a project and then add some folders, those
 * folders should be automatically part of the agent until the user explicity
 * excludes them.
 * 
 * @author ray
 */
class SoarAgentFileSelectionUpdater
{
    /**
     * List of agent working copies we are working on. We get working copies
     * of all agents, modify them, and then save them in one batch. They're
     * smart enough to do nothing if nothing has changed.
     */
    private List<ISoarAgent> workingCopies = new ArrayList<ISoarAgent>();
        
    public void resourceChanged(IResourceDelta delta,
                                IProgressMonitor monitor) throws CoreException
    {
        monitor = SoarModelTools.getSafeMonitor(monitor);
        try
        {
            collectWorkingCopies();
            processResourceDelta(delta, monitor);
            List<ISoarAgent> changed = saveWorkingCopies(monitor);
            
            // Force an update of any changed agents.
            for(ISoarAgent agent : changed)
            {
                agent.makeConsistent(monitor);
            }
        }
        finally
        {
            discardWorkingCopies();
        }
    }
    
    /**
     * Collects working copies of all agents in the workspace
     * 
     * @throws SoarModelException
     */
    private void collectWorkingCopies() throws SoarModelException
    {
        workingCopies.clear();
        
        for(ISoarProject project : SoarCorePlugin.getDefault().getSoarModel().getProjects())
        {
            for(ISoarAgent agent : project.getAgents())
            {
                workingCopies.add(agent.getWorkingCopy());
            }
        }
    }
    
    /**
     * Save changes to all previously collected working copies.
     * 
     * @param monitor
     * @throws SoarModelException
     */
    private List<ISoarAgent> saveWorkingCopies(IProgressMonitor monitor) throws SoarModelException
    {
        List<ISoarAgent> changed = new ArrayList<ISoarAgent>();
        for(ISoarAgent workingCopy : workingCopies)
        {
            if(workingCopy.save(monitor))
            {
                changed.add(workingCopy.getPrimary());
            }
        }
        return changed;
    }
    
    /**
     * Discard all previously collected working copies
     */
    private void discardWorkingCopies()
    {
        for(ISoarAgent workingCopy : workingCopies)
        {
            workingCopy.discardWorkingCopy();
        }
        workingCopies.clear();
    }
    
    private void processResourceDelta(final IResourceDelta delta, 
                                      final IProgressMonitor monitor) throws CoreException
    {
        // Visit all the changes in this change event
        delta.accept(new IResourceDeltaVisitor() {

            public boolean visit(IResourceDelta delta) throws CoreException
            {
                IResource r = delta.getResource();
                
                // Skip non-Soar projects
                if(r.getProject() != null && !SoarProject.hasNature(r.getProject()))
                {
                    return false;
                }
                
                boolean processChildren = true;
                final int type = r.getType();
                final int flags = delta.getFlags();
                if(type == IResource.PROJECT && (flags & IResourceDelta.OPEN) != 0)
                {
                    // If it's a project that's just opening, don't process anything
                    processChildren = false;
                }
                else if(type == IResource.FOLDER &&
                   delta.getKind() == IResourceDelta.ADDED)
                {
                    processNewSoarFolder((IFolder) r, monitor);
                }
                else if(type != IResource.PROJECT &&
                        delta.getKind() == IResourceDelta.REMOVED)
                {
                    processChildren = processDeletedSoarResource(r, monitor);
                }
                return processChildren;
            }});
    }
    
    /**
     * Handle addition of a new folder to the workspace. If it's part of a 
     * Soar project, and it should be added to any of the project's agents,
     * it will be added to the agents.
     * 
     * @param folder The added folder.
     * @throws SoarModelException 
     */
    private void processNewSoarFolder(final IFolder folder, IProgressMonitor monitor) throws SoarModelException
    {
        if(folder.getName().startsWith("."))
        {
            return;
        }
        
        IWorkspaceRunnable operation = new IWorkspaceRunnable()
        {
            public void run(IProgressMonitor monitor) throws CoreException
            {
                IContainer parent = folder.getParent();
                for(ISoarAgent agent : workingCopies)
                {
                    if(agent.contains(parent))
                    {
                        agent.addFolder(folder);
                    }
                }
            }
        };
        
        try
        {
            ResourcesPlugin.getWorkspace().run(operation, monitor);
        }
        catch (CoreException e)
        {
            throw new SoarModelException(e);
        }
    }

    private boolean processDeletedSoarResource(final IResource resource, IProgressMonitor monitor) throws SoarModelException
    {
        // Stick the return value in a final array so we can set it in the runnable.
        final boolean[] returnValue = { true };
        
        // TODO: Not sure if this workspace runnable is really needed.
        IWorkspaceRunnable operation = new IWorkspaceRunnable()
        {
            public void run(IProgressMonitor monitor) throws CoreException
            {
                for(ISoarAgent agent : workingCopies)
                {
                    if(agent.contains(resource))
                    {
                        if(resource instanceof IFolder)
                        {
                            agent.removeFolder((IFolder) resource);
                            
                            // We can short-circuit here because agent.removeFolder
                            // removes all children as well.
                            returnValue[0] = false;
                        }
                        else if(resource instanceof IFile)
                        {
                            agent.removeFile((IFile) resource);
                            returnValue[0] = false;
                        }
                    }
                }
            }
        };
        
        try
        {
            ResourcesPlugin.getWorkspace().run(operation, monitor);
        }
        catch (CoreException e)
        {
            throw new SoarModelException(e);
        }
        
        return returnValue[0];
    }
}
