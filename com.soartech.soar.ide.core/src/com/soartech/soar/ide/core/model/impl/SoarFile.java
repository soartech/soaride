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
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import com.soartech.soar.ide.core.model.BasicSoarSourceRange;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarBuffer;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarFileAgentProxy;
import com.soartech.soar.ide.core.model.ISoarProblemReporter;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.ISoarWorkingCopyOwner;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarModelTools;
import com.soartech.soar.ide.core.model.SoarProblem;
import com.soartech.soar.ide.core.model.impl.serialization.ElementMemento;
import com.soartech.soar.ide.core.model.impl.serialization.FileMemento;
import com.soartech.soar.ide.core.tcl.TclAstNode;
import com.soartech.soar.ide.core.tcl.TclParser;
import com.soartech.soar.ide.core.tcl.TclParserError;

/**
 * Implementation of ISoarFile interface
 * 
 * @author ray
 */
public class SoarFile extends AbstractSoarOpenable implements ISoarFile
{
    /**
     * The underlying Eclipse IFile
     */
    private IFile file;
    
    /**
     * If this is a working copy, primary is the SoarFile it is a working copy
     * of. Otherwise, it is null. 
     */
    private SoarFile primary;
    
    /**
     * If this is a working copy, the owner that created it 
     */
    private ISoarWorkingCopyOwner workingCopyOwner;
    /**
     * The currently active working copy, or null for none
     */
    private SoarFile workingCopy;
    
    /**
     * The reference count on the working copy. 
     */
    private int workingCopyCount;
    
    /**
     * Root of the Tcl parse tree for this file 
     */
    private TclAstNode root;
    
    private boolean errors;
    private boolean warnings;
    
    /**
     * @param parent The owning project
     * @param file The Eclipse file
     */
    public SoarFile(SoarProject parent, IFile file)
    {
        super(parent);
        this.file = file;
        System.out.println("SoarFile attached to file " + file.getFullPath()); 
    }
    
    public SoarFile(SoarProject parent, IFile file, FileMemento memento) throws SoarModelException
    {
        super(parent, memento);
        
        this.file = file;
        updateChildProblems(new SoarResourceProblemReporter(file));
        System.out.println("SoarFile memento attached to file " + file.getFullPath()); 
    }
    
    /**
     * Construct a working copy
     * 
     * @param primary The primary file
     * @param wco The working copy owner
     * @throws SoarModelException 
     */
    private SoarFile(SoarFile primary, ISoarWorkingCopyOwner wco) throws SoarModelException
    {
        super((SoarProject) primary.getParent());
        
        this.file = primary.file;
        this.primary = primary;
        this.workingCopyOwner = wco;
        
        
        assert getParent() == primary.getParent();
        assert !getParent().getChildren().contains(this);
    }
    
