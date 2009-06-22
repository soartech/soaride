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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarModelTools;
import com.soartech.soar.ide.core.model.impl.SoarFile;
import com.soartech.soar.ide.core.model.impl.SoarProject;
import com.soartech.soar.ide.core.model.impl.serialization.Mementos;

class SoarFileVistor implements IResourceVisitor, IResourceDeltaVisitor
{
    /**
     * 
     */
    private final SoarBuilder builder;
    private IProgressMonitor monitor;
    
    /**
     * @param monitor
     * @param builder TODO
     */
    public SoarFileVistor(SoarBuilder builder, IProgressMonitor monitor)
    {
        this.builder = builder;
        this.monitor = SoarModelTools.getSafeMonitor(monitor);
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.resources.IResourceVisitor#visit(org.eclipse.core.resources.IResource)
     */
    public boolean visit(IResource resource) throws CoreException
    {
        // This is the visit method called during the full builds
        
        SoarModelTools.checkForCancellation(monitor);
        
        SoarFile soarFile = this.builder.getSoarFile(resource);
        if(soarFile == null)
        {
            return true;
        }
        
        makeFileConsistent(soarFile, monitor);
        
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
     */
    public boolean visit(IResourceDelta delta) throws CoreException
    {
        // This is the visitor method called during incremental builds
        
        SoarModelTools.checkForCancellation(monitor);
        
        SoarProject project = this.builder.getSoarProject();
        SoarFile soarFile = this.builder.getSoarFile(delta.getResource());
        if(soarFile == null)
        {
            return true;
        }
        
        int kind = delta.getKind();
        if(kind == IResourceDelta.ADDED)
        {
            System.out.println("Added file " + soarFile.getFile().getFullPath());
            makeFileConsistent(soarFile, monitor);
        }
        else if (kind == IResourceDelta.CHANGED)
        {
            System.out.println("Changed file " + soarFile.getFile().getFullPath());
            makeFileConsistent(soarFile, monitor);
        }
        else if(kind == IResourceDelta.REMOVED)
        {
            System.out.println("Removed file " + soarFile.getFile().getFullPath());
            SoarBuilder.deleteOutputFile(soarFile.getFile());
            project.removeSoarFile(soarFile);
            monitor.worked(1);
        }
        
        return true;
    }
    
    private void makeFileConsistent(SoarFile soarFile, IProgressMonitor monitor) throws SoarModelException
    {
        soarFile.makeConsistent(monitor);
        File outFile = SoarBuilder.createOutputDirectoryForFile(soarFile.getFile());
        Mementos.serialize(soarFile.createMemento(), 
                                      new File(outFile.getPath() + ".ser"));
        
        monitor.worked(1);
    }
    
}