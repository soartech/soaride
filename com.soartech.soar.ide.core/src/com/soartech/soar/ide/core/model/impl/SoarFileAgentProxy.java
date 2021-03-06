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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarBuffer;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarFileAgentProxy;
import com.soartech.soar.ide.core.model.ISoarProblemReporter;
import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.ITclProcedure;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarModelTools;
import com.soartech.soar.ide.core.model.SoarProblem;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamap;
import com.soartech.soar.ide.core.model.datamap.SoarDatamapAdditionResult;
import com.soartech.soar.ide.core.model.impl.datamap.SoarDatamap;
import com.soartech.soar.ide.core.model.impl.serialization.ElementMemento;
import com.soartech.soar.ide.core.model.impl.serialization.FileAgentProxyMemento;
import com.soartech.soar.ide.core.tcl.TclAstNode;
import com.soartech.soar.ide.core.tcl.TclParser;

import edu.umich.soar.editor.editors.datamap.Datamap;
import edu.umich.soar.editor.editors.datamap.ValidateDatamapAction;


/**
 * @author ray
 */
public class SoarFileAgentProxy extends AbstractSoarElement implements ISoarFileAgentProxy
{
    private SoarFile file;
    private SoarAgent agent;
    private TclAstNode root;
    private boolean errors;
    private boolean warnings;
    
    public SoarFileAgentProxy(SoarFile file, SoarAgent agent)
    {
        super(file);
        
        this.file = file;
        this.agent = agent;
    }
    
