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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.tcl.SoarTclInterface;
import org.jsoar.tcl.SoarTclInterfaceFactory;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.commands.SoarCommandInterpreter;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.IExpandedTclCode;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarFileAgentProxy;
import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.ITclProcedure;
import com.soartech.soar.ide.core.model.SoarModelEvent;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarModelTools;
import com.soartech.soar.ide.core.model.TclExpansionError;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamap;
import com.soartech.soar.ide.core.model.impl.datamap.SoarDatamap;

import tcl.lang.RelocatableTclInterpreter;

/**
 * Implementation of {@link ISoarAgent} interface
 * 
 * @author ray
 */
public class SoarAgent extends AbstractSoarElement implements ISoarAgent
{
    private static final String ROOT_TAG = "soaragent";
    private static final String START_FILE_TAG = "startfile";

    private static final String START_FILE_PATH_ATTR = "path";
    
    private static final String MEMBER_PATH_ATTR = "path";

    private static final String MEMBER_TAG = "member";

    private static final String MEMBERS_TAG = "members";
    
    private static final String EMPTY_AGENT = "<" + ROOT_TAG + "><" + MEMBERS_TAG + "/></" + ROOT_TAG  + ">";
    
    // TODO: Not clear how these are different
    private IFile startFile;
    private IFile file;

    private String name;

    private Set<IResource> members = new LinkedHashSet<IResource>();

    private SoarAgent primary = null;

    // TODO: Unclear what the purpose of the working copy is, not sure why
    // there is a workingCopyCount but only one pointer to a workingCopy
    private SoarAgent workingCopy = null;

    private int workingCopyCount = 0;
    
    private SoarModelTclInterpreter interpreter;
    private SoarDatamap datamap = new SoarDatamap();
    
    private Set<ISoarProduction> productions = new HashSet<ISoarProduction>();
    private Set<ITclProcedure> procedures = new HashSet<ITclProcedure>();
    
    private Map<String, String> expandedSourceMap = new HashMap<String, String>();
    private Map<String, List<String>> fileSourceMap = new HashMap<String, List<String>>();
    
    private Map<String, SoarDatamap> fileDatamapMap = new HashMap<String, SoarDatamap>();
    
//    private List<Datam>
    
    private String previousExpandedSourceKey;
    
    // Keeping a handle on the SoarTclInterface and also this ScheduledExecutorService
    // because the SoarTclInterface can only be disposed by the thread that created it.
    private ScheduledExecutorService tclExecutorService = null;
    private Agent jsoarAgent;
    
    public SoarAgent(SoarProject soarProject, IFile file)
            throws SoarModelException
    {
        super(soarProject);
        
        preloadClasses();
        this.tclExecutorService = Executors.newSingleThreadScheduledExecutor();
        
        this.file = file;
        this.datamap.setAgent(this);
        
        // Create a new jsoar agent
        jsoarAgent = new Agent(soarProject.getProject().getName() + "-agent");

        // Doing this in a helper function because the associated TCL interpreter
        // needs to be created and deleted in the same thread
        createAndRegisterInterpInIsolatedThread();

        // The agent should be ready to initialize at this point
        jsoarAgent.initialize();

        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        this.name = fileName.substring(0, dotIndex != -1 ? dotIndex : fileName
                .length());
        
        System.out.println("Constructed " + this);

        makeConsistent(new NullProgressMonitor());
    }
    
    private SoarAgent(SoarAgent primary)
    {
        super((SoarProject) primary.getParent());

        this.tclExecutorService = primary.tclExecutorService;

        this.primary = primary;
        this.file = primary.file;
        this.name = primary.name;
        this.startFile = primary.startFile;
        this.members = new LinkedHashSet<IResource>(primary.members);
        this.datamap.setAgent(this);
        
        System.out.println("Constructed working copy for " + this);
    }
    
    /**
     * Retrieve the expansion information for a named production
     * 
     * @param name The name of the production to retrieve
     * @return The expansion info, or null if not found
     */
    ExpandedProductionInfo getExpandedProductionBody(String name)
    {
        synchronized (getLock())
        {
            return interpreter != null ? interpreter.getExpandedProductionBody(name) : null;
        }
    }
    
    // TODO: I don't see this being used anywhere?
    boolean fileWasVisited(IFile file)
    {
        synchronized(getLock())
        {
            return interpreter != null ? interpreter.fileWasVisited(file.getLocation()) : false;
        }
    }

