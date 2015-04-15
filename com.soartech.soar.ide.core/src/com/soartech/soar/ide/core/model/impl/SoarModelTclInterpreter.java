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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jsoar.kernel.SoarException;
import org.jsoar.tcl.SoarTclInterface;
import org.jsoar.util.commands.SoarCommandInterpreter;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.RelocatableTclInterpreter;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

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
    
    private RelocatableTclInterpreter interp;
    
    private boolean sourceChangesDir;
    private TclSourceCommand sourceCommand;
    private TclWorkingDirectoryCommands workingDirectoryCommands;
    private Map<String, ExpandedProductionInfo> previousProductionMap;
    private Map<String, ExpandedProductionInfo> productionMap = new HashMap<String, ExpandedProductionInfo>();
    private Map<String, OverwrittenProductionInfo> overwrittenProductions = new HashMap<String, OverwrittenProductionInfo>();
    private Set<IPath> visitedFiles = new HashSet<IPath>();
    
    private Object lock = new Object();
    private boolean expanding = false;
    private IProgressMonitor currentMonitor;
    
    private Set<String> filesToBuild = new HashSet<String>();
    
    public SoarModelTclInterpreter(boolean sourceChangesDir, Map<String,ExpandedProductionInfo> previousProductionMap, SoarCommandInterpreter jsoarInterp )
    {
            this.interp = new RelocatableTclInterpreter(new ProgressHandler());
            this.jsoarInterp = jsoarInterp;
            this.sourceChangesDir = sourceChangesDir;
            this.previousProductionMap = previousProductionMap;
            
            installCommands();
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
            //JSOAR
            jsoarInterp.dispose();
            jsoarInterp = null;
            
            //TCL
            interp.dispose();
            interp = null;
            
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
            if(interp == null)
            {
                throw new IllegalStateException("Interpreter is disposed");
            }
            enterExpandMode();
            TclExpansionError error = null;
            String result = "";
            
            //JSOAR
            try {
                result = jsoarInterp.eval("namespace eval " + namespace + " {" + 
                        "return " + input + "}");
            } catch (SoarException e) {
                error = new TclExpansionError(null,e.getMessage(),e.getMessage(), offset, 0);
            }
            
            //TCL
//            try
//            {
//                // How does this work? Well, it evaluates the command
//                //      namespace eval <namespace> { return <input> }
//                // and returns the result.  The return command just returns
//                // the expansion of its argument. "namespace eval" evaluates
//                // commands in a particular namespace and returns the result.
//                interp.eval("namespace eval " + namespace + " {" + 
//                            "return " + input + "}", TCL.EVAL_GLOBAL);
//                result = interp.getResult().toString();
//                
//            }
//            catch (TclException e)
//            {
//            	//Less informative error
//                //error = new TclExpansionError(interp.getResult().toString(), offset, 0);
//            	error = new TclExpansionError(null,getErrorInfo(),interp.getResult().toString(), offset, 0);
//            } 
            exitExpandMode();
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
            if(interp == null)
            {
                throw new IllegalStateException("Interpreter is disposed");
            }
            
            //JSOAR
            try 
            {
                String jsoarResult = jsoarInterp.eval(command); 
                return jsoarResult;
            } 
            catch (SoarException e) 
            {
//                e.printStackTrace();
                return e.getMessage();
            }
            
            //TCL
//            try {
//                interp.eval(command, TCL.EVAL_GLOBAL);
//                String result = interp.getResult().toString();
//                return result;
//            } catch (TclException e) {
//                String result = interp.getResult().toString();
//                return result;
//            }
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
        //JSOAR
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
        
        
        //TCL
//        String dir = file.getParent().replace('\\', '/');
//        String fileName = file.getName().replace('\\', '/');
//        synchronized(lock)
//        {
//            if(interp == null)
//            {
//                throw new IllegalStateException("Interpreter is disposed");
//            }
//            try
//            {
//                sourceCommand.reset();
//                workingDirectoryCommands.reset();
//                
//                currentMonitor = monitor;
//                if(dir != null)
//                {
//                    interp.eval("pushd \"" + dir + "\"", TCL.EVAL_GLOBAL);
//                    interp.eval("source \"" + fileName + "\"", TCL.EVAL_GLOBAL);
//                    interp.eval("popd", TCL.EVAL_GLOBAL);
//                }
//                else
//                {
//                    interp.eval("source \"" + fileName + "\"", TCL.EVAL_GLOBAL);            
//                }
//                return null;
//            }
//            catch(TclException e)
//            {
//                return new TclExpansionError(sourceCommand.getCurrentFile(), 
//                                             getErrorInfo(), 0, 0);
//            }
//            finally
//            {
//                currentMonitor.done();
//                currentMonitor = null;
//            }
//        }
    }
    
    public String getErrorInfo()
    {
        //this method is only used by the TCL interpreter
        synchronized (lock)
        {
            try
            {
                return interp.getVar("errorInfo", TCL.GLOBAL_ONLY).toString();
            }
            catch (TclException e)
            {
                e.printStackTrace();
                return "Error while retrieving errorInfo: " + e.getMessage();
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
    
    RelocatableTclInterpreter getInterpreter()
    {
        return interp;
        
    }
    IProgressMonitor getProgressMonitor()
    {
        return currentMonitor;
    }
    
    private void installCommands()
    {
        sourceCommand = new TclSourceCommand(this, sourceChangesDir);
        workingDirectoryCommands = new TclWorkingDirectoryCommands(interp);
        interp.createCommand("sp", new SoarProductionCommand());
        interp.createCommand("excise", new ExciseCommand());
        SoarModelTclCommands.installNullCommands(interp);
    }
    
    private void enterExpandMode()
    {
        sourceCommand.uninstall();
        workingDirectoryCommands.uninstall();
        expanding = true;
    }
    
    private void exitExpandMode()
    {
        expanding = false;
        workingDirectoryCommands.install();
        sourceCommand.install();
    }
    
    private class ProgressHandler implements RelocatableTclInterpreter.ProgressHandler
    {
        /* (non-Javadoc)
         * @see tcl.lang.RelocatableTclInterpreter.ProgressHandler#onProgress(tcl.lang.RelocatableTclInterpreter)
         */
        public void onProgress(RelocatableTclInterpreter interp)
        {
            if(currentMonitor != null)
            {
                if(currentMonitor.isCanceled())
                {
                    throw new RelocatableTclInterpreter.InterruptedException();
                }
                else
                {
                    currentMonitor.worked(1);
                }
            }
        }
    }
    
    private class SoarProductionCommand implements Command
    {
        /* (non-Javadoc)
         * @see tcl.lang.Command#cmdProc(tcl.lang.Interp, tcl.lang.TclObject[])
         */
        public void cmdProc(Interp interp, TclObject[] args) throws TclException
        {
            if(args.length != 2)
            {
                throw new TclNumArgsException(interp, 1, args, "body");
            }
            
            String body = args[1].toString();
            String name = getProductionName(body);
            
            if(!expanding)
            {
                // First check whether this is overwriting an existing, non-excised, production
                ExpandedProductionInfo overwritten = productionMap.get(name);
                if(overwritten != null && 
                   !overwritten.excised && 
                   !overwrittenProductions.containsKey(name))
                {
                    OverwrittenProductionInfo opi = new OverwrittenProductionInfo();
                    opi.file = sourceCommand.getCurrentFile();
                    opi.name = name;
                    opi.previousInstance = overwritten;
                    
                    overwrittenProductions.put(name, opi);
                }
                
                // Construct expansion info and store in production map
                ExpandedProductionInfo info = new ExpandedProductionInfo();
                info.name = name;
                info.expandedBody = body;
                interp.eval("namespace current");
                info.namespace = interp.getResult().toString();
                info.file = sourceCommand.getCurrentFile();
                
                productionMap.put(name, info);
                
                // Check whether this production's expansion has changed since the
                // previous run of the interpreter. If so, the file needs to marked
                // for reprocessing to catch secondary affects of Tcl macro changes.
                ExpandedProductionInfo previousInfo = previousProductionMap.get( name );
                if ( previousInfo != null ) 
                {
                	String previousBody = previousInfo.expandedBody;
                	if ( ( previousBody == null && body != null ) ||
                	     ( previousBody != null && body == null ) ||
                		 ( previousBody != null && body != null &&
                		 !previousBody.equals( body ) ) ) 
                    {
                	    filesToBuild.add( info.file );
                	}
                }
                
            }
            
            interp.setResult(args[1]);
        }
        
    }
    
    private class ExciseCommand implements Command
    {
        /* (non-Javadoc)
         * @see tcl.lang.Command#cmdProc(tcl.lang.Interp, tcl.lang.TclObject[])
         */
        public void cmdProc(Interp interp, TclObject[] args) throws TclException
        {
            if(args.length != 2)
            {
                throw new TclNumArgsException(interp, 1, args, "production");
            }
            
            String name = args[1].toString();
            
            if(!expanding)
            {
                ExpandedProductionInfo info = productionMap.get( name );
                if ( info != null ) 
                {
                    info.excised = true;
                }
            }
            
            interp.setResult(args[1]);
        }
        
    }

    private String getProductionName(String body)
    {
        int i = 0;
        boolean nonWSFound = false;
        while(i < body.length())
        {
            if(Character.isWhitespace(body.charAt(i)))
            {
            	if ( nonWSFound ) {
            		break;
            	}
            } else {
            	nonWSFound = true;
            }
            ++i;
        }
        return body.substring(0, i).trim();
    }
    
    public static void main(String[] args)
    {
        SoarModelTclInterpreter interp = new SoarModelTclInterpreter(false, 
        		new HashMap<String,ExpandedProductionInfo>(), null);
        
        try
        {
            interp.interp.eval("namespace eval Foo { variable x 99 }");
            interp.interp.eval(" puts $Foo::x");
        }
        catch (TclException e)
        {
            System.err.println(interp.interp.getResult().toString());
            e.printStackTrace();
        }
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
