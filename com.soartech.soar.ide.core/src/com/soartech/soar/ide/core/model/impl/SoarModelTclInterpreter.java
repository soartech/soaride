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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommandInterpreter;

import com.soartech.soar.ide.core.model.IExpandedTclCode;
import com.soartech.soar.ide.core.model.TclExpansionError;

/**
 * This is the wrapper around the Jacl Tcl interpreter used by each Soar
 * project to process Tcl.
 * 
 * <p>Each project instantiates a new interpreter each time it is built and
 * runs the interpreter starting from its start file.  The bodies of 
 * productions are cached as they are encountered to support display of
 * expanded Tcl.  The state of the interpreter is maintained until the
 * next build so that further expansion can be performed dynamically in
 * the editor using the current state of the interpreter (procs, globals,
 * etc.). 
 * 
 * @author ray
 */
public class SoarModelTclInterpreter
{
    private SoarCommandInterpreter jsoarInterp;
    private Map<String, ExpandedProductionInfo> productionMap = new HashMap<String, ExpandedProductionInfo>();
    private Map<String, OverwrittenProductionInfo> overwrittenProductions = new HashMap<String, OverwrittenProductionInfo>();
    private Set<IPath> visitedFiles = new HashSet<IPath>();
    
    private Object lock = new Object();
    private IProgressMonitor currentMonitor;
    
    private Set<String> filesToBuild = new HashSet<String>();
    
    public SoarModelTclInterpreter(SoarCommandInterpreter jsoarInterp )
    {
            this.jsoarInterp = jsoarInterp;
    }
    
    /**
     * Dispose this interpreter.
     * 
     * The JTCL requires that the Interp class be disposed with the same thread that instantiated it.
     * 
     * This method must only be called from the same thread that created this object instance.
     */
    public void dispose()
    {
        synchronized(lock)
        {
            jsoarInterp.dispose();
            jsoarInterp = null;
            
            productionMap.clear();
            overwrittenProductions.clear();
            filesToBuild.clear();
        }
    }
    
    /**
     * Expand the given input string as if it were an argument to a Tcl command.
     * 
     * @param namespace namespace to evaluate in
     * @param input The string to expand
     * @param offset The input string's offset in the parent buffer
     * @return Expanded tcl code object
     */
    public IExpandedTclCode expand(String namespace, String input, int offset)
    {
        // If no namespace is given, default to global
        if(namespace == null || namespace.length() == 0)
        {
            namespace = "::";
        }
        
        synchronized(lock)
        {
            TclExpansionError error = null;
            String result = "";
            
            try {
                result = jsoarInterp.eval("namespace eval " + namespace + " {" + 
                        "return " + input + "}");
            } catch (SoarException e) {
                error = new TclExpansionError(null,e.getMessage(),e.getMessage(), offset, 0);
            }
            
            return new ExpandedTclCode(error, result);
        }
    }
    
    /**
     * Execute a command string in the interpreter.
     * 
     * @param command
     * @return
     */
    public String executeString(String command)
    {
        synchronized(lock)
        {
            try 
            {
                String jsoarResult = jsoarInterp.eval(command); 
                return jsoarResult;
            } 
            catch (SoarException e) 
            {
                return e.getMessage();
            }            
        }
        
    }
    
    /**
     * Evaluate the given file in this interpreter.
     * 
     * @param file The file to evaluate
     * @param monitor Progress monitor
     * @return null on success or an error object on failure
     */
    public TclExpansionError evaluate(File file, IProgressMonitor monitor)
    {
        synchronized(lock)
        {
            try 
            {
                currentMonitor = monitor;
                jsoarInterp.source(file);
                return null;
            } 
            catch (SoarException e) 
            {
                e.printStackTrace();
                return new TclExpansionError(file.getName(), e.getMessage(), 0, 0);
            } 
            finally 
            {
                currentMonitor.done();
                currentMonitor = null;
            }
        }        
    }
    
    public ExpandedProductionInfo getExpandedProductionBody(String name)
    {
        synchronized(lock)
        {
            return productionMap.get(name);
        }
    }
    
    // TODO: Should probably not expose this, really just need to expose the list of 
    // sourced files
    public SoarCommandInterpreter getJSoarInterpreter()
    {
        return jsoarInterp;
    }
    
	/**
	 * @return The map of expanded production info
	 */
	public Map<String, ExpandedProductionInfo> getProductions() 
    {
	    return productionMap;
	}
    
    public Map<String, OverwrittenProductionInfo> getOverwrittenProductions()
    {
        return overwrittenProductions;
    }
    
    public boolean fileWasVisited(IPath path)
    {
        return visitedFiles.contains(path);
    }
    
    public void addVisitedFile(IPath path)
    {
        visitedFiles.add(path);
    }

	/**
	 * @return The set of files that need to be rebuilt in response
	 * to modifications to associated Tcl procedures.
	 */
	public Set<String> getFilesToBuild() {
		return filesToBuild;
	}
}
