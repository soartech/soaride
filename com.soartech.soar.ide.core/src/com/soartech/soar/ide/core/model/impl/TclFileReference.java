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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.soartech.soar.ide.core.model.BasicSoarSourceRange;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ITclFileReference;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarModelTools;
import com.soartech.soar.ide.core.model.impl.serialization.ElementMemento;
import com.soartech.soar.ide.core.model.impl.serialization.TclFileReferenceMemento;
import com.soartech.soar.ide.core.tcl.TclAstNode;

/**
 * @author ray
 */
class TclFileReference extends AbstractSourceReferenceElement
    implements ITclFileReference
{
    
    private IPath path;
    private boolean directory;
    
    /**
     * @param parent
     */
    TclFileReference(SoarFileAgentProxy parent, TclAstNode node, IPath path, boolean directory)
    {
        super(parent);
        this.path = path;
        this.directory = directory;
        setSourceRange(new BasicSoarSourceRange(node));
    }
    
    TclFileReference(SoarFileAgentProxy parent, TclFileReferenceMemento memento) throws SoarModelException
    {
        super(parent, memento);
        this.path = new Path(memento.getPath());
        this.directory = memento.isDirectory();
        
        // Range is set in super constructor
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSourceReferenceElement#createMemento()
     */
    @Override
    public ElementMemento createMemento()
    {
        return saveState(new TclFileReferenceMemento());
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSourceReferenceElement#saveState(com.soartech.soar.ide.core.model.impl.serialization.Mementos.Element)
     */
    @Override
    protected ElementMemento saveState(ElementMemento memento)
    {
        TclFileReferenceMemento refMemento = (TclFileReferenceMemento) memento;
        
        refMemento.setDirectory(directory);
        refMemento.setPath(path.toPortableString());
        
        return super.saveState(memento);
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ITclFileReference#isDirectory()
     */
    public boolean isDirectory()
    {
        return directory;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getCorrespondingResource()
     */
    public IResource getCorrespondingResource()
    {
        // No resource for a file reference
        return null;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ITclFileReference#getFileReferencePath()
     */
    public IPath getReferencedLocation()
    {
        return path;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ITclFileReference#getWorkspaceRelativePath()
     */
    public IPath getWorkspacePath()
    {
        IResource resource = getEclipseResource();
        
        return resource != null ? resource.getFullPath() : null;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ITclFileReference#getWorkspaceRelativePath()
     */
    public IPath getProjectRelativePath()
    {
        IResource resource = getEclipseResource();
        
        return resource != null ? resource.getProjectRelativePath() : null;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ITclFileReference#getSoarFile()
     */
    public ISoarFile getReferencedSoarFile() throws SoarModelException
    {
        if(isDirectory() || path == null)
        {
            return null;
        }
        IResource resource = getEclipseResource();
        if(resource == null || !(resource instanceof IFile))
        {
            return null;
        }
        
        return this.getSoarModel().getFile((IFile) resource);
    }


    private IResource getEclipseResource()
    {
        return SoarModelTools.getEclipseResource(path);
    }
}
