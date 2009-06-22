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
import org.eclipse.core.resources.IProject;

/**
 * @author ray
 */
public interface ISoarProject extends ISoarElement, ISoarOpenable
{

    /**
     * @return The underlying Eclipse project this Soar project refers to
     */
    IProject getProject();
    
    /**
     * @return List of agents defined in this project in priority order
     * @throws SoarModelException 
     */
    List<ISoarAgent> getAgents() throws SoarModelException;
    
    /**
     * Get the agent associated with the given file, (*.soaragent)
     * 
     * @param file The file
     * @return The agent or null if not found.
     * @throws SoarModelException 
     */
    ISoarAgent getAgent(IFile file) throws SoarModelException;
    
    /**
     * Given a Soar file, returns the preferred agent used to the process
     * this file.  If the file is not part of any agent, then null is returned.
     * 
     * @param soarFile A Soar file
     * @return The preferred agent for processing the given file
     * @throws SoarModelException 
     */
    ISoarAgent getPreferredAgent(ISoarFile soarFile) throws SoarModelException;
    
    void setAgentPriorities(List<ISoarAgent> agents) throws SoarModelException;
    
}
