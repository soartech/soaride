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
import org.eclipse.core.resources.IWorkspace;

/**
 * The root interface of the  Soar model. This is the main entry point for
 * access to all Soar objects. The single instance can be retrieved through
 * {@link SoarModelManager#getSoarModel()}.
 * 
 * @author ray
 */
public interface ISoarModel extends ISoarElement, ISoarOpenable
{
    
    /**
     * Add a listener to this model
     * 
     * @param listener The listener to add
     */
    void addListener(ISoarModelListener listener);
    
    /**
     * Remove a listener from this model
     * 
     * @param listener The listener to remove
     */
    void removeListener(ISoarModelListener listener);

    /**
     * @return List of projects with the Soar nature
     * @throws SoarModelException 
     */
    List<ISoarProject> getProjects() throws SoarModelException;
    
    /**
     * Get a Soar project by name.
     * 
     * @param name The name of the project
     * @return The project, or null if not found
     * @throws SoarModelException 
     */
    ISoarProject getProject(String name) throws SoarModelException;
    
    /**
     * Retrieve the Soar file object for a particular Eclipse file. 
     * 
     * @param file The Eclipse file
     * @return The corresponding Soar file, or <code>null</code> if not found.
     * @throws SoarModelException
     */
    ISoarFile getFile(IFile file) throws SoarModelException;
    
    ISoarAgent getAgent(IFile file) throws SoarModelException;
    
    /**
     * Retrieve the initial contents for a new agent file
     * 
     * @param file The proposed new agent file
     * @param empty If true, the agent will be initially empty.
     * @return Initial contents of the file
     */
    String getInitialAgentFileContents(IFile file, boolean empty);
    
    /**
     * @return The workspace associated with this model
     */
    IWorkspace getWorkspace();
    
    /**
     * @return The Tcl help model for the Soar IDE. This method will never
     *      return <code>null</code>
     */
    ITclHelpModel getTclHelpModel();
}
