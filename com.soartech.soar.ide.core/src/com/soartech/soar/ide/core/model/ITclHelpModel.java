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

/**
 * Interface for accessing Tcl procedure help, in particular hover help in
 * the Soar IDE. An instance of this interface can be retrieved through
 * {@link ISoarModel#getTclHelpModel()}.
 * 
 * <p>The help objects obtained through this interface are a combination of
 * pre-defined help object read from disk at startup and those dynamically
 * generated from source comments at run-time.
 * 
 * @author ray
 */
public interface ITclHelpModel
{
    /**
     * Retrieve help information for a named procedure. 
     * 
     * <p>The search algorithm is as follows:
     * 
     * <ul>
     * <li>If a project is specified, the project is searched for a procedure
     * with the given name and queried for its help info using
     * {@link ITclProcedure#getHelp()}
     * <li>If the procedure is not found in the project, the global help 
     * registry is checked.
     * </ul>
     * 
     * @param procName The name of the procedure
     * @param project The relevant Soar project, or <code>null</code>.
     * @param agent The preferred agent, or null for any agent in the project
     * @return Help for the procedure, or <code>null</code> if not found.
     */
    ITclProcedureHelp getHelp(String procName, ISoarProject project, ISoarAgent agent);
    
    /**
     * Does the given named procedure have help associated with it.
     * 
     * @param procName
     * @return
     */
    boolean hasHelp(String procName);
}
