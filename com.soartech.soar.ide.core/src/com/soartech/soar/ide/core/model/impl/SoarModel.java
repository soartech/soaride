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
package com.soartech.soar.ide.core.model.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.builder.SoarBuilder;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.model.ISoarModelListener;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.ITclHelpModel;
import com.soartech.soar.ide.core.model.SoarModelEvent;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarModelTools;

/**
 * Implementation of the ISoarModel interface. The instance of this object 
 * should only be accessed through SoarModelManager.
 *  
 * @author ray
 */
public class SoarModel extends AbstractSoarOpenable implements ISoarModel
{
    private List<ISoarModelListener> listeners = new ArrayList<ISoarModelListener>();
    private SoarBufferManager bufferManager = new SoarBufferManager();
    
    private ProjectOpenListener projectOpenListener = new ProjectOpenListener();
    
    private ITclHelpModel tclHelpModel = new TclHelpModel();
    
    private int queuedEventsDepth = 0;
    private List<SoarModelEvent> queuedEvents = new ArrayList<SoarModelEvent>();
    private int queuedTypedEvents[] = { 0, 0, 0 };
    
    /**
     * Construct a new Soar model. This method should not be used by client code.
     * Instead, access the model through SoarCorePlugin.getDefault.getSoarModel().
     */
    public SoarModel()
    {
        super(null);
        System.out.println("SoarModel implementation constructed.");
        
        // Register for workspace changes, in particular, we want to know
        // when a project is opened so we can connect the model to it.
        getWorkspace().addResourceChangeListener(projectOpenListener, 
                IResourceChangeEvent.POST_CHANGE);
    }
    
    /**
     * @return The buffer manager for the model
     */
    public SoarBufferManager getBufferManager()
    {
        return bufferManager;
    }
    
    /**
     * Construct an ISoarProject object for the given Eclipse project. If the
     * project already exists, it is simply returned. Otherwise, a new one is
     * created and an ADDED event is fired.
     * 
     * @param project The Eclipse project.
     * @return The Soar project or null if the project does not have the Soar
     *          project nature.
     * @throws SoarModelException
     */
    public SoarProject createSoarProject(IProject project) throws SoarModelException
    {
        if(!SoarProject.hasNature(project))
        {
            return null;
        }
        
        synchronized(getLock())
        {
            ISoarProject existing = getProject(project.getName());
            if(existing != null)
            {
                assert existing.getProject() == project;
                return (SoarProject) existing;
            }
            
            SoarProject soarProject = new SoarProject(this, project);
            addChild(soarProject);
            
            return soarProject;
        }
    }
    
    /**
     * Removes the given soar project from the model and fires a REMOVED event.
     * 
     * @param project The project to remove
     * @throws SoarModelException
     */
    public void removeSoarProject(ISoarProject project) throws SoarModelException
    {
        SoarProject soarProject = (SoarProject) project;
        removeChild(soarProject);
    }
    
    /**
     * Fire a model event
     * 
     * @param event The event to fire
     */
    public void fireEvent(SoarModelEvent event)
    {
        synchronized(queuedEvents)
        {
            if(queuedEventsDepth == 0)
            {
                List<ISoarModelListener> safeListeners = new ArrayList<ISoarModelListener>(listeners);
                for(ISoarModelListener listener : safeListeners)
                {
                    listener.onEvent(event);
                }
            }
            else
            {
                queuedEvents.add(event);
                queuedTypedEvents[event.getType()] += event.getElements().length;
            }
        }
    }
    
    public void beginModification()
    {
        synchronized(queuedEvents)
        {
            ++queuedEventsDepth;
        }
    }
    
