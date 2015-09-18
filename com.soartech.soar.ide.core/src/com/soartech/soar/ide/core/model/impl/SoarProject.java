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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.SoarProjectNature;
import com.soartech.soar.ide.core.builder.SoarBuilder;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.SoarModelEvent;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarModelTools;
import com.soartech.soar.ide.core.model.impl.datamap.SoarDatamap;

/**
 * @author ray
 */
public class SoarProject extends AbstractSoarOpenable 
						 implements ISoarProject
{
    public static final String PREFERENCE_SCOPE = SoarCorePlugin.PLUGIN_ID + ".project";
    public static final String AGENT_PRIORITIES = "agentPriorities";
    
    private IProject project;
    private List<SoarAgent> agents = new ArrayList<SoarAgent>();
    private List<IFile> agentPriorities = new ArrayList<IFile>();
    private boolean needsFullRebuild = false;
    
    /**
     * Map of all Soar files for quick lookup. Files are also stored in
     * child list.
     */
    private Map<IFile, SoarFile> soarFileMap = new HashMap<IFile, SoarFile>();
    
    private IResourceChangeListener projectCloseListener = new ProjectCloseListener();

    public SoarProject(SoarModel model, IProject project)
    {
        super(model);
        this.project = project;
        
        // Register for close and delete events so we can clean ourselves up.
        this.project.getWorkspace().addResourceChangeListener(projectCloseListener , 
                    IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE);
        System.out.println("Soar project attached to project " + project.getName());
        readPreferences();
    }

    
    /**
     * Returns true if the given project has the Soar project nature
     * 
     * @param project The project to check
     * @return true if project has Soar project nature, or if the project does not
     *          exist or is closed
     * @throws SoarModelException
     */
    public static boolean hasNature(IProject project) throws SoarModelException
    {
        try
        {
            return project.exists() &&
                   project.isOpen() && 
                   project.hasNature(SoarProjectNature.NATURE_ID);
        }
        catch (CoreException e)
        {
            throw new SoarModelException(e);
        }
    }
    
    /**
     * Method called by the builder to clean a project out for rebuilding. No
     * one but SoarBuilder should call this! 
     * @throws SoarModelException 
     */
    public void clean() throws SoarModelException
    {
        synchronized (getLock())
        {
            System.out.println("Detaching Soar project from project " + project.getName());
            
            for(SoarAgent agent : agents)
            {
                agent.detach();
            }
            agents.clear();
            
            soarFileMap.clear();
            clearChildren();
        }
    }
    
    /**
     * @return the needsFullRebuild
     */
    public boolean needsFullRebuild()
    {
        return needsFullRebuild;
    }

    protected void detach()
    {
        synchronized(getLock())
        {
            System.out.println("Detaching Soar project from project " + project.getName());
            soarFileMap.clear();
            
            for(SoarAgent agent : agents)
            {
                agent.detach();
            }
            agents.clear();
            
            // Don't need events any more
            project.getWorkspace().removeResourceChangeListener(projectCloseListener);
            super.detach();
        }
    }
    
    private void readPreferences()
    {
        agentPriorities.clear();
        
        IScopeContext scope = new ProjectScope(project);
        IEclipsePreferences prefs = scope.getNode(PREFERENCE_SCOPE);
        if(prefs != null)
        {
            readAgentPriorities(prefs);
        }
    }

    /**
     * @param prefs
     */
    private void readAgentPriorities(IEclipsePreferences prefs)
    {
        String agentPrioritiesString = prefs.get(AGENT_PRIORITIES, "");
        String[] tokens = agentPrioritiesString.split(";");
        for(String token : tokens)
        {
            IResource resource = getProject().findMember(Path.fromPortableString(token.trim()));
            if(resource instanceof IFile)
            {
                agentPriorities.add((IFile) resource);
            }
        }
    }
    
    public void storePreferences()
    {
        IScopeContext scope = new ProjectScope(project);
        IEclipsePreferences prefs = scope.getNode(PREFERENCE_SCOPE);
        if(prefs == null)
        {
            return;
        }
        
        synchronized (getLock())
        {
            needsFullRebuild = true;
            storeAgentPriorities(prefs);
        }
        try
        {
            prefs.flush();
        }
        catch (BackingStoreException e)
        {
            SoarCorePlugin.log(e);
        }
    }

    /**
     * 
     */
    private void storeAgentPriorities(IEclipsePreferences prefs)
    {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for(IFile file : agentPriorities)
        {
            if(!first)
            {
                builder.append(';');
            }
            builder.append(file.getProjectRelativePath().toPortableString());
            first = false;
        }
        
        prefs.put(AGENT_PRIORITIES, builder.toString());
    }
    
    public SoarAgent getOrCreateSoarAgent(IFile file) throws SoarModelException
    {
        synchronized (getLock())
        {
            ISoarAgent agent = getAgent(file);
            if(agent != null)
            {
                return (SoarAgent) agent;
            }
            
            SoarAgent newAgent = new SoarAgent(this, file);
            agents.add(newAgent);
            if(!agentPriorities.contains(file))
            {
                agentPriorities.add(file);
            }
            sortAgents();
            fireEvent(SoarModelEvent.createAdded(newAgent));
            return newAgent;
        }
    }
    
    public void removeSoarAgent(SoarAgent agent)
    {
        synchronized(getLock())
        {
            agents.remove(agent);
            agentPriorities.remove(agent.getFile());
            fireEvent(SoarModelEvent.createRemoved(agent));
        }
    }
    
    public SoarFile getOrCreateSoarFile(IFile file) throws SoarModelException
    {
        synchronized(getLock())
        {
            ISoarFile soarFile = getSoarFile(file);
            if(soarFile != null)
            {
                return (SoarFile) soarFile;
            }
            
            SoarFile newSoarFile = new SoarFile(this, file);
            soarFileMap.put(file, newSoarFile);
            addChild(newSoarFile);
            return newSoarFile;
        }
    }
    
    public ISoarFile getSoarFile(IFile file) throws SoarModelException
    {
        synchronized(getLock())
        {
            return soarFileMap.get(file);
        }
    }
    
    public void removeSoarFile(SoarFile soarFile) throws SoarModelException
    {
        synchronized (getLock())
        {
            soarFileMap.remove(soarFile.getFile());
            removeChild(soarFile);
        }
    }
        
    private List<IFile> getFiles() throws SoarModelException
    {
        final List<IFile> r = new ArrayList<IFile>();
        try
        {
            project.accept(new IResourceProxyVisitor() {

                public boolean visit(IResourceProxy proxy) throws CoreException
                {
                    if(!proxy.isDerived() && SoarModelTools.isSoarFile(proxy))
                    {
                        r.add((IFile) proxy.requestResource());
                    }
                    return true;
                }}, IResource.NONE);
        }
        catch (CoreException e)
        {
            throw new SoarModelException(e);
        }
        return r;
    }
    
    private List<IFile> getAgentFiles() throws SoarModelException
    {
        final List<IFile> r = new ArrayList<IFile>();
        try
        {
            project.accept(new IResourceProxyVisitor() {

                public boolean visit(IResourceProxy proxy) throws CoreException
                {
                    if(!proxy.isDerived() && SoarModelTools.isAgentFile(proxy))
                    {
                        r.add((IFile) proxy.requestResource());
                    }
                    return true;
                }}, IResource.NONE);
        }
        catch (CoreException e)
        {
            throw new SoarModelException(e);
        }
        return r;
    }
    
    private void sortAgents()
    {
        Collections.sort(agents, new AgentPriorityComparator());
    }

    /**
     * Update markers on the project itself.
     * 
     * @throws SoarModelException
     */
    private void updateProjectMarkers() throws SoarModelException
    {
        try
        {
            // Clear any problem markers directly on the project
            project.deleteMarkers(SoarCorePlugin.PROBLEM_MARKER_ID, true, IResource.DEPTH_ZERO);
            
            // Clear any Tcl pre-processor problem markers at any level in the
            // project. These will be recreated when we run the agent's 
            // interpreters
            project.deleteMarkers(SoarCorePlugin.TCL_PREPROCESSOR_PROBLEM_MARKER_ID, 
                                  true, IResource.DEPTH_INFINITE);
        }
        catch (CoreException e)
        {
            throw new SoarModelException(e);
        }
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProject#getProject()
     */
    public IProject getProject()
    {
        return project;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProject#getAgent(org.eclipse.core.resources.IFile)
     */
    public ISoarAgent getAgent(IFile file) throws SoarModelException
    {
        synchronized (getLock())
        {
            for(ISoarAgent agent : getAgents())
            {
                if(agent.getCorrespondingResource().equals(file))
                {
                    return agent;
                }
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProject#getAgents()
     */
    public List<ISoarAgent> getAgents() throws SoarModelException
    {
        synchronized(getLock())
        {
            openWhenClosed(new NullProgressMonitor());
            return new ArrayList<ISoarAgent>(agents);
        }
    }


    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProject#getPreferredAgent(com.soartech.soar.ide.core.model.ISoarFile)
     */
    public ISoarAgent getPreferredAgent(ISoarFile soarFile) throws SoarModelException
    {
        synchronized(getLock())
        {
            for(ISoarAgent agent : getAgents())
            {
                if(agent.contains(soarFile.getFile()))
                {
                    return agent;
                }
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarProject#setAgentPriorities(java.util.List)
     */
    public void setAgentPriorities(List<ISoarAgent> newAgentPriorities) throws SoarModelException
    {
        synchronized (getLock())
        {
            openWhenClosed(new NullProgressMonitor());
            if(newAgentPriorities.equals(agents))
            {
                System.out.println(getPath() + ": agent priorities unchanged. Ignoring.");
                return;
            }
            agentPriorities.clear();
            for(ISoarAgent agent : newAgentPriorities)
            {
                agentPriorities.add(agent.getFile());
            }
            sortAgents();
        }
        
        // Saving project preferences causes a project build IN ANOTHER THREAD
        // so we have to make sure not to call this while holding the lock.
        storePreferences();
        
        fireEvent(SoarModelEvent.createChanged(this));
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getContainingResource()
     */
    public IResource getContainingResource()
    {
        return this.project;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getCorrespondingResource()
     */
    public IResource getCorrespondingResource()
    {
        return this.project;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getPath()
     */
    public IPath getPath()
    {
        return project.getFullPath();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarOpenable#makeConsistent(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void makeConsistent(IProgressMonitor monitor) throws SoarModelException
    {
        monitor = SoarModelTools.getSafeMonitor(monitor);
        synchronized(getLock())
        {
            try
            {
                monitor.beginTask("Preparing Soar project " + project.getName(), agents.size());
                needsFullRebuild = false;
                updateProjectMarkers();
                SoarModelTools.checkForCancellation(monitor);
                
                for(SoarAgent agent : agents)
                {
                    agent.performTclPreprocessing(new SubProgressMonitor(monitor, 1));
                    SoarModelTools.checkForCancellation(monitor);
                }
            }
            finally
            {
                monitor.done();
            }
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarOpenable#open(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void open(IProgressMonitor monitor) throws SoarModelException
    {
        if(isOpen())
        {
            return;
        }
        
        boolean rebuild = false;
        synchronized (getLock())
        {
            monitor = SoarModelTools.getSafeMonitor(monitor);
            try
            {
                monitor.beginTask("Opening Soar project " + project.getName(), IProgressMonitor.UNKNOWN);
                rebuild = !doOpen(monitor);
            }
            finally
            {
                monitor.done();
            }
        }
        
        if(rebuild)
        {
            SoarBuilder.scheduleBuild(this, SoarBuilder.FULL_BUILD);
        }
    }

    /**
     * Helper method to open the project. Returns true on success, false if
     * a full rebuild is required due to missing serialized resources. This is
     * a separate method so that we can invoke the build while the lock is
     * not held and avoid deadlocks.
     * 
     * @param monitor
     * @return
     * @throws SoarModelException
     */
    private boolean doOpen(IProgressMonitor monitor) throws SoarModelException
    {
        super.open(monitor);
        
        // Find and open all agents
        for(IFile file : getAgentFiles())
        {
            monitor.subTask("Opening agent: " + file.getFullPath());
            
            ISoarAgent agent = getOrCreateSoarAgent(file);
            agent.makeConsistent(monitor);
            monitor.worked(1);
        }
        
        // Find and deserialize files
        List<AbstractSoarElement> deserialized = new ArrayList<AbstractSoarElement>();
        for(IFile file : getFiles())
        {
            monitor.subTask("Deserializing: " + file.getFullPath());
            try
            {
                SoarFile outFile = SoarBuilder.deserializeOutputFile(this, file);
                deserialized.add(outFile);
            }
            catch (SoarModelException exception)
            {
                // Clean up the files we've read so far
                for(AbstractSoarElement e : deserialized)
                {
                    e.detach();
                }
                
                // Do a full build.
                return false;
            }
            monitor.worked(1);
        }
        
        for(AbstractSoarElement fileElement : deserialized)
        {
            SoarFile file = (SoarFile) fileElement;
            soarFileMap.put(file.getFile(), file);
        }
        addChildren(deserialized);

        for(SoarAgent agent : agents)
        {
            monitor.subTask(agent.getName() + ": Deserializing datamap");
            if(!SoarBuilder.deserializeDatamap(this, agent, (SoarDatamap) agent.getDatamap()))
            {
                // Do a full build.
                return false;
            }
        }
        makeConsistent(monitor);
        
        return true;
    }
        
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#getAdapter(java.lang.Class)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class adapter)
    {
        if(adapter.equals(IProject.class))
        {
            if(project != null)
            {
                return project;
            }
        }
        return super.getAdapter(adapter);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "SoarProject " + project;
    }
    	    
    /**
     * Listens for project close or delete events so we can clean up the model
     * appropriately.
     */
    private class ProjectCloseListener implements IResourceChangeListener
    {
        /* (non-Javadoc)
         * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
         */
        public void resourceChanged(IResourceChangeEvent event)
        {
            if(project != event.getResource())
            {
                return;
            }
            
            boolean deleted = (event.getType() & IResourceChangeEvent.PRE_DELETE) != 0;
            
            System.out.println("Project " + project.getName() + 
                               (deleted ? " deleted." : " closed."));
            
            try
            {
                if(deleted)
                {
                    SoarBuilder.cleanProjectOutput(project);
                }
                getInternalSoarModel().removeSoarProject(SoarProject.this);
            }
            catch (SoarModelException e)
            {
                SoarCorePlugin.log(e.getStatus());
            }
        }
        
    }
    
    /**
     * Called by the builder at the end of a build to finish any build tasks. 
     * 
     * @param incremental True if the build was incremental 
     * @throws SoarModelException
     */
    public void finishBuild(boolean incremental, IProgressMonitor monitor) throws SoarModelException 
    {
    	synchronized( getLock() ) 
        {
            try
            {
                monitor.beginTask("Finishing build of Soar project " + project.getName(), 
                                  incremental ? 2 : 1);
                if(incremental)
                {
                    makeSecondaryFilesConsistent(new SubProgressMonitor(monitor, 1));
                }
                
                for(SoarAgent agent : agents)
                {
                    agent.checkForOverwrittenProductions();
                }
                
                monitor.worked(1);
            }
            finally
            {
                monitor.done();
            }
    	}
    }
    
    private void makeSecondaryFilesConsistent(IProgressMonitor monitor) throws SoarModelException
    {
        Set<String> filesToBuild = new HashSet<String>();
        for(SoarAgent agent : agents)
        {
            SoarModelTclInterpreter interpreter = agent.getInterpreter();
            
            if(interpreter != null)
            {
                Set<String> agentFiles = interpreter.getFilesToBuild();
                filesToBuild.addAll(agentFiles);
                agentFiles.clear();
            }
        }
        
        try
        {
            monitor.beginTask("Processing secondary files", filesToBuild.size());
            for ( String fileToBuild : filesToBuild ) 
            {
                IResource resource = SoarModelTools.getEclipseResource(new Path(fileToBuild));
                if ( resource instanceof IFile ) 
                {
                    ISoarFile file = getSoarFile((IFile)resource);
                    file.makeConsistent(new SubProgressMonitor(monitor, 1));
                }
            }
        }
        finally
        {
            monitor.done();
        }
    }
        
    private class AgentPriorityComparator implements Comparator<ISoarAgent>
    {
        /* (non-Javadoc)
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(ISoarAgent a, ISoarAgent b)
        {
            int aIndex = agentPriorities.indexOf(a.getFile());
            if(aIndex == -1) aIndex = Integer.MAX_VALUE;
            int bIndex = agentPriorities.indexOf(b.getFile());
            if(bIndex == -1) bIndex = Integer.MAX_VALUE;
            
            return aIndex - bIndex;
        }
        
    }
}
