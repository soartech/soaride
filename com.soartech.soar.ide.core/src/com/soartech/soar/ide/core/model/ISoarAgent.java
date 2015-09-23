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
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;


import com.soartech.soar.ide.core.model.datamap.ISoarDatamap;

/**
 * Interface for an agent in the Soar IDE.
 * 
 * @author ray
 */
public interface ISoarAgent extends ISoarElement
{
    /**
     * Returns the name of the agent. This is NOT guaranteed to be unique 
     * within a project. Use the agent's file path to get a truly unique
     * name
     * 
     * @return The possibly non-unique name of the agent
     */
    String getName();
    
    /**
     * @return The underlying file behind this object. Typically the same
     *      as {@link ISoarElement#getCorrespondingResource()}.
     */
    IFile getFile();
    
    /**
     * Constructs a working copy of this agent that can be modified
     * 
     * @return A new working copy of the agent.
     */
    ISoarAgent getWorkingCopy();
    
    /**
     * Returns true if this is a working copy of an agent retrieved through
     * a call to {@link #getWorkingCopy()}. A working copy may be modified 
     * and saved.
     * 
     * @return True if this is a working copy.
     */
    boolean isWorkingCopy();
    
    /**
     * Discard a working copy and any changes made to it. Once this method is
     * called no other methods should be called on the object
     */
    void discardWorkingCopy();
    
    /**
     * @return true if this is a working copy and it is changed.
     */
    boolean isWorkingCopyChanged();
    
    /**
     * @return The primary agent for this working copy, or this if it is not
     *       a primary.
     */
    ISoarAgent getPrimary();
    
    /**
     * Resynchronize this agent with its file on disk. If this is a working
     * copy, then the working copy is reverted to the state of the agent
     * from which is was created.
     * 
     * @param monitor
     * @throws SoarModelException
     */
    void makeConsistent(IProgressMonitor monitor) throws SoarModelException;

    /**
     * Save the contents of this agent working copy to disk. This method is
     * only valid on a working copy.
     * 
     * <p>Calling this method will write the agent to disk and trigger a
     * build of the project which, in turn, will cause the agent from which 
     * this working copy was created to be rebuilt through a call to 
     * {@link #makeConsistent(IProgressMonitor)}
     * 
     * @param monitor
     * @return true if changes were detected and the file was actually saved
     * @throws IllegalStateException if called on a non-working copy
     * @throws SoarModelException
     */
    boolean save(IProgressMonitor monitor) throws SoarModelException;
    
    /**
     * @return true if the source command automatically changes directory 
     *      (UofM-style), or false otherwise (Tcl/SoarTech-style)
     */
    boolean getSourceCommandChangesDirectory();
    
    /**
     * Set to true if the source command automatically changes directory 
     * (UofM-style), or false otherwise (Tcl/SoarTech-style).
     * 
     * @param sourceCommandChangesDirectory
     */
    void setSourceCommandChangesDirectory(boolean sourceCommandChangesDirectory);
    
    /**
     * Returns the root startup file, e.g. load.soar, for this project. This
     * is the file that is loaded when the project is run in Soar
     * 
     * @return The root startup file for this project
     */
    IFile getStartFile();
    
    /**
     * Set the root startup file for this project. This value is persisted
     * across development sessions. The file must be a memberr of this project.
     * 
     * <p>This method may only be called on a working copy
     * 
     * @param file The file, or <code>null</code> to clear the file.
     * @throws IllegalStateException if called on a non-working copy
     */
    void setStartFile(IFile file);
    
    /**
     * @return Set of all files and folders in the agent
     */
    Set<IResource> getMembers();
    
    /**
     * Add a Soar file to this agent. If the file is already part of the agent
     * this is a no-op.
     * 
     * <p>This method may only be called on a working copy
     * 
     * @param file The file to add
     * @throws IllegalStateException if called on a non-working copy
     */
    void addFile(IFile file);
    
    /**
     * Remove a Soar file to this agent. If the file is not part of the agent
     * this is a no-op.
     * 
     * <p>This method may only be called on a working copy
     * 
     * @param file The file to remove
     * @throws SoarModelException 
     * @throws IllegalStateException if called on a non-working copy
     */
    void removeFile(IFile file) throws SoarModelException;

    /**
     * Add a folder to the agent. Implies that all SoarFiles in all subfolders
     * are included in the agent.
     * 
     * <p>This method may only be called on a working copy
     * 
     * @param folder The folder to add
     * @throws IllegalStateException if called on a non-working copy
     */
    void addFolder(IContainer folder);
    
    /**
     * Remove a folder from this agent. This implies that any sub-file or
     * sub-folder of this folder is removed from the agent. If the folder
     * is not already part of the agent, this is a no-op.
     * 
     * <p>This method may only be called on a working copy
     * 
     * @param folder The folder to remove.
     * @throws IllegalStateException if called on a non-working copy
     */
    void removeFolder(IContainer folder);
    
    /**
     * Returns true if the given resource is a member of the agent. 
     * 
     * @param resource The file or folder to test for membership
     * @return True if the file is part of the agent
     */
    boolean contains(IResource resource);
    
    /**
     * Returns true if all child of the given container are contained
     * by this agent.
     * 
     * @param container The container
     * @return true if all child at all levels are contained by this
     *  agent.
     */
    boolean containsAllChildrenOf(IContainer container);
    
    /**
     * Returns true if any children of the given container are contained by
     * this agent
     * 
     * @param container The container
     * @return true if any children of the given container are contained by
     *  this agent
     */
    boolean containsAnyChildrenOf(IContainer container);
    
    /**
     * Expand Tcl variables and macros in the given string and return the
     * result. The offset parameter gives an offset of the string in its
     * original context. This allows the result object to give reasonable
     * mappings from output text to input text.
     * 
     * @param namespace The namespace to expand the string in
     * @param input The string to expand, i.e. a Tcl string with embedded
     *      variables and bracketed commands.
     * @param offset Offset of the input in its original buffer context
     * @return The expansion result
     */
    IExpandedTclCode expandTclString(String namespace, String input, int offset);
    
    /**
     * @return The project's datamap
     */
    ISoarDatamap getDatamap();
    
    /**
     * Get a tcl procedure by name.
     * 
     * @param name The name of the procedure.
     * @return The tcl procedure, or null if not found.
     */
    ITclProcedure getProcedure(String name);
    
    /**
     * Get all the tcl procedures from all the files contained in this project.
     * 
     * @return The tcl procedures.
     */
    List<ITclProcedure> getAllProcedures();
    
    /**
     * Get a soar production by name.
     * 
     * @param name The name of the production.
     * @return The production, or null if not found.
     */
    ISoarProduction getProduction(String name);
    
    /**
     * Get all the soar productions from all the files contained in this project.
     * 
     * @return The soar productions.
     */
    List<ISoarProduction> getAllProductions();
    
    /**
     * Execute the given string command in the agent's interpreter. 
     * 
     * @return The interpreter's result.
     */
    String executeString(String command);

}
