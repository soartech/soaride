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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.jsoar.util.UrlTools;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarModelTools;
import com.soartech.soar.ide.core.model.impl.SoarAgent;
import com.soartech.soar.ide.core.model.impl.SoarFile;
import com.soartech.soar.ide.core.model.impl.SoarProject;
import com.soartech.soar.ide.core.model.impl.datamap.SoarDatamap;
import com.soartech.soar.ide.core.model.impl.serialization.DatamapMemento;
import com.soartech.soar.ide.core.model.impl.serialization.FileMemento;
import com.soartech.soar.ide.core.model.impl.serialization.Mementos;

/**
 * Implements a builder for Soar projects.  Automatically added to
 * projects when the SoarProjectNature is added.
 * 
 * @author ray
 */
public class SoarBuilder extends IncrementalProjectBuilder
{
    public static final String BUILDER_ID = SoarCorePlugin.PLUGIN_ID + ".builder";

    private SoarAgentFileSelectionUpdater changeHandler = new SoarAgentFileSelectionUpdater();
    
    /**
     * Add this builder to the given soarProject. This should only be called
     * when the Soar soarProject nature is added to a soarProject.
     * 
     * @param soarProject The soarProject
     * @throws CoreException
     */
    public static void addToProject(IProject project) throws CoreException
    {
        System.out.println("Adding Soar builder to soarProject " + project.getName());
        IProjectDescription desc = project.getDescription();
        List<ICommand> commands = new ArrayList<ICommand>(Arrays.asList(desc.getBuildSpec()));
        
        ICommand command = findBuilder(commands);
        if(command == null)
        {
            command = desc.newCommand();
            command.setBuilderName(BUILDER_ID);
            
            commands.add(command);
            
            desc.setBuildSpec(commands.toArray(new ICommand[commands.size()]));
            project.setDescription(desc, null);
        }
    }
    
    /**
     * Remove this builder from a soarProject. This should only be called when
     * the Soar soarProject nature is removed from a soarProject
     * 
     * @param soarProject The soarProject
     * @throws CoreException
     */
    public static void removeFromProject(IProject project) throws CoreException
    {
        System.out.println("Removing Soar builder from soarProject " + project.getName());
        IProjectDescription desc = project.getDescription();
        List<ICommand> commands = new ArrayList<ICommand>(Arrays.asList(desc.getBuildSpec()));
        
        ICommand command = findBuilder(commands);
        if(command != null)
        {
            commands.remove(command);
            
            desc.setBuildSpec(commands.toArray(new ICommand[commands.size()]));
            project.setDescription(desc, null);
        }
        cleanProjectOutput( project );
        project.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
    }
    
    /**
     * Schedule a build for the given Soar soarProject. If soarProject is 
     * <code>null</code>, all Soar projects are built.
     *   
     * @param soarProject The soarProject to build, or <code>null</code> for all
     * @param kind Kind of build (IncrementalProjectBuilder.XXXX)
     */
    public static void scheduleBuild(final ISoarProject project, final int kind)
    {
        System.out.println("Scheduling build for " + 
                          (project != null ? " soarProject " + project.getProject().getName() : 
                                                 "all Soar Projects"));
        Job buildJob = new Job(project != null ? "Building Soar project " + project.getProject().getName() : 
                                                 "Building Soar Projects") 
        {
            protected IStatus run(IProgressMonitor monitor)
            {
                final Status status = new Status(IStatus.OK, 
                        SoarCorePlugin.PLUGIN_ID, 0, "Soar projects initialized", null);
                try
                {
                    if(project == null)
                    {
                        final List<ISoarProject> projects = SoarCorePlugin.getDefault().getSoarModel().getProjects();
                        monitor.beginTask("Building Soar projects", projects.size());
                        for(ISoarProject project : projects)
                        {
                            project.getProject().build(kind, new SubProgressMonitor(monitor, 1));
                        }
                    }
                    else
                    {
                        monitor.beginTask("Building Soar project " + project.getProject().getName(), 1);
                        project.getProject().build(kind, new SubProgressMonitor(monitor, 1));
                    }
                }
                catch(CoreException e)
                {
                    return e.getStatus();
                }
                finally
                {
                    monitor.done();
                }
                
                return status;
            }
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.core.runtime.jobs.Job#belongsTo(java.lang.Object)
             */
            public boolean belongsTo(Object family) {
                return ResourcesPlugin.FAMILY_MANUAL_BUILD == family;
            }
        };
        buildJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
        buildJob.setUser(true); // Show job in UI progress bar
        buildJob.schedule();
    }
    