    public void endModification()
    {
        synchronized (queuedEvents)
        {
            --queuedEventsDepth;
            if(queuedEventsDepth < 0)
            {
                throw new IllegalStateException("Too many calls to endModification");
            }
            else if(queuedEventsDepth == 0)
            {
                // Create initial composite events for each type of event, indexed by type
                SoarModelEvent[] events = new SoarModelEvent[SoarModelEvent.MAX_ELEMENT_TYPE];
                for(int i = 0; i < SoarModelEvent.MAX_ELEMENT_TYPE; ++i)
                {
                    int count = queuedTypedEvents[i];
                    events[i] = new SoarModelEvent(new ISoarElement[count], i);
                }
                
                // Current index in each event's element list
                int[] indexes = new int[SoarModelEvent.MAX_ELEMENT_TYPE];
                
                // Sort queued events into bins by type
                for(SoarModelEvent event : queuedEvents)
                {
                    int type = event.getType();
                    
                    final ISoarElement[] elements = event.getElements();
                    System.arraycopy(elements, 0,
                                     events[type].getElements(), indexes[type], 
                                     elements.length);
                    
                    indexes[type] += elements.length;
                }
                
                for(int i = 0; i < SoarModelEvent.MAX_ELEMENT_TYPE; ++i)
                {
                    assert indexes[i] == events[i].getElements().length;
                }
                
                // Reset queued event state 
                queuedEvents.clear();
                Arrays.fill(queuedTypedEvents, 0);
                
                // Fire composite events.
                for(SoarModelEvent event : events)
                {
                    fireEvent(event);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarModel#addListener(com.soartech.soar.ide.core.model.ISoarModelListener)
     */
    public void addListener(ISoarModelListener listener)
    {
        if(!listeners.contains(listener))
        {
            listeners.add(listener);
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarOpenable#open(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void open(IProgressMonitor monitor) throws SoarModelException
    {
        monitor = SoarModelTools.getSafeMonitor(monitor);
        
        synchronized(getLock())
        {
            beginModification();
            try
            {
                super.open(monitor);
                
                for(IProject project : getWorkspace().getRoot().getProjects())
                {
                    if(project.isOpen() && SoarProject.hasNature(project))
                    {
                        SoarProject sp = createSoarProject(project);
                        sp.open(monitor);
                    }
                }
            }
            finally
            {
                endModification();
            }
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarModel#getProject(java.lang.String)
     */
    public ISoarProject getProject(String name) throws SoarModelException
    {
        synchronized(getLock())
        {
            for(ISoarElement element : getChildren())
            {
                if(element instanceof ISoarProject)
                {
                    ISoarProject project = (ISoarProject) element;
                    if(project.getProject().getName().equals(name))
                    {
                        return project;
                    }
                }
            }
            return null;
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarModel#getProjects()
     */
    public List<ISoarProject> getProjects() throws SoarModelException
    {
        synchronized(getLock())
        {
            List<ISoarProject> projects = new ArrayList<ISoarProject>();
            for(ISoarElement element : getChildren())
            {
                if(element instanceof ISoarProject)
                {
                    projects.add((ISoarProject) element);
                }
            }
            return projects;
        }
    }


    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarModel#getWorkspace()
     */
    public IWorkspace getWorkspace()
    {
        return ResourcesPlugin.getWorkspace();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarModel#removeListener(com.soartech.soar.ide.core.model.ISoarModelListener)
     */
    public void removeListener(ISoarModelListener listener)
    {
        listeners.remove(listener);
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getContainingResource()
     */
    public IResource getContainingResource()
    {
        return ResourcesPlugin.getWorkspace().getRoot();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getCorrespondingResource()
     */
    public IResource getCorrespondingResource()
    {
        // No corresponding resource for the model
        return null;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getPath()
     */
    public IPath getPath()
    {
        return Path.ROOT;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarOpenable#makeConsistent(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void makeConsistent(IProgressMonitor monitor) throws SoarModelException
    {
        monitor = SoarModelTools.getSafeMonitor(monitor);
        synchronized (getLock())
        {
            for(ISoarProject project : getProjects())
            {
                project.makeConsistent(monitor);
            }
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarModel#getFile(org.eclipse.core.resources.IFile)
     */
    public ISoarFile getFile(IFile file) throws SoarModelException
    {
        if(!SoarModelTools.isSoarFile(file))
        {
            return null;
        }
        IProject project = file.getProject();
        
        synchronized (getLock())
        {
            SoarProject soarProject = createSoarProject(project);
            if(soarProject == null)
            {
                return null;
            }
            
            return soarProject.getOrCreateSoarFile(file);            
        }

    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarModel#getAgent(org.eclipse.core.resources.IFile)
     */
    public ISoarAgent getAgent(IFile file) throws SoarModelException
    {
        if(!SoarModelTools.isAgentFile(file))
        {
            return null;
        }
        IProject project = file.getProject();
        if(!SoarProject.hasNature(project))
        {
            return null;
        }
        
        synchronized (getLock())
        {
            SoarProject soarProject = createSoarProject(project);
            if(soarProject == null)
            {
                return null;
            }
            
            return soarProject.getOrCreateSoarAgent(file);            
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarModel#getInitialAgentFileContents(org.eclipse.core.resources.IFile, boolean)
     */
    public String getInitialAgentFileContents(IFile file, boolean empty)
    {
        return SoarAgent.getInitialFileContents(file, empty);
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarModel#getTclHelpModel()
     */
    public ITclHelpModel getTclHelpModel()
    {
        return tclHelpModel;
    }

    /**
     * Unnecessarily complicated listener that determines when a project has
     * been opened.
     */
    private class ProjectOpenListener implements IResourceChangeListener
    {
        /* (non-Javadoc)
         * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
         */
        public void resourceChanged(IResourceChangeEvent event)
        {
            assert event.getType() == IResourceChangeEvent.POST_CHANGE;
            
            IResourceDelta delta = event.getDelta();
            // get deltas for the projects
            IResourceDelta[] projectDeltas = delta.getAffectedChildren();
            for (IResourceDelta projectDelta : projectDeltas)
            {
                int kind = projectDelta.getKind();
                int flags = projectDelta.getFlags();
                if (kind == IResourceDelta.CHANGED && ((flags & IResourceDelta.OPEN) != 0))
                {
                    IProject project = (IProject) projectDelta.getResource();
                    try
                    {
                        if(project.isOpen() && SoarProject.hasNature(project))
                        {
                            SoarProject soarProject = createSoarProject(project);
                            System.out.println("Scheduling initialization of project " + project.getName());
                            SoarBuilder.scheduleProjectInitialization(soarProject);
                        }
                    }
                    catch (SoarModelException e)
                    {
                        SoarCorePlugin.log(e.getStatus());
                    }
                }
            }
        }
        
    }
    
}
