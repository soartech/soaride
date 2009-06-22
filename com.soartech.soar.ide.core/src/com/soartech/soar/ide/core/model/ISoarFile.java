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
package com.soartech.soar.ide.core.model;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;

import com.soartech.soar.ide.core.tcl.TclAstNode;

/**
 * Represents a Soar (or Tcl) file.  The children of a Soar file consist of
 * zero or more {@link ISoarFileAgentProxy} objects, one for each agent this
 * file is part of.
 * 
 * @author ray
 */
public interface ISoarFile extends ISoarElement, ISoarOpenable, ISoarSourceReference
{
    /**
     * @return The underlying file behind this object. Typically the same
     *      as {@link ISoarElement#getCorrespondingResource()}.
     */
    IFile getFile();
    
    /**
     * Returns the syntax tree for this file.  Check {@link TclAstNode#getError()}
     * before trusting the nodes. The returned node will always be of type
     * {@link TclAstNode#COMMAND} and will never be null.
     * 
     * @return The syntax tree for this command. This will not return null.
     */
    TclAstNode getTclSyntaxTree();
    
    /**
     * Returns the primary agent proxy for this file, i.e. the highest ranked
     * agent that this file is a member of.
     * 
     * @return Primary agent proxy for this file, or null if it is not part of
     *      any agent.
     * @throws SoarModelException
     */
    ISoarFileAgentProxy getPrimaryAgentProxy() throws SoarModelException;
    
    /**
     * @return List of all agent proxies for this file, one for each agent this
     *  file is part of.
     * @throws SoarModelException
     */
    List<ISoarFileAgentProxy> getAgentProxies() throws SoarModelException;
        
    /**
     * Return proxy for the given agent
     * 
     * @param agent The agent
     * @return The proxy for the given agent, or null if not found
     * @throws SoarModelException
     */
    ISoarFileAgentProxy getAgentProxy(ISoarAgent agent) throws SoarModelException;
    
    /**
     * @return true if this is a working copy
     */
    boolean isWorkingCopy();
    
    /**
     * If this is a working copy, returns the primary non-working copy that
     * this object was created from. Otherwise, returns <code>this</code>.
     * 
     * @return Primary non-working copy.
     */
    ISoarFile getPrimaryFile();
    
    /**
     * Create a working copy of this Soar file with the given owner.
     * If this object is already a working copy, then <code>this</code>
     * is returned.
     * 
     * @param owner The owner used to create the buffer
     * @return A working copy version of this file.
     * @throws SoarModelException 
     */
    ISoarFile createWorkingCopy(ISoarWorkingCopyOwner owner) throws SoarModelException;
    
    /**
     * If this file is in working copy mode, discards it.
     */
    void discardWorkingCopy();
    
    void makeConsistent(IProgressMonitor monitor, ISoarProblemReporter reporter) throws SoarModelException;

}