    public SoarFileAgentProxy(SoarFile file, FileAgentProxyMemento memento) throws SoarModelException
    {
        super(file, memento);
        
        this.file = file;
        
        final IPath agentPath = Path.fromPortableString(memento.getAgentPath());
        IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(agentPath);
        if(!(resource instanceof IFile))
        {
            throw new SoarModelException("Cannot find agent resource: " + agentPath);
        }
        this.agent = (SoarAgent) this.file.getSoarProject().getAgent((IFile) resource);
        if(this.agent == null)
        {
            throw new SoarModelException("Cannot find agent: " + resource);
        }
        
        agent.addElements(getChildren());
    }

    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#detach()
     */
    @Override
    protected void detach()
    {
        try
        {
            agent.removeElements(getChildren());
        }
        catch (SoarModelException e)
        {
            SoarCorePlugin.log(e);
        }
        super.detach();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#shouldFireEvents()
     */
    @Override
    protected boolean shouldFireEvents()
    {
        return !isWorkingCopy();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#createMemento()
     */
    @Override
    public ElementMemento createMemento()
    {
        return saveState(new FileAgentProxyMemento());
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#saveState(com.soartech.soar.ide.core.model.impl.serialization.Mementos.Element)
     */
    @Override
    protected ElementMemento saveState(ElementMemento memento)
    {
        super.saveState(memento);
        
        FileAgentProxyMemento proxyMemento = (FileAgentProxyMemento) memento;
        proxyMemento.setAgentPath(agent.getPath().toPortableString());
        
        return memento;
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getContainingResource()
     */
    public IResource getContainingResource()
    {
        return file.getContainingResource();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getCorrespondingResource()
     */
    public IResource getCorrespondingResource()
    {
        return file.getCorrespondingResource();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getPath()
     */
    public IPath getPath()
    {
        return file.getPath();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarFileAgentProxy#getAgent()
     */
    public ISoarAgent getAgent()
    {
        return agent;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarFileAgentProxy#getFile()
     */
    public ISoarFile getFile()
    {
        return file;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarFileAgentProxy#getProcedures()
     */
    public List<ITclProcedure> getProcedures()
    {
        synchronized(getLock())
        {
            List<ITclProcedure> procedures = new ArrayList<ITclProcedure>();
            
            try {
                List<ISoarElement> elements = this.getChildren();
                
                for(ISoarElement e:elements)
                {
                    if(e instanceof ITclProcedure)
                    {
                        procedures.add((ITclProcedure) e);
                    }
                }
            } catch (SoarModelException e) {
                // TODO: Make sure that this is appropriate
                //nothing
            }
            return procedures;
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarFileAgentProxy#getProductions()
     */
    public List<ISoarProduction> getProductions()
    {
        synchronized(getLock())
        {
            List<ISoarProduction> productions = new ArrayList<ISoarProduction>();
            
            try {
                List<ISoarElement> elements = this.getChildren();
                
                for(ISoarElement e:elements)
                {
                    if(e instanceof ISoarProduction)
                    {
                        productions.add((ISoarProduction) e);
                    }
                }
            } catch (SoarModelException e) {
                // TODO: Make sure that this is appropriate
                //nothing
            }
            
            return productions;
        }
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarFile#makeConsistent(org.eclipse.core.runtime.IProgressMonitor, com.soartech.soar.ide.core.model.ISoarProblemReporter)
     */
    public void makeConsistent(IProgressMonitor monitor, ISoarProblemReporter reporter, TclAstNode root) throws SoarModelException
    {
        monitor = SoarModelTools.getSafeMonitor(monitor);
        
        SoarModelTools.checkForCancellation(monitor);

        synchronized(getLock())
        {
            if(!isWorkingCopy())
            {
                agent.removeElements(getChildren());
            }
            clearChildren();
            
            this.root = root;
            
            // TODO: Null pointer exception below!!!!
            //get the list of this agent's sourced files from jsoar
            //these should always have a "/" as the path separator
            Collection<String> sourcedFiles = agent.getInterpreter().getSourcedFiles();
            
            // TODO: Get sourced paths should probably by absolute/canonical
            HashSet<String> sourcedPaths = new HashSet<String>();
            for ( String spath : sourcedFiles )
            {
                File file = new File(spath);
                try
                {
                    sourcedPaths.add(file.getCanonicalPath());
                }
                catch (IOException e)
                {
                    // There isn't much we can do here except log it
                    SoarCorePlugin.log(e);
                }
            }
            
            //get the current filename and format it's path to use backslashes since that's what jsoar is giving us
            String filename = getFile().getFile().getLocation().makeAbsolute().toOSString();
            
            // Only report this warning if a start file has actually be specified.
            if(agent.getStartFile() != null && !sourcedPaths.contains(filename))
            {
                // Here were taking advantage of the fact that the reporter ignores
                // duplicate problems.
                reporter.report(SoarProblem.createWarning(
                        agent.getName() + ": file does not appear reachable from selected start file", 
                        0, 0));
            }

            processParseResult(monitor, reporter, agent.getDatamap());
            
            updateChildProblems(reporter);
        }
        monitor.worked(1);    
    }
    
    boolean isWorkingCopy()
    {
        return file != null && file.isWorkingCopy();
    }
    
    ISoarBuffer getBuffer() throws SoarModelException
    {
        return file.getBuffer();
    }
    
    @SuppressWarnings("unchecked")
    private void processParseResult(IProgressMonitor monitor, ISoarProblemReporter reporter, ISoarDatamap datamap) throws SoarModelException
    {
        System.out.println("[SoarFileAgentProxy] processParseResult() for " + file.getFile().getName());
        
        try
        {
            datamap.beginModification();
            
            List<AbstractSoarElement> elements = new ArrayList<AbstractSoarElement>();
    
            elements.addAll(TclFileReferenceBuilder.findReferences(this, root, reporter));
            
            for(TclAstNode child : root.getChildren())
            {
                SoarModelTools.checkForCancellation(monitor);

                if(child.getType() == TclAstNode.COMMAND)
                {
                    processTclCommand(child, elements, reporter);
                }
            }
            
            List<AbstractSoarElement> unexpandedSoarElements = new ArrayList<AbstractSoarElement>();
            
            ISoarSourceRange elemSourceRange;
            
            //construct a string buffer of the expanded elements
            String expandedSource = "";
            
            List<GenericCommand> expandedGenericCommands = new ArrayList<GenericCommand>();
            
            for(AbstractSoarElement elem : elements)
            {
                if(elem instanceof SoarProduction)
                {
//                    productions.add((ISoarProduction) e);
                    SoarProduction sp = (SoarProduction) elem;
                    expandedSource += sp.getExpandedSource();
                    expandedSource += "\n";
                }
                else if(elem instanceof ITclProcedure)
                {
//                    procedures.add((ITclProcedure) e);
                    
                } 
                else if(elem instanceof GenericCommand)
                {
                    GenericCommand com = (GenericCommand) elem;
                    
//                    expandedSource += com.getExpandedSource();
//                    expandedSource += "\n";
                    expandedGenericCommands.add(com);
                    
                }
            }
            
            //get the datamap for this file
            SoarDatamap dm = agent.getOrCreateDatamapForFile(file.getFile(), true);
            
            //add the expanded source for the sp's to the file datamap
            addSourceToFileDatamap(expandedSource, null, dm, monitor, reporter, elements);
            
            //add the expanded source for the generic commands to the file datamap
            for(GenericCommand gc : expandedGenericCommands)
            {
                addSourceToFileDatamap(gc.getExpandedSource(), gc.getSourceRange(), dm, monitor, reporter, elements);
            }

            //delete any datamap problem markers
            try {
                SoarModelTools.deleteMarkers(file.getFile(), SoarCorePlugin.DATAMAP_PROBLEM_MARKER_ID);
            } catch (CoreException e) {
                e.printStackTrace();
            }
            
            //validate against the dynamic file datamap we just created
            Set<IResource> agentFiles = agent.getMembers();
            for(IResource res : agentFiles)
            {
                if (res instanceof IFile)
                {
                    IFile f = (IFile) res;
                    
                    System.out.println("[SoarReconcilingStrategy] checking member file " + f.getName());
                    
                    String extension = f.getFileExtension();
                    if(extension.equals("dm"))
                    {
                        Datamap staticDatamap = Datamap.read(f);
                        
                        ValidateDatamapAction validateDatamap = new ValidateDatamapAction(staticDatamap, agent.getOrCreateDatamapForFile(file.getFile(), false), file.getSource());
                        validateDatamap.run();
                    }
                }
            }
            
            
            //tell the agent about new productions and stuff
            //"elements" is the array of unexpanded code
            //do we want to use the expanded code instead??
            if(!isWorkingCopy())
            {
                agent.addElements((List) elements);
            }
            addChildren(elements);
        }
        finally
        {
            datamap.endModification();
        }
        
    }
    
    private void addSourceToFileDatamap(String expandedSource, ISoarSourceRange tclSourceRange, SoarDatamap dm, IProgressMonitor monitor, ISoarProblemReporter reporter, List<AbstractSoarElement> elements) throws SoarModelException
    {
        if(expandedSource == null)
        {
            return;
        }
        
        // Get the contents of the source
        char[] contents = expandedSource.toCharArray();
        
        // re-parse the tcl commands, this time with their expanded values
        TclParser parser = new TclParser();
        parser.setInput(contents, 0, contents.length);
        TclAstNode expandedRoot = parser.parse();

        //get the commands in the expanded tcl graph and make a datamap out of them
        for(TclAstNode child : expandedRoot.getChildren())
        {
            SoarModelTools.checkForCancellation(monitor);
            if(child.getType() == TclAstNode.COMMAND)
            {
                //get the name and words that make up this production
                List<TclAstNode> words = child.getWordChildren();
                if(words.isEmpty())
                {
                    return;
                }
                TclAstNode nameWord = words.get(0);
                
                String name = "";
                for(int i = nameWord.getStart(); i < nameWord.getStart() + nameWord.getLength(); i++)
                {
                    name += contents[i];
                }
                
                //evaluate every sp we find
                if(name.equals("sp"))
                {
                    //create a SoarProduction object from the TclAstNode
                    ISoarProduction p = new SoarProduction2(this, tclSourceRange, reporter, child, expandedSource, elements);
                    
                    //add this SoarProduction to the datamap 
                    SoarDatamapAdditionResult result = dm.addProduction(p);
                }
            }
        }
        
    }
    
    private void processTclCommand(TclAstNode commandNode, 
                                   List<AbstractSoarElement> elements,
                                   ISoarProblemReporter reporter) throws SoarModelException
    {
        assert commandNode.getType() == TclAstNode.COMMAND;
        
        List<TclAstNode> words = commandNode.getWordChildren();
        if(words.isEmpty())
        {
            return;
        }
        TclAstNode nameWord = words.get(0);
        ISoarBuffer buffer = file.getBuffer();
        String name = buffer.getText(nameWord.getStart(), nameWord.getLength());
        
        if(name.equals("sp"))
        {
            SoarProduction p = new SoarProduction(this, reporter, commandNode, null);
            elements.add(p);
            
//            SoarDatamap dm = agent.getOrCreateDatamapForFile(file.getFile());
//            dm.addProduction(p);
//            p.getExpandedSource();
            
        }
        else if(name.equals("proc"))
        {
            elements.add(new TclProcedure(this, reporter, commandNode));
        }
        else if(name.equals("set"))
        {
            //run the set command in the interpreter to add the variable
            ExpandedProductionInfo info = agent.getExpandedProductionBody(name);
            String namespace = info != null ? info.namespace : "::";
            
            TclAstNode endWord = words.get(words.size() - 1);
            String input = "\"" + buffer.getText(nameWord.getStart(), (endWord.getStart() + endWord.getLength()) - nameWord.getStart()) + "\"";
            agent.expandTclString(namespace, input, 0);
        }
        else if(isImprobableCommandName(name))
        {
            // TODO: Workspace and project preference for disabling this warning
            reporter.report(SoarProblem.createWarning("'" + name + "' is an improbable Tcl command. Possible syntax error.", 
                    nameWord.getStart(), nameWord.getLength()));
        }
        else if(isIngorableCommandName(name))
        {
            //do nothing
        }
        else {

            //make a new source command
            GenericCommand command = new GenericCommand(this,  reporter, commandNode);
            elements.add(command);
        }
    }
    
    private boolean isIngorableCommandName(String name)
    {
        if(name.equals("source"))
        {
            return true;
        }
        
        return false;
    }

    /**
     * Returns true if a command name is improbable in typical Tcl code. Since
     * a command name can be pretty much anything in Tcl, it's tough to do real
     * error checking, but it's not that tough to guess that a command name 
     * just isn't right. Most command names start with a normal identifier
     * character, a dollar sign (e.g., Tk widgets) or double colons for global
     * namespace commands.
     * 
     * @param name Name of the command to test
     * @return true if name is probably not a Tcl command name
     */
    private static boolean isImprobableCommandName(String name)
    {
        char c = name.charAt(0);
        return !Character.isLetterOrDigit(c) &&
               c != '$' &&
               c != '_' &&
               c != '.' &&
               !name.startsWith("::");
    }
    
    
    /**
     * @return the errors
     */
    public boolean hasErrors()
    {
        return errors;
    }

    /**
     * @return the warnings
     */
    public boolean hasWarnings()
    {
        return warnings;
    }

    public void updateChildProblems(ISoarProblemReporter reporter) throws SoarModelException
    {
        errors = false;
        warnings = false;
        
        // reset the cached problem state for all children
        for(ISoarElement child : getChildren())
        {
            if(child instanceof AbstractSourceReferenceElement)
            {
                ((AbstractSourceReferenceElement) child).resetProblems();
            }
        }
        
        for(SoarProblem problem : reporter.getProblems())
        {
            for(ISoarElement child : getChildren())
            {
                if(child instanceof AbstractSourceReferenceElement)
                {
                    ((AbstractSourceReferenceElement) child).updateProblems(problem);
                }
                errors |= child.hasErrors();
                warnings |= child.hasWarnings();
            }
        }
    }

}
