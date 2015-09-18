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
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
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
import com.soartech.soar.ide.core.model.ITclProcedure;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarModelTools;
import com.soartech.soar.ide.core.model.SoarProblem;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamap;
import com.soartech.soar.ide.core.model.impl.serialization.ElementMemento;
import com.soartech.soar.ide.core.model.impl.serialization.FileAgentProxyMemento;
import com.soartech.soar.ide.core.tcl.TclAstNode;

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
            
            //get the list of this agent's sourced files from jsoar
            //these should always have a "/" as the path separator
            Collection<String> sourcedFiles = agent.getInterpreter().getJSoarInterpreter().getSourcedFiles();
            
            //get the current filename and format it's path to use backslashes since that's what jsoar is giving us
            String filename = getFile().getFile().getLocation().makeAbsolute().toString();
            
            // Only report this warning if a start file has actually be specified.
            if(agent.getStartFile() != null && !sourcedFiles.contains(filename))
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
            
            // Tell the agent about new productions and stuff
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
            elements.add(new SoarProduction(this, reporter, commandNode));
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
        else if(name.equals("define-heuristic"))
        {
//            elements.add(new TclDefineHeuristicCommand(this, commandNode));
        }
        else if(isImprobableCommandName(name))
        {
            // TODO: Workspace and project preference for disabling this warning
            reporter.report(SoarProblem.createWarning("'" + name + "' is an improbable Tcl command. Possible syntax error.", 
                    nameWord.getStart(), nameWord.getLength()));
        }
//        else {
//            ExpandedProductionInfo info = agent.getExpandedProductionBody(name);
//            String namespace = info != null ? info.namespace : "::";
//            
//            TclAstNode endWord = words.get(words.size() - 1);
//            String input = "\"" + buffer.getText(nameWord.getStart(), (endWord.getStart() + endWord.getLength()) - nameWord.getStart()) + "\"";
//            agent.expandTclString(namespace, input, 0);
//        }
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
