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

import org.eclipse.core.runtime.IPath;

/**
 * Interface for a "source" command in a Soar or Tcl file. 
 * 
 * <p>A source command can take several forms:
 * <pre>
 * The Tcl source command: 
 * 
 *     source filename.soar
 * 
 * Tcl source command nested in a pushd block:
 * 
 *     pushd dir
 *     source filename.soar
 *     ...
 *     popd
 * 
 * Various NGS commands:
 * 
 *     NGS_load-soar-dir dir
 *     NGS_load-settings dir
 *     NGS_echo-source
 *     NGS_echo-pushd
 * </pre>
 * 
 * <p>This interface encapsulates these alll of these approaches. A composite
 * file reference, such as a pushd block will be broken into multiple
 * ITclFileReference objects, each taking the pushd into account.
 * 
 * @author ray
 */
public interface ITclFileReference extends ISoarElement, ISoarSourceReference
{
    /**
     * @return True if a directory is referenced rather than a file. This 
     *      is the case for commands like pushd.
     */
    boolean isDirectory();
    
    /**
     * @return The Soar file referenced by this element or <code>null</code>
     *      if the file does not exist or no ISoarFile exists for the file. 
     *      This can happen for file references outside of the workspace.
     * @throws SoarModelException 
     */
    ISoarFile getReferencedSoarFile() throws SoarModelException;
    
    /**
     * @return Location (OS path) of the referenced file, or directory in the 
     *      case of pushd
     */
    IPath getReferencedLocation();
    
    /**
     * @return Path of the referenced file relative to the Eclipse workspace
     *      or null if not in the workspaces.
     */
    IPath getWorkspacePath();
    
    /**
     * @return Path of the referenced file relative to the Eclipse project
     *      it resides in or null if not in the workspace.
     */
    IPath getProjectRelativePath();
}