    /**
     * Perform check for any overwritten productions and report errors if any are
     * found. 
     */
    void checkForOverwrittenProductions()
    {
        // TODO: Are these null checks here for the right reason
        if(interpreter == null)
        {
            return;
        }
        
        for(OverwrittenProductionInfo info : interpreter.getOverwrittenProductions().values())
        {
            // info.file is a file system path. Try to convert to eclipse resource
            IResource resource = SoarModelTools.getEclipseResource(new Path(info.file));
            if(resource != null && resource instanceof IFile)
            {
                // Lookup the associated soar file
                ISoarFile soarFile = getSoarFile((IFile) resource);
                if(soarFile != null)
                {
                    try
                    {
                        reportOverwrittenProduction(info, soarFile);
                    }
                    catch (SoarModelException e)
                    {
                        SoarCorePlugin.log(e);
                    }
                }
            }
        }
        
    }
    
    /**
     * Find the last named production in a file for this agent. 
     *  
     * @param soarFile The Soar file to search
     * @param name The name of the production
     * @return The last production in the file for this agent with the given name
     *      or null if not found.
     * @throws SoarModelException
     */
    private ISoarProduction getProductionInFile(ISoarFile soarFile, String name) throws SoarModelException
    {
        // First get the file proxy for this agent
        ISoarFileAgentProxy proxy = soarFile.getAgentProxy(this);
        
        if(proxy != null)
        {
            List<ISoarProduction> prods = proxy.getProductions();
            
            // It's more likely that the last production with the given name 
            // is the one we want. If we ever get better location info from the
            // Tcl interpreter we can do better.
            Collections.reverse(prods);
            for(ISoarProduction production : prods)
            {
                if(name.equals(production.getProductionName()))
                {
                    return production;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Report an overwritten production
     * 
     * @param info overwrite information
     * @param soarFile The Soar file to report the error in
     * @throws SoarModelException
     */
    private void reportOverwrittenProduction(OverwrittenProductionInfo info, ISoarFile soarFile) throws SoarModelException
    {
        ISoarProduction production = getProductionInFile(soarFile, info.name);
        
        if(production == null)
        {
            return;
        }
        
        // Try to get a nice name for the other file rather than a file system path
        IResource previous = SoarModelTools.getEclipseResource(new Path(info.previousInstance.file));
        String previousFilePath = previous != null ? previous.getFullPath().toPortableString() :
                                                     info.previousInstance.file;
        
        // Construct the message
        String message = "Overwrites production in '" + previousFilePath + "' (" + info.name + ")" ;
        
        // Create the error marker on the production name
        ISoarSourceRange range = production.getProductionNameRange();
        
        // TODO: Probably should create a new problem marker type for this. For now, reusing
        // the tcl processing problem should work since these problems have the same lifetime.
        SoarModelTools.createErrorMarker(SoarCorePlugin.TCL_PREPROCESSOR_PROBLEM_MARKER_ID,
                                         soarFile.getFile(), 
        								 "", // location
                                         range.getOffset(), range.getLength(), message,
                                         "", //quickFixID
                                         new HashMap<String, Comparable<?>>()); // empty hashmap
    }
    
    /**
     * Performs Tcl preprocessing for this agent if a start file has been specified.
     * 
     * @param monitor progress monitor
     * @throws SoarModelException
     */
    void performTclPreprocessing(final IProgressMonitor monitor)
            throws SoarModelException
    {
        
        //create the tcl interpreter in a special thread that will
        //be reused to dispose the interpreter as well.
        //
        //the JTCL library enforces creating/disposing from the same thread
        synchronized(getLock())
        {
            // We need a new tcl interpreter here
            createAndRegisterInterpInIsolatedThread();
            initCommands(this.jsoarAgent.getInterpreter());
            
            if (startFile != null)
            {
                System.out.println(name + ": Processing Tcl from start file '"
                        + startFile.getFullPath());
                monitor.subTask(name + ": Processing Tcl from start file '"
                        + startFile.getFullPath());
                IPath location = startFile.getLocation();
                try
                {
                    long start = System.currentTimeMillis();
                    TclExpansionError error = interpreter.evaluate(location.toFile(), 
                                                                   new SubProgressMonitor(monitor, 1));
                    System.out.println(name + ": Ran Tcl processing on "
                            + location.toPortableString() + " in "
                            + (System.currentTimeMillis() - start) + " ms");
                    if (error != null)
                    {
                        createTclPreprocessorErrorMarker(error);
                        System.out.println("### " + name + ": ERROR: " + error);
                    }
                }
                catch (RelocatableTclInterpreter.InterruptedException | SoarModelException e)
                {
                    // TODO: I don't think there's really anything to do here, but
                    // think about it.
                    System.out.println("### " + name + ": Tcl processing cancelled by user ###");
                }
            }
        }
    }
    
    private void initCommands(SoarCommandInterpreter jsoarInterp)
    {
        try {
            // Adding some stubs proc's to the JSoar TCL interp to avoid errors
            // for commands that are in CSoar but not JSoar
            // TODO: Seems like this needs to be hooked up to the preferences information
            // (isn't this info stored in preferences)
            jsoarInterp.eval("proc cli { args } { }");
            jsoarInterp.eval("proc indifferent-selection { args } { }");
            jsoarInterp.eval("proc max-chunks { args } { }");
            jsoarInterp.eval("proc svs { args } { }");
            jsoarInterp.eval("proc watch { args } { }");
            
            //add the command for spInternal
            jsoarInterp.addCommand("sp", new SpInternalCommand(this.jsoarAgent, this));
            
        } catch (SoarException e) {
            System.out.println(e.getMessage());
        }
    }
    
    SoarModelTclInterpreter getInterpreter()
    {
        return interpreter;
    }
    
    public Agent getJsoarAgent() 
    {
        return jsoarAgent;
    }

    void addElements(List<ISoarElement> elements)
    {
        synchronized (getLock())
        {
            for(ISoarElement e : elements)
            {
                if(e instanceof ISoarProduction)
                {
                    productions.add((ISoarProduction) e);
                }
                else if(e instanceof ITclProcedure)
                {
                    procedures.add((ITclProcedure) e);
                }
            }
        }
    }
    
    void removeElements(List<ISoarElement> elements)
    {
        synchronized(getLock())
        {
            for(ISoarElement e : elements)
            {
                if(e instanceof ISoarProduction)
                {
                    productions.remove((ISoarProduction) e);
                }
                else if(e instanceof ITclProcedure)
                {
                    procedures.remove((ITclProcedure) e);
                }
            }
        }
    }
    
    private void enforceWorkingCopy()
    {
        if (!isWorkingCopy())
        {
            throw new IllegalStateException(
                    "Operation only permitted on working copy.");
        }
    }

    /**
     * Reverts this working copy back to the state of the primary file it was
     * created from. It is assumed that the lock is held
     */
    private void revertWorkingCopy()
    {
        this.startFile = primary.startFile;
        this.members = new LinkedHashSet<IResource>(primary.members);
    }
    
    private static ISoarFile getSoarFile(IFile file)
    {
        if(file == null)
        {
            return null;
        }
        return (ISoarFile) file.getAdapter(ISoarFile.class);
    }
    
    public Map<String, String> getExpandedSourceMap() {
        return expandedSourceMap;
    }
    
    public Map<String, List<String>> getFileSourceMap() {
        return fileSourceMap;
    }
    
    public Map<String, SoarDatamap> getFileDatamapMap() {
        return fileDatamapMap;
    }
    
    /**
     * 
     * @param file
     * @param clear create a new datamap if true
     * @return
     */
    public SoarDatamap getOrCreateDatamapForFile(IFile file, boolean clear)
    {
        String key = file.getFullPath().toString();
        
        if(!fileDatamapMap.containsKey(key) || clear)
        {
            fileDatamapMap.put(key, new SoarDatamap());
        }
        
        return fileDatamapMap.get(key);
    }
    
    public String getPreviousExpandedSourceKey() {
        return previousExpandedSourceKey;
    }

    public void setPreviousExpandedSourceKey(String previousExpandedSourceKey) {
        this.previousExpandedSourceKey = previousExpandedSourceKey;
    }

    private void notifyStartFileChanged(IFile oldStartFile)
    {
        if(oldStartFile == startFile ||
           (oldStartFile != null && oldStartFile.equals(startFile)))
        {
            return;
        }
        
        // Fire changed event on the start file (if it changed) so that 
        // the UI can update correctly
        ISoarFile startFileSoar = getSoarFile(startFile);
        ISoarFile oldStartFileSoar = getSoarFile(oldStartFile);
        if(startFileSoar != null && oldStartFileSoar != null)
        {
            fireEvent(SoarModelEvent.createChanged(oldStartFileSoar, startFileSoar));
        }
        else if(startFileSoar != null)
        {
            fireEvent(SoarModelEvent.createChanged(startFileSoar));
        }
        else if(oldStartFileSoar != null)
        {
            fireEvent(SoarModelEvent.createChanged(oldStartFileSoar));
        }
    }

    /**
     * Read from the underlying file. It is assumed that the lock is held
     * 
     * @throws SoarModelException
     */
    private void readFromFile() throws SoarModelException
    {
        System.out.println("[SoarAgent] readFromFile() : " + file.getName());
        
        try
        {
            SoarModelTools.deleteMarkers(file, SoarCorePlugin.PROBLEM_MARKER_ID);
            SoarModelTools.deleteMarkers(file, SoarCorePlugin.TASK_MARKER_ID);
        }
        catch (CoreException e)
        {
            throw new SoarModelException(e);
        }

        IFile oldStartFile = startFile;
        startFile = null;
        members.clear();

        // If the file doesn't exist or is empty, then the agent is empty.
        if (!file.exists() || file.getLocation().toFile().length() == 0)
        {
            return;
        }

        InputStream input = null;
        try
        {
            input = file.getContents();
            SAXBuilder builder = new SAXBuilder();
            builder.setValidation(false);
            builder.setIgnoringElementContentWhitespace(true);

            readFromFile(builder.build(input));
            
            notifyStartFileChanged(oldStartFile);
        }
        catch (JDOMException e)
        {
            SoarModelTools.createErrorMarker(file, 0, 0, e.getMessage());
        }
        catch (IOException e)
        {
            throw new SoarModelException(e);
        }
        catch (CoreException e)
        {
            SoarModelTools.createErrorMarker(file, 0, 0, e.getMessage());
        }
        finally
        {
            try
            {
                if(input != null)
                {
                    input.close();
                }
            }
            catch (IOException e)
            {
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void readFromFile(Document doc) throws SoarModelException
    {
        Element root = doc.getRootElement();
        if (root == null)
        {
            SoarModelTools.createErrorMarker(file, 0, 0,
                    "Agent file has no root XML element");
            return;
        }
        
        IContainer container = file.getParent();

        Element startFileElement = root.getChild(START_FILE_TAG);
        String startFilePath = startFileElement != null ? startFileElement
                .getAttributeValue(START_FILE_PATH_ATTR) : null;
        if (startFilePath != null)
        {
            IResource resource = container.findMember(new Path(startFilePath
                    .trim()));
            if (resource instanceof IFile)
            {
                startFile = (IFile) resource;
            }
            else
            {
                SoarModelTools.createErrorMarker(file, 0, 0, "'"
                        + startFilePath + "' does not exist or is not a file");
            }
        }

        Element members = root.getChild(MEMBERS_TAG);

        if (members != null)
        {
            for (Element member : (List<Element>) members
                    .getChildren(MEMBER_TAG))
            {
                readMember(container, member);
            }
        }
    }

    private void readMember(IContainer container, Element member)
            throws SoarModelException
    {
        String path = member.getAttributeValue(MEMBER_PATH_ATTR);
        if (path != null)
        {
            path = path.trim();

            IResource resource = container.findMember(new Path(path));
            if (resource != null)
            {
                System.out.println("[SoarAgent] Resource '" + resource + "' added to agent " + name);
                
                members.add(resource);
                SoarModelTools.getPathRelativeToContainer(file.getParent(),
                        resource);
            }
            else
            {
                SoarModelTools.createErrorMarker(file, 0, 0, "'" + path
                        + "' does not exist");
            }
        }
        else
        {
            SoarModelTools.createErrorMarker(file, 0, 0,
                    "member tag missing path attribute");
        }

    }

    private static Element createMemberElement(IFile file, IResource member)
    {
        Element memberElement = new Element(MEMBER_TAG);

        IPath relativePath = SoarModelTools.getPathRelativeToContainer(file
                .getParent(), member);
        memberElement.setAttribute(MEMBER_PATH_ATTR, relativePath
                .toPortableString());

        return memberElement;
    }
    
    /**
     * Creates the error marker for an error during the global tcl 
     * pre-processing phase
     * 
     * @param error The error
     * @throws SoarModelException
     */
    private void createTclPreprocessorErrorMarker(TclExpansionError error) throws SoarModelException
    {
        // HACK: We shouldn't have to reach into the interpreter to get this state, a SourceLocation
        // should be in the TclExpansionError
        SoarTclInterface sti = (SoarTclInterface) jsoarAgent.getInterpreter();
        SourceLocation sloc = sti.getContext().getSourceLocation();
        
        // Best error message is on the last line of the TclExpansion error...
        BufferedReader bufread = new BufferedReader(new StringReader(error.getMessage()));
        String msg = "Unknown error";
        try
        {
            String line = null;
            while ( (line = bufread.readLine()) != null )
            {
                msg = line;
            }
        }
        catch (IOException e1)
        {
            msg = "Difficulty reading error string";
        }
        
        // See if the file exists in Eclipse...
        IResource resource = null;
        if ( sloc != null && sloc.getFile() != null )
        {
            resource = SoarModelTools.getEclipseResource(new Path(sloc.getFile()));
        }
                
        Map<String, Comparable<?>> attributes = new HashMap<String, Comparable<?>>();
        attributes.put(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
        attributes.put(IMarker.LOCATION, "Tcl pre-processing phase");
        
        if(resource != null && resource.exists())
        {
            // If it's in the workspace, then mark it as usual
            attributes.put(IMarker.LINE_NUMBER, sloc.getLine());
            attributes.put(IMarker.MESSAGE, name + ": " + msg);
        }
        else
        {
            // If it's an external file, we add the marker to the project and
            // expand the error message a bit.
            attributes.put(IMarker.MESSAGE, name + ": In external or unknown file '" + file + ": " + msg);
            resource = this.file;
        }
        
        try
        {
            IMarker marker = resource.createMarker(SoarCorePlugin.TCL_PREPROCESSOR_PROBLEM_MARKER_ID);
            marker.setAttributes(attributes);
        }
        catch (CoreException e)
        {
            throw new SoarModelException(e);
        }
        
        
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#getLock()
     */
    @Override
    public Object getLock()
    {
        // TODO: We really need to document the approach to locking here, there
        // clearly is one but why it exists is unclear. I'm having trouble believing
        // that this type of locking is necessary in typical Eclipse plugins so 
        // I'm wondering if this complexity is self inflicted.
        
        // The working copy and primary share the same lock object. This makes
        // some of the working copy code simpler since we only have to lock
        // this.
        return primary != null ? primary.getLock() : super.getLock();
    }

    @Override
    protected void detach()
    {
        // TODO: What's going on here???
        if (interpreter != null)
        {
            disposeInterpInIsolatedThread();
            interpreter = null;
        }
        
        System.out.println("Detaching the interpreter");
        
        productions.clear();
        procedures.clear();
        datamap.clear();
        members.clear();

        SoarAgent.super.detach();
                
        System.out.println("Finished detaching the interpreter!");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.ISoarAgent#getMembers()
     */
    public Set<IResource> getMembers()
    {
        synchronized (getLock())
        {
            return new LinkedHashSet<IResource>(members);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.ISoarAgent#addFile(org.eclipse.core.resources.IFile)
     */
    public void addFile(IFile file)
    {
        enforceWorkingCopy();

        synchronized (getLock())
        {
            if (contains(file))
            {
                return;
            }

            members.add(file);

            System.out.println("[SoarAgent] File '" + file + "' added to agent " + name);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.ISoarAgent#addFolder(org.eclipse.core.resources.IContainer)
     */
    public void addFolder(IContainer folder)
    {
        enforceWorkingCopy();

        synchronized (getLock())
        {
            if (contains(folder))
            {
                return;
            }

            IPath folderPath = folder.getFullPath();

            // First remove any children of the new folder since they
            // will now be implied.
            Iterator<IResource> it = members.iterator();
            while (it.hasNext())
            {
                IResource resource = it.next();

                if (folderPath.isPrefixOf(resource.getFullPath()))
                {
                    it.remove();
                }
            }

            members.add(folder);

            System.out.println("Folder '" + folder + "' added to agent " + name);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.ISoarAgent#contains(org.eclipse.core.resources.IResource)
     */
    public boolean contains(IResource resource)
    {
        if (resource == null)
        {
            return false;
        }
        synchronized (getLock())
        {
            if (members.contains(resource))
            {
                return true;
            }

            // If it's a file, see if its folder is in the agent
            if (resource instanceof IFile)
            {
                return members.contains(resource.getParent());
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarAgent#containsAnyChildOf(org.eclipse.core.resources.IContainer)
     */
    public boolean containsAllChildrenOf(IContainer container)
    {
        if(container == null || container.getName().startsWith("."))
        {
            return false;
        }
        synchronized (getLock())
        {
            try
            {
                for(IResource resource : container.members())
                {
                    // Ignore non-Soar files.
                    if(resource instanceof IFile)
                    {
                        if(!SoarModelTools.isSoarFile((IFile) resource))
                        {
                            continue;
                        }
                    }
                    
                    if(!contains(resource))
                    {
                        return false;
                    }
                    if(resource instanceof IContainer)
                    {
                        IContainer childContainer = (IContainer) resource;
                        if(!containsAllChildrenOf(childContainer))
                        {
                            return false;
                        }
                    }
                }
            }
            catch (CoreException e)
            {
                return false;
            }
            return true;
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarAgent#containsAnyChildrenOf(org.eclipse.core.resources.IContainer)
     */
    public boolean containsAnyChildrenOf(IContainer container)
    {
        if(container == null || container.getName().startsWith("."))
        {
            return false;
        }
        synchronized (getLock())
        {
            try
            {
                for(IResource resource : container.members())
                {
                    if(contains(resource))
                    {
                        return true;
                    }
                    if(resource instanceof IContainer)
                    {
                        IContainer childContainer = (IContainer) resource;
                        if(containsAnyChildrenOf(childContainer))
                        {
                            return true;
                        }
                    }
                }
            }
            catch (CoreException e)
            {
                return false;
            }
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.ISoarAgent#discardWorkingCopy()
     */
    public void discardWorkingCopy()
    {
        synchronized (getLock())
        {
            if (!isWorkingCopy())
            {
                return;
            }

            --primary.workingCopyCount;
            if (primary.workingCopyCount == 0)
            {
                primary.workingCopy = null;
                detach();
            }
            System.out.println("Removed working copy from '" + primary
                    + "', count=" + primary.workingCopyCount);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.ISoarAgent#getName()
     */
    public String getName()
    {
        return name;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarAgent#getFile()
     */
    public IFile getFile()
    {
        return file;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.ISoarAgent#getStartupFile()
     */
    public IFile getStartFile()
    {
        return startFile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.ISoarAgent#getWorkingCopy()
     */
    public ISoarAgent getWorkingCopy()
    {
        if (isWorkingCopy())
        {
            return primary.getWorkingCopy();
        }
        synchronized (getLock())
        {
            if (workingCopy == null)
            {
                workingCopy = new SoarAgent(this);
            }
            ++workingCopyCount;

            return workingCopy;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.ISoarAgent#isWorkingCopy()
     */
    public boolean isWorkingCopy()
    {
        return primary != null;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarAgent#getPrimary()
     */
    public ISoarAgent getPrimary()
    {
        return primary;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.ISoarAgent#makeConsistent(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void makeConsistent(IProgressMonitor monitor)
            throws SoarModelException
    {
        System.out.println("[SoarAgent] makeConsistent() : " + name);
        
        monitor = SoarModelTools.getSafeMonitor(monitor);
        try
        {
            if (isWorkingCopy())
            {
                monitor.beginTask("Reverting agent working copy", 1);
                revertWorkingCopy();
                
            }
            else
            {
                monitor.beginTask("Reading agent from '" + file.getFullPath() + "'", 1);
                readFromFile();
                fireEvent(SoarModelEvent.createChanged(this));
            }
            monitor.worked(1);
        }
        finally
        {
            monitor.done();
        }
        
        if(getStartFile() == null)
        {
            SoarModelTools.createWarningMarker(file, -1, -1, 
                    "No start file selected for agent '" + name +
                    "'. Tcl expansion will be limited.");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.ISoarAgent#removeFile(org.eclipse.core.resources.IFile)
     */
    public void removeFile(IFile file) throws SoarModelException
    {
        enforceWorkingCopy();

        synchronized (getLock())
        {
            // If the file is already in there individually, then there's
            // nothing
            // more to do
            if (members.remove(file))
            {
                return;
            }

            // Otherwise, if the container is in the agent, we have to remove
            // the container and then add all of the Soar files except this
            // one.
            IContainer parent = file.getParent();
            if (parent == null || !members.remove(parent))
            {
                // The parent folder wasn't there either, so we're done
                return;
            }

            try
            {
                // Re-add all children of the container except the one we're
                // removing
                for (IResource resource : parent.members())
                {
                    if (!resource.equals(file) && resource instanceof IFile
                            && SoarModelTools.isSoarFile((IFile) resource))
                    {
                        members.add(resource);
                    }
                }
            }
            catch (CoreException e)
            {
                throw new SoarModelException(e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.ISoarAgent#removeFolder(org.eclipse.core.resources.IContainer)
     */
    public void removeFolder(IContainer folder)
    {
        enforceWorkingCopy();

        synchronized (getLock())
        {
            members.remove(folder);

            // Remove all children of this folder as well
            IPath folderPath = folder.getFullPath();

            Iterator<IResource> it = members.iterator();
            while (it.hasNext())
            {
                IResource member = it.next();

                if (folderPath.isPrefixOf(member.getFullPath()))
                {
                    it.remove();
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.ISoarAgent#save(org.eclipse.core.runtime.IProgressMonitor)
     */
    public boolean save(IProgressMonitor monitor) throws SoarModelException
    {
        enforceWorkingCopy();

        synchronized (getLock())
        {
            if(!isWorkingCopyChanged())
            {
                System.out.println(file + " is unchanged. Not saving.");
                return false;
            }
            
            Element root = new Element(ROOT_TAG);
            Document doc = new Document(root);

            saveStartFile(file, startFile, root);
            saveMembers(root);

            try
            {
                // First we write the XML to a byte stream
                ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
                XMLOutputter outputter = new XMLOutputter(Format
                        .getPrettyFormat());
                outputter.output(doc, outByteStream);

                // Now we set the file's contents to the byte stream. Kind of
                // weird but that's how the IFile interface works.
                InputStream inByteStream = new ByteArrayInputStream(
                        outByteStream.toByteArray());

                file.setContents(inByteStream, false, true, monitor);
            }
            catch (IOException e)
            {
                throw new SoarModelException(e);
            }
            catch (CoreException e)
            {
                throw new SoarModelException(e);
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarAgent#isWorkingCopyChanged()
     */
    public boolean isWorkingCopyChanged()
    {
        if(!isWorkingCopy())
        {
            return false;
        }
        
        // If the file doesn't exist on disk, it's dirty
        if(!file.exists())
        {
            return true;
        }
        
        // Check if the start file is different
        if(startFile == null && primary.startFile != null ||
           (startFile != null && !startFile.equals(primary.startFile)))
        {
            return true;
        }
        
        // Check if the members have been changed.
        return !members.equals(primary.members);
    }
    
    private void saveMembers(Element root)
    {
        Element membersElement = new Element(MEMBERS_TAG);
        root.addContent(membersElement);

        for (IResource member : members)
        {
            membersElement.addContent(createMemberElement(file, member));
        }
    }

    private static void saveStartFile(IFile file, IFile startFile, Element root)
    {
        if (startFile != null)
        {
            IPath relativePath = SoarModelTools.getPathRelativeToContainer(file
                    .getParent(), startFile);
            Element startFileElement = new Element(START_FILE_TAG);
            startFileElement.setAttribute(START_FILE_PATH_ATTR, relativePath
                    .toPortableString());
            root.addContent(startFileElement);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.ISoarAgent#setStartupFile(com.soartech.soar.ide.core.model.ISoarFile)
     */
    public void setStartFile(IFile file)
    {
        enforceWorkingCopy();

        startFile = file;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.ISoarElement#getContainingResource()
     */
    public IResource getContainingResource()
    {
        return file;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.ISoarElement#getCorrespondingResource()
     */
    public IResource getCorrespondingResource()
    {
        return file;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.ISoarElement#getPath()
     */
    public IPath getPath()
    {
        return file.getFullPath();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarAgent#expandTclString(java.lang.String, java.lang.String, int)
     */
    public IExpandedTclCode expandTclString(String namespace, String input,
            int offset)
    {
        System.out.println("[SoarAgent] expandTclString()");
        
        synchronized (getLock())
        {            
            if (interpreter == null)
            {
                TclExpansionError error = new TclExpansionError(
                        "Project is not configured for Tcl expansion.", offset,
                        0);
                return new ExpandedTclCode(error, null);
            }
            return interpreter.expand(namespace, input, offset);
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarAgent#getDatamap()
     */
    public ISoarDatamap getDatamap()
    {
        return datamap;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarAgent#getAllProcedures()
     */
    public List<ITclProcedure> getAllProcedures()
    {
        synchronized(getLock())
        {
            return new ArrayList<ITclProcedure>(procedures);
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarAgent#getAllProductions()
     */
    public List<ISoarProduction> getAllProductions()
    {
        synchronized(getLock())
        {
            return new ArrayList<ISoarProduction>(productions);
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarAgent#getProcedure(java.lang.String)
     */
    public ITclProcedure getProcedure(String name)
    {
        synchronized (getLock())
        {
            for(ITclProcedure proc : procedures)
            {
                if(name.equals(proc.getProcedureName()))
                {
                    return proc;
                }

                //check for namespaced procs
                //::foo_namespace::proc_name
                String procName = proc.getProcedureName();
                int index = proc.getProcedureName().lastIndexOf("::");
                procName = procName.substring(index + 2);
                if(name.equals(procName))
                {
                    return proc;
                }
            }
            return null;
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarAgent#getProduction(java.lang.String)
     */
    public ISoarProduction getProduction(String name)
    {
        synchronized (getLock())
        {
            for(ISoarProduction prod : productions)
            {
                if(name.equals(prod.getProductionName()))
                {
                    return prod;
                }
            }
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#getAdapter(java.lang.Class)
     */
    @SuppressWarnings({ "rawtypes" })
    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter.equals(IFile.class))
        {
            if (file != null)
            {
                return file;
            }
        }
        return super.getAdapter(adapter);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "SoarAgent " + file;
    }
    
    /**
     * Returns initial contents for a new Soar agent.
     * 
     * @param file The file, probably not created yet, where the agent will be created.
     * @param empty 
     * @return Initial contents as a string
     */
    static String getInitialFileContents(IFile file, boolean empty)
    {
        if(empty)
        {
            return EMPTY_AGENT;
        }
        Element root = new Element(ROOT_TAG);
        Document doc = new Document(root);
        
        // Use existing load.soar file if present
        IContainer parent = file.getParent();
        IResource startFile = parent != null ? parent.findMember("load.soar") : null;
        if(startFile != null && startFile instanceof IFile)
        {
            saveStartFile(file, (IFile) startFile, root); 
        }
        
        final Element membersElement = new Element(MEMBERS_TAG);
        root.addContent(membersElement);
        
        try
        {
            addAllFoldersToInitialContents(file, file.getParent(), membersElement);
        }
        catch (CoreException e)
        {
            membersElement.removeContent();
            SoarCorePlugin.log(e);
        }
        
        try
        {
            ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            outputter.output(doc, outByteStream);
            return outByteStream.toString();
        }
        catch (IOException e)
        {
            SoarCorePlugin.log(e);
        }

        // Never mind then. Just return an empty agent
        return EMPTY_AGENT;
    }
    
    
    private static void addAllFoldersToInitialContents(IFile file, IContainer container, Element membersElement) throws CoreException
    {
        if(container == null)
        {
            return;
        }
        
        membersElement.addContent(createMemberElement(file, container));
        
        for(IResource r : container.members())
        {
            if(r instanceof IContainer)
            {
                addAllFoldersToInitialContents(file, (IContainer) r, membersElement);
            }
        }
    }

    @Override
    public String executeString(String command) 
    {
        return this.interpreter.executeString(command);
    }
    
    /**
     * HACK
     * 
     * This function helps avoid class loader problems in other threads by
     * exercising the creation classes on a temporary agent.
     */
    private void preloadClasses()
    {
        Agent tempAgent = new Agent(null, true);
        SoarTclInterfaceFactory fac = new SoarTclInterfaceFactory();
        SoarCommandInterpreter sti = fac.create(tempAgent);
        tempAgent.setInterpreter(sti);
        tempAgent.setInterpreter(null);
        new SoarModelTclInterpreter(tempAgent.getInterpreter());
    }

    /**
     * This method handles creating the TCL interpreter for the agent and registering
     * it. The tricky part here is that it has to be done in the same thread that 
     * disposes of the interpreter latter.
     * 
     * @return
     */
    private void createAndRegisterInterpInIsolatedThread()
    {
        try
        {
            this.tclExecutorService.submit(new Runnable() {

                @Override
                public void run()
                {
                    // This will cause the previous one to be disposed in JSoar
                    jsoarAgent.setInterpreter(null);

                    // Create a TCL command interpreter for the agent
                    SoarTclInterfaceFactory factory = new SoarTclInterfaceFactory();
                    SoarCommandInterpreter scInterp = factory.create(jsoarAgent);                    
                    jsoarAgent.setInterpreter(scInterp);

                    SoarAgent.this.interpreter = new SoarModelTclInterpreter(jsoarAgent.getInterpreter());
                }
                
            }).get();
        }
        catch (InterruptedException | ExecutionException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * This method handles cleaning up the TCL interpreter in the same thread that 
     * created it earlier.
     * 
     * @param scInterp
     */
    private void disposeInterpInIsolatedThread()
    {
        try
        {
            // The "get" at the end should mean that this method won't
            // return until the dispose has been called and completed
            // so this should be essentially "running in this thread"
            this.tclExecutorService.submit(new Runnable() {

                @Override
                public void run()
                {
                    SoarAgent.this.jsoarAgent.setInterpreter(null);
                }
                
            }).get();
        }
        catch (InterruptedException | ExecutionException e)
        {
            // Logging that it was interrupted, this shouldn't really ever happen
            SoarCorePlugin.log(e);
        }
    }
}