    public static void scheduleProjectInitialization(final SoarProject project)
    {
        System.out.println("Scheduling open for Soar soarProject " + project.getProject().getName());
        Job buildJob = new Job("Opening soarProject " + project.getProject().getName()) {

            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                try
                {
                    project.open(monitor);
                }
                catch (SoarModelException e)
                {
                    return e.getStatus();
                }
                return new Status(IStatus.OK, 
                        SoarCorePlugin.PLUGIN_ID, 0, "Project opened", null);
            }
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.core.runtime.jobs.Job#belongsTo(java.lang.Object)
             */
            public boolean belongsTo(Object family) {
                return ResourcesPlugin.FAMILY_MANUAL_BUILD == family;
            }
        };
        buildJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
        buildJob.setUser(true); // Show job in UI progress bar
        buildJob.schedule();
        
    }
    
    /**
     * Find this builder in the given list of builder commands
     * 
     * @param commands List of builder commands from a soarProject
     * @return The command, or null if not found
     */
    private static ICommand findBuilder(List<ICommand> commands)
    {
        for(ICommand command : commands)
        {
            if(command.getBuilderName().equals(BUILDER_ID))
            {
                return command;
            }
        }
        return null;
    }
    
    /**
     * Deserialize the output file for a given soar file
     * 
     * @param soarProject The owning soarProject
     * @param file The soar file
     * @return The deserialized SoarFile, never null.
     * @throws SoarModelException if an error occurs.
     */
    public static SoarFile deserializeOutputFile(SoarProject project, IFile file) throws SoarModelException
    {
        assert SoarModelTools.isSoarFile(file);
        
        // Find or create workspace build directory
        File buildDir = createBuildDirectory();
        
        // Build path to output file
        File outputFile = new File(buildDir, file.getFullPath().toPortableString() + ".ser");
        
        if(!outputFile.exists() || !outputFile.isFile())
        {
            throw new SoarModelException("Serialized file does not exist: " + outputFile);
        }
        
        System.out.println("Deserializing " + outputFile);
        FileMemento memento = (FileMemento) Mementos.deserialize(outputFile);
        if(memento == null)
        {
            throw new SoarModelException("Failed to deserialize memento: " + outputFile);
        }
        
        SoarFile soarFile = new SoarFile(project, file, memento);
        
        return soarFile;
    }
    
    public static boolean deserializeDatamap(SoarProject project, SoarAgent agent, SoarDatamap datamap)
    {
        // Find or create workspace build directory
        File buildDir = createBuildDirectory();
        
        // TODO: Make sure agent name is unique
        File datamapFile = new File(buildDir, getDatamapName(agent));
        if(!datamapFile.exists() || !datamapFile.isFile())
        {
            return false;
        }
        
        System.out.println("Deserializing datamap from " + datamapFile);
        long start = System.currentTimeMillis();
        DatamapMemento memento = (DatamapMemento) Mementos.deserialize(datamapFile);
        if(memento == null)
        {
            return false;
        }
        
        boolean r = datamap.deserialize(project, memento);
        System.out.println("... done in " + (System.currentTimeMillis() - start) + " ms");
        return r;
    }
    
    /**
     * Create and return the plugin's build directory within the workspace
     * 
     * @return The build directory location
     */
    public static File createBuildDirectory()
    {
        File stateLoc = SoarCorePlugin.getDefault().getStateLocation().toFile();        
        File buildDir = new File(stateLoc, "build");
        buildDir.mkdirs();
        return buildDir;
    }
    
    public static File getProjectOutputDirectory(IProject project, boolean create)
    {
        // Find or create workspace build directory
        File buildDir = createBuildDirectory();
        
        File outputDir = new File(buildDir, project.getFullPath().toPortableString());
        if(create)
        {
            outputDir.mkdirs();
        }
        
        return outputDir;
    }
    
    /**
     * Clean the output for the given soarProject. This shoudl only be called by 
     * the soarProject when it is deleted or cleaned. 
     * 
     * @param soarProject The soarProject
     */
    public static void cleanProjectOutput(IProject project)
    {
        // Clear out the soarProject's output directory
        File dir = getProjectOutputDirectory(project, false);
        deleteDirectoryTree(dir);
        
        // Delete cached datamap
        File datamap = new File(createBuildDirectory(), project.getName() + ".datamap.ser");
        datamap.delete();
    }
    
    public static File createOutputDirectoryForFile(IFile file)
    {
        // Find or create workspace build directory
        File buildDir = createBuildDirectory();
        
        // Build path to output file
        File outputFile = new File(buildDir, file.getFullPath().toPortableString());
        
        // Build output file directory structure
        outputFile.getParentFile().mkdirs();
        
        return outputFile;
    }
    
    public static void deleteOutputFile(IFile file)
    {
        // Find or create workspace build directory
        File buildDir = createBuildDirectory();
        
        // Build path to output file
        File outputFile = new File(buildDir, file.getFullPath().toPortableString());
        
        // Delete the file
        outputFile.delete();
    }
    
    /**
     * Delete an entire directory tree. This is done manually because
     * File.delete() will only delete empty directories :(
     * 
     * @param dir The directory to delete
     */
    public static void deleteDirectoryTree(File dir)
    {
        if(!dir.exists() || !dir.isDirectory())
        {
            return;
        }
        
        for(File f : dir.listFiles())
        {
            if(f.isFile())
            {
                f.delete();
            }
            else
            {
                deleteDirectoryTree(f);
            }
        }
        dir.delete();
    }
    
    /**
     * Constructor. Called by Eclipse framework through builder
     * extension point.
     */
    public SoarBuilder()
    {
    }
    
    private void runBuildRunnable(IWorkspaceRunnable runnable, IProgressMonitor monitor)
    {
        try
        {
            ResourcesPlugin.getWorkspace().run(runnable, monitor);
        }
        catch (CoreException e)
        {
            SoarCorePlugin.log(e.getStatus());
        }        
    }
    
    private void fullBuild(IProgressMonitor monitor, SoarProject soarProject)
    {
        System.out.println("Full Soar build triggered on soarProject " + getProject().getName());
        
        runBuildRunnable(new BuildTask(soarProject, null), monitor);        
    }
    
    private boolean isSoarFileAffected(IResourceDelta delta)
    {
        IResource resource = delta.getResource();
        if(resource instanceof IFile)
        {
            if(SoarModelTools.isSoarFile((IFile) resource) ||
               SoarModelTools.isAgentFile((IFile) resource))
            {
                return true;
            }
        }
        else
        {
            for(IResourceDelta child : delta.getAffectedChildren())
            {
                if(isSoarFileAffected(child))
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    private void incrementalBuild(IProgressMonitor monitor, 
                                  SoarProject soarProject,
                                  IResourceDelta delta)
    {
        
        // Only process the delta if it has files of interest to us. That is, if
        // the user modifies and saves "readme.txt", there's no reason to do any 
        // Soar processing.
        if(!isSoarFileAffected(delta))
        {
            System.out.println("Ignoring incremental build triggered on soarProject " + getProject().getName() + ". No files affected.");
            return;
        }
        System.out.println("Incremental Soar build triggered on soarProject " + getProject().getName());
        runBuildRunnable(new BuildTask(soarProject, delta), monitor);        
    }
    
    SoarProject getSoarProject() throws SoarModelException
    {
    	return SoarCorePlugin.getDefault().getInternalSoarModel().createSoarProject(getProject());
    }
    
    SoarFile getSoarFile(IResource resource) throws SoarModelException
    {
        IFile file = (IFile) resource.getAdapter(IFile.class);
        if(file == null)
        {
            return null;
        }
        if(!SoarModelTools.isSoarFile(file))
        {
            return null;
        }
        
        SoarProject project = getSoarProject();
        return project.getOrCreateSoarFile(file);
    }
    
    SoarAgent getSoarAgent(IResource resource) throws SoarModelException
    {
        IFile file = (IFile) resource.getAdapter(IFile.class);
        if(file == null)
        {
            return null;
        }
        if(!SoarModelTools.isAgentFile(file))
        {
            return null;
        }
        if(!file.isAccessible()) // file could be deleted and not yet refreshed
        {
        	return null;
        }
        
        SoarProject project = getSoarProject();
        return project.getOrCreateSoarAgent(file);
    }
    
    
    /* (non-Javadoc)
     * @see org.eclipse.core.resources.IncrementalProjectBuilder#build(int, java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IProject[] build(int kind, Map<String,String> args, IProgressMonitor monitor)
            throws CoreException
    {
        // In any kind of build, thre first thing we do is make the soarProject
        // consistent. In particular, we want to make sure the Tcl interpreter
        // has been run and is up to date.
        SoarProject soarProject = getSoarProject();
        if(kind == FULL_BUILD)
        {
            fullBuild(monitor, soarProject);
        }
        else
        {
            IResourceDelta delta = getDelta(getProject());
            if(delta == null || soarProject.needsFullRebuild() || resourceDeltaRequiresFullBuild(delta))
            {
                // Still have to process the delta in case any files were 
                // deleted.
                if(delta != null)
                {
                    incrementalBuild(monitor, soarProject, delta);
                }
                SoarModelTools.checkForCancellation(monitor);

                fullBuild(monitor, soarProject);
            }
            else
            {
                incrementalBuild(monitor, soarProject, delta);
            }
        }
        
        return null;
    }

    private boolean resourceDeltaRequiresFullBuild(IResourceDelta delta) throws CoreException
    {
        // Use the final array trick so that the inner class can pass back a 
        // boolean to us. 
        final boolean[] flag = { false };
        
        delta.accept(new IResourceDeltaVisitor() {

            public boolean visit(IResourceDelta delta) throws CoreException
            {
                // If the soarProject has been changed, or a Soar agent has been changed
                // we need to do a full build.
                if(null != getSoarAgent(delta.getResource()))
                {
                    flag[0] = true;
                    return false;
                }
                return true;
            }});
        
        return flag[0];
    }
    
    private static String getDatamapName(ISoarAgent agent)
    {
        // TODO: Make sure this is unique.
        String name = agent.getFile().getFullPath().toPortableString();
        name = name.replace('/', '_');
        return name + ".datamap.ser";
    }
    
    /**
     * @param soarProject
     * @throws SoarModelException 
     */
    private void saveDatamap(SoarProject soarProject) throws SoarModelException
    {
        for(ISoarAgent agent : soarProject.getAgents())
        {
            // Save the datamap.
            // TODO: Really only need to do this at shutdown when the model closes.
            SoarDatamap datamap = (SoarDatamap) agent.getDatamap();
            DatamapMemento memento = datamap.createMemento();
            Mementos.serialize(memento, new File(createBuildDirectory(), getDatamapName(agent)));
        }
    }

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException
    {
        System.out.println("Clean Soar build triggered on soarProject " + getProject().getName());

        cleanProjectOutput(getProject());
        
        // Clear out the soarProject (datamap, children, etc)
        try
        {
            SoarCorePlugin.getDefault().getInternalSoarModel().beginModification();
            getSoarProject().clean();
        }
        finally
        {
            SoarCorePlugin.getDefault().getInternalSoarModel().endModification();
        }
        
        // Remove all problem markers (as suggested in super.clean() comments)
        getProject().deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
    }

    @Override
    protected void startupOnInitialize()
    {
        super.startupOnInitialize();
        createBuildDirectory();
    }
    
    private final class BuildTask implements IWorkspaceRunnable
    {
        private final SoarProject soarProject;
        private final IResourceDelta delta; 
                
        private BuildTask(SoarProject project, IResourceDelta delta)
        {
            this.soarProject = project;
            this.delta = delta;
        }

        public void run(IProgressMonitor monitor) throws CoreException
        {
            try
            {
                monitor = SoarModelTools.getSafeMonitor(monitor);
                monitor.beginTask(toString(), IProgressMonitor.UNKNOWN);

                SoarCorePlugin.getDefault().getInternalSoarModel().beginModification();
                if(!isIncremental()) // Full build
                {
                    IProject project = getProject();
                    
                    ClassLoader cl = getProjectClassLoader(project);
                    UrlTools.setClasspathResourceResolverClassLoader(cl);
                    
                    // Clean up all markers on the project
                    SoarModelTools.deleteMarkers(project, SoarCorePlugin.PROBLEM_MARKER_ID);
                    SoarModelTools.deleteMarkers(project, SoarCorePlugin.TASK_MARKER_ID);
                    
                    project.accept(new SoarAgentVisitor(SoarBuilder.this, monitor));
                    soarProject.makeConsistent(new SubProgressMonitor(monitor, 1));
                    project.accept(new SoarFileVistor(SoarBuilder.this, monitor));
                }
                else // incremental build
                {
                    assert delta != null;
                    
                    // Update agents
                    delta.accept(new SoarAgentVisitor(SoarBuilder.this, monitor));
                    
                    // Update agents with any new files or folders
                    changeHandler.resourceChanged(delta, monitor);
                    
                    // Update the rest of the project and files
                    soarProject.makeConsistent(new SubProgressMonitor(monitor, 1));
                    delta.accept(new SoarFileVistor(SoarBuilder.this, monitor));
                    
                }
                                
                // Now finish the build.
                soarProject.finishBuild(isIncremental(), new SubProgressMonitor(monitor, 1));
                
                saveDatamap(soarProject);
            }
            catch (CoreException e)
            {
                SoarCorePlugin.log(e.getStatus());
            }
            finally
            {
                SoarCorePlugin.getDefault().getInternalSoarModel().endModification();
                monitor.done();
            }
        }

		private ClassLoader getProjectClassLoader(IProject project) throws CoreException, JavaModelException {
			IJavaProject javaProject = (IJavaProject)project.getNature(JavaCore.NATURE_ID);
			IClasspathEntry[] resolvedClasspath = javaProject.getResolvedClasspath(true);
			
			List<URL> urls = new ArrayList<URL>();
			for(IClasspathEntry cpe : resolvedClasspath)
			{
				try {
					urls.add(cpe.getPath().toFile().toURI().toURL());
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			URLClassLoader cl = new URLClassLoader(urls.toArray(new URL[0]));
			return cl;
		}

        public boolean isIncremental()
        {
            return delta != null;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            return (isIncremental() ? "Incremental" : "Full") + 
                " build of Soar project " + getProject().getName();
        }
    }
}