    @Override
    protected void detach()
    {
        synchronized (getLock())
        {
            System.out.println("SoarFile detached from file " + file.getFullPath());
            // If we have an active working copy we have to detach it as well to 
            // ensure that its buffer gets cleaned up and that it doesn't try to
            // do anything bad when its editor is closed automatically by
            // Eclipse.
            if(workingCopy != null)
            {
                workingCopy.detach();
            }
            
            super.detach();
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#hasErrors()
     */
    @Override
    public boolean hasErrors()
    {
        return errors;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#hasWarnings()
     */
    @Override
    public boolean hasWarnings()
    {
        return warnings;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#createMemento()
     */
    @Override
    public ElementMemento createMemento()
    {
        return saveState(new FileMemento());
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#saveState(com.soartech.soar.ide.core.model.impl.serialization.Mementos.Element)
     */
    @Override
    protected ElementMemento saveState(ElementMemento memento)
    {
        super.saveState(memento);
        
        FileMemento fileMemento = (FileMemento) memento;
        fileMemento.setPath(file.getFullPath().toPortableString());
        
        return memento;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarFile#getFile()
     */
    public IFile getFile()
    {
        return file;
    }

	/* (non-Javadoc)
	 * @see com.soartech.soar.ide.core.model.ISoarFile#getTclSyntaxTree()
	 */
	public TclAstNode getTclSyntaxTree() 
	{
		return root;
	}

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarFile#getPrimaryAgentProxy()
     */
    public ISoarFileAgentProxy getPrimaryAgentProxy() throws SoarModelException
    {
        synchronized (getLock())
        {
            if(!hasChildren())
            {
                return null;
            }
            
            return (ISoarFileAgentProxy) getChildren().get(0);
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarFile#getAgentProxies()
     */
    public List<ISoarFileAgentProxy> getAgentProxies() throws SoarModelException
    {
        List<ISoarFileAgentProxy> proxies = new ArrayList<ISoarFileAgentProxy>();
        synchronized(getLock())
        {
            for(ISoarElement child : getChildren())
            {
                proxies.add((ISoarFileAgentProxy) child);
            }
        }
        return proxies;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarFile#getAgentProxy(com.soartech.soar.ide.core.model.ISoarAgent)
     */
    public ISoarFileAgentProxy getAgentProxy(ISoarAgent agent) throws SoarModelException
    {
        for(ISoarFileAgentProxy p : getAgentProxies())
        {
            if(p.getAgent() == agent)
            {
                return p;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getContainingResource()
     */
    public IResource getContainingResource()
    {
        return file;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getCorrespondingResource()
     */
    public IResource getCorrespondingResource()
    {
        return file;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getPath()
     */
    public IPath getPath()
    {
        return file.getFullPath();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarFile#makeConsistent(org.eclipse.core.runtime.IProgressMonitor, com.soartech.soar.ide.core.model.ISoarProblemReporter)
     */
    public void makeConsistent(IProgressMonitor monitor, ISoarProblemReporter reporter) throws SoarModelException
    {
        monitor = SoarModelTools.getSafeMonitor(monitor);
        
        SoarModelTools.checkForCancellation(monitor);

        reporter.clear();
        
        synchronized(getLock())
        {
            // Ignore detached files.
            if ( file == null ) 
            {
                return;
            }
            
            List<SoarFileAgentProxy> proxies = updateProxies();

            // If there are no proxies, then this file is not in any agents. 
            // Skip it.
            if(proxies.isEmpty())
            {
                return;
            }
            
            if(!isWorkingCopy())
            {
                // Force reset of buffer so that file will be re-read. We don't 
                // use getBuffer().close() because getBuffer() will cause the file
                // to be read even if the buffer isn't currently open.
                // The next call to getBuffer() will reopen the object.
                close();
            }
            
            monitor.subTask("Processing: " + file.getFullPath());
            System.out.println("Parsing: " + file.getFullPath() + 
                                (isWorkingCopy() ? " (working copy)" : ""));
            
            // Get the contents of the file
            ISoarBuffer buffer = getBuffer();
            
            // Parse the file as raw tcl
            TclParser parser = new TclParser();
            parser.setInput(buffer.getCharacters(), 0, buffer.getLength());
            this.root = parser.parse();
            
            processParseErrors(reporter, parser);
            
            // Now process the file in the context of each agent this file is
            // part of.
            for(SoarFileAgentProxy proxy : proxies)
            {
                proxy.makeConsistent(monitor, reporter, root);
            }
            
            reporter.apply();
            
            updateChildProblems(reporter);
        }
        monitor.worked(1);    
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarOpenable#makeConsistent(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void makeConsistent(IProgressMonitor monitor) throws SoarModelException
    {
        makeConsistent(monitor, new SoarResourceProblemReporter(file));
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarOpenable#hasBuffer()
     */
    @Override
    protected boolean hasBuffer()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarOpenable#openBuffer(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected ISoarBuffer openBuffer(IProgressMonitor monitor) throws SoarModelException
    {
        monitor = SoarModelTools.getSafeMonitor(monitor);
        
        SoarBufferManager manager = getInternalSoarModel().getBufferManager();
        ISoarBuffer buffer = isWorkingCopy() ? workingCopyOwner.createBuffer(this) : 
                                               manager.createBuffer(this);
        
        // If it's not a working copy, fill the buffer with the contents of the
        // file. Otherwise, the buffer will already be filled by the working
        // copy owner that created it.
        if(!isWorkingCopy())
        {
            buffer.setContents(SoarModelTools.readFileAsCharArray(file));
            
            // Working copy buffers are not managed by the manager. Since 
            // they're being edited we want them to always remain in memory.
            manager.addBuffer(buffer);
        }
        
        return buffer;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#shouldFireEvents()
     */
    @Override
    protected boolean shouldFireEvents()
    {
        // Working copies do not fire events when they are modified. This
        // keeps a document in the editor from causing lots of changes in
        // the rest of the system for minor, usually transient, modifications.
        return !isWorkingCopy();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarSourceReference#getSource()
     */
    public String getSource() throws SoarModelException
    {
        ISoarBuffer buffer = getBuffer();
        return buffer != null ? buffer.getContents() : "";
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarSourceReference#getSourceRange()
     */
    public ISoarSourceRange getSourceRange() throws SoarModelException
    {
        ISoarBuffer buffer = getBuffer();
        if(buffer == null)
        {
            return new BasicSoarSourceRange(0, 0);
        }
        
        return new BasicSoarSourceRange(0, buffer.getLength());
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#getAdapter(java.lang.Class)
     */
    @SuppressWarnings({ "rawtypes" })
    @Override
    public Object getAdapter(Class adapter)
    {
        if(adapter.equals(IFile.class))
        {
            if(file != null)
            {
                return file;
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
        return "SoarFile " + file;
    }

    private void processParseErrors(ISoarProblemReporter reporter, TclParser parser) throws SoarModelException
    {
        for(TclParserError e : parser.getErrors())
        {
            System.err.println(file.getFullPath() + ": " + e);
            reporter.report(SoarProblem.createError(e.getMessage(), e.getStart(), e.getLength()));
        }        
    }
        
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarFile#createWorkingCopy(com.soartech.soar.ide.core.model.ISoarWorkingCopyOwner)
     */
    public ISoarFile createWorkingCopy(ISoarWorkingCopyOwner owner) throws SoarModelException
    {
        synchronized(getLock())
        {
            if(workingCopy != null)
            {
                ++workingCopyCount;
                return workingCopy;
            }
            
            workingCopy = new SoarFile(this, owner);
            ++workingCopyCount;
            
            System.out.println("Created working copy from '" + this + "', count=" + workingCopyCount);
            
            return workingCopy;
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarFile#discardWorkingCopy()
     */
    public void discardWorkingCopy()
    {
        synchronized(getLock())
        {
            if(!isWorkingCopy())
            {
                return;
            }
            
            synchronized (primary.getLock())
            {
                --primary.workingCopyCount;
                if(primary.workingCopyCount == 0)
                {
                    primary.workingCopy = null;
                }
                System.out.println("Removed working copy from '" + primary + "', count=" + primary.workingCopyCount);
            }
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarFile#getPrimaryFile()
     */
    public ISoarFile getPrimaryFile()
    {
        return isWorkingCopy() ? primary : this;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarFile#isWorkingCopy()
     */
    public boolean isWorkingCopy()
    {
        return primary != null;
    }
    
    private void updateChildProblems(ISoarProblemReporter reporter) throws SoarModelException
    {
        errors = false;
        warnings = false;
        
        for(ISoarElement element : getChildren())
        {
            SoarFileAgentProxy ap = (SoarFileAgentProxy) element;
            
            ap.updateChildProblems(reporter);
            
            errors |= ap.hasErrors();
            warnings |= ap.hasWarnings();
        }
    }
    
    /**
     * Get a proxy for the given agent. If a proxy doesn't exist yet, create
     * and add a new one.
     * 
     * @param agent The agent to get a proxy for
     * @return The proxy
     * @throws SoarModelException
     */
    private SoarFileAgentProxy getOrCreateAgentProxy(SoarAgent agent) throws SoarModelException
    {
        for(ISoarElement element : getChildren())
        {
            SoarFileAgentProxy proxy = (SoarFileAgentProxy) element;
            
            if(proxy.getAgent() == agent)
            {
                return proxy;
            }
        }
        SoarFileAgentProxy proxy = new SoarFileAgentProxy(this, agent);
        addChild(proxy);
        return proxy;
    }
    
    @SuppressWarnings("unchecked")
    private List<SoarFileAgentProxy> updateProxies() throws SoarModelException
    {
        // Use a raw list here so we can pass to addChildren() below and also
        // return it without Java complaining too much.
        List newProxies = new ArrayList<SoarFileAgentProxy>();
        
        // First get the new set of proxies for this file
        for(ISoarAgent agent : getSoarProject().getAgents())
        {
            if(agent.contains(file))
            {
                SoarFileAgentProxy proxy = getOrCreateAgentProxy((SoarAgent) agent);
                newProxies.add(proxy);
            }
        }
        
        // Now calculate proxies that are no longer in the file and remove them
        // from the child list
        List<AbstractSoarElement> removedProxies = new ArrayList<AbstractSoarElement>();
        for(ISoarElement child : getChildren())
        {
            if(!newProxies.contains(child))
            {
                removedProxies.add((AbstractSoarElement) child);
            }
        }
        removeChildren(removedProxies);
        
        replaceChildrenFast(newProxies);
        
        return newProxies;
    }
}
