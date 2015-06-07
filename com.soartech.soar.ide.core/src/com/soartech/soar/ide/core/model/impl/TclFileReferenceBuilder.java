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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.soartech.soar.ide.core.model.ISoarBuffer;
import com.soartech.soar.ide.core.model.ISoarProblemReporter;
import com.soartech.soar.ide.core.model.ITclFileReferenceConstants;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarProblem;
import com.soartech.soar.ide.core.model.SoarModelTools;
import com.soartech.soar.ide.core.tcl.TclAstNode;

/**
 * @author ray
 */
class TclFileReferenceBuilder implements ITclFileReferenceConstants
{

    private static final List<String> COMMANDS =
        Arrays.asList(CD, PUSHD, SOURCE, POPD, 
                      NGS_LOAD_DIR, NGS_LOAD_SETTINGS, NGS_SOURCE, NGS_PUSHD,
                      LP_SOURCE_FILE, LP_DONT_SOURCE_FILE, LP_SOURCE_DIR);
    
    private SoarFileAgentProxy soarFile;
    private TclAstNode root;
    private ISoarProblemReporter reporter;
    private ISoarBuffer buffer;
    private Stack<IPath> pathStack = new Stack<IPath>();

    public static List<TclFileReference> findReferences(SoarFileAgentProxy parent, TclAstNode rootNode, ISoarProblemReporter reporter) throws SoarModelException
    {
        TclFileReferenceBuilder builder = new TclFileReferenceBuilder();
        builder.soarFile = parent;
        builder.root = rootNode;
        builder.reporter = reporter;
        builder.buffer = parent.getFile().getBuffer();
        
        return builder.findReferences();
    }
    
    private IPath getCurrentDirectory()
    {
        assert !pathStack.isEmpty();
        return pathStack.peek();
    }
    
    private boolean mayContainVariableOrCommand( ReferenceCommand c ) {

    	for ( TclAstNode word : c.words ) {
    		String wordStr = buffer.getText( word.getStart(), word.getLength());
    		if ( wordStr.contains( "$" ) ) return true;
    		if ( word.containsType( TclAstNode.COMMAND_WORD ) ) return true;
    	}
    	
    	return false;
    }
    
    private List<TclFileReference> findReferences() throws SoarModelException
    {
        assert soarFile != null;
        assert root != null;
        assert root.getType() == TclAstNode.ROOT;
        assert buffer != null;
        
        List<TclFileReference> r = new ArrayList<TclFileReference>();
        
        List<ReferenceCommand> commands = getFileReferences();
        
        // Seed path stack with directory of the Soar file
        pathStack.push(soarFile.getCorrespondingResource().getLocation().removeLastSegments(1));
        
        ReferenceCommand lastPushd = null;
        for(ReferenceCommand c : commands)
        {
            if(CD.equals(c.command) || PUSHD.equals(c.command) || NGS_PUSHD.equals(c.command))
            {
                if(c.words.size() >= 2)
                {
                    IPath arg = getPath(c, 1, true);
                    checkPath(c, arg, true);
                    
                    r.add(new TclFileReference(soarFile, c.node, arg, true));
                    pathStack.push(arg);
                    lastPushd = c;
                }
                
                if(CD.equals(c.command))
                {
                    createWarning(c, "Use of cd is discouraged. Use pushd instead.");
                }
            }
            else if(SOURCE.equals(c.command) || NGS_SOURCE.equals(c.command) || 
                    LP_SOURCE_FILE.equals(c.command) || LP_DONT_SOURCE_FILE.equals(c.command))
            {
                if(c.words.size() >= 2)
                {
                    IPath arg = getPath(c, 1, false);
                    // Don't try to validate dont_source_file command (bug #1454)
                    if(!LP_DONT_SOURCE_FILE.equals(c.command))
                    {
                        checkPath(c, arg, false);
                    }
                    r.add(new TclFileReference(soarFile, c.node, arg, false));
                }
            }
            else if(NGS_LOAD_DIR.equals(c.command) || LP_SOURCE_DIR.equals(c.command))
            {
                if(c.words.size() >= 2)
                {
                    IPath arg = getPath(c, 1, true);
                    arg = arg.append("load.soar");
                    checkPath(c, arg, false);
                    r.add(new TclFileReference(soarFile, c.node, arg, false));
                }
            }
            else if(NGS_LOAD_SETTINGS.equals(c.command))
            {
                if(c.words.size() >= 2)
                {
                    IPath arg = getPath(c, 1, true);
                    arg = arg.append("settings.tcl");
                    checkPath(c, arg, false);
                    r.add(new TclFileReference(soarFile, c.node, arg, false));
                }
            }
            else if(POPD.equals(c.command))
            {
                if(pathStack.size() > 1)
                {
                    pathStack.pop();
                }
                else
                {
                    createWarning(c, "Missing matching pushd");
                }
            }
        }
        
        if(pathStack.size() > 1)
        {
            assert lastPushd != null;
            createWarning(lastPushd, "Missing matching popd");
        }
        
        return r;
    }
    
    private List<ReferenceCommand> getFileReferences()
    {
        List<ReferenceCommand> refs = new ArrayList<ReferenceCommand>();
        
        for(TclAstNode kid : root.getChildren())
        {
            ReferenceCommand ref = getFileReference(kid);
            if(ref != null)
            {
                refs.add(ref);
            }
        }
        return refs;
    }
    
    private ReferenceCommand getFileReference(TclAstNode node)
    {
        if(node.getError() != null || node.getType() != TclAstNode.COMMAND)
        {
            return null;
        }
        ReferenceCommand c = new ReferenceCommand();
        c.node = node;
        c.words = node.getWordChildren();
        if(c.words.isEmpty())
        {
            return null;
        }
        TclAstNode nameWord = c.words.get(0);
        if(nameWord.getError() != null)
        {
            return null;
        }
        else if(nameWord.getType() == TclAstNode.NORMAL_WORD)
        {
            c.command = buffer.getText(nameWord.getStart(), nameWord.getLength());
        }
        else if(nameWord.getType() == TclAstNode.BRACED_WORD || nameWord.getType() == TclAstNode.QUOTED_WORD)
        {
            c.command = buffer.getText(nameWord.getStart() + 1, nameWord.getLength() - 2);
        }
        
        if(!COMMANDS.contains(c.command))
        {
            return null;
        }
        return c;
    }
    
    private IPath getPath(ReferenceCommand cmd, int index, boolean dir) throws SoarModelException
    {
        TclAstNode arg = cmd.words.get(index);
        int start = arg.getStart();
        int length = arg.getLength();
        
        // Stip off braces or quotes...
        // TODO: What if quotes contain a command?
        if(arg.getType() == TclAstNode.BRACED_WORD || 
           arg.getType() == TclAstNode.QUOTED_WORD)
        {
           if(length >= 2)
           {
               start = start + 1;
               length = length - 2;
           }
        }
        IPath fileArg = new Path(buffer.getText(start, length));
        if(!fileArg.isAbsolute())
        {
            fileArg = getCurrentDirectory().append(fileArg);
        }
        
        return fileArg;
    }
    
    private void checkPath(ReferenceCommand cmd, IPath path, boolean dir) throws SoarModelException
    {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        
        // See if the file exists in Eclipse...
        IResource eclipseResource = root.findMember(path);
        boolean eclipseExists = eclipseResource != null && eclipseResource.exists();
        
        // See if the file exists externally....
        File externalFile = path.toFile();
        boolean externalExists = externalFile.exists();
        
        // Check for the wrong type of argument (directory vs. file)
        boolean wrongType = false;
        if(eclipseExists)
        {
            if(dir && !(eclipseResource instanceof IContainer))
            {
                wrongType = true;
            }
            else if(!dir && !(eclipseResource instanceof IFile))
            {
                wrongType = true;
            }
        }
        else if(externalExists)
        {
            if(dir && !externalFile.isDirectory())
            {
                wrongType = true;
            }
            else if(!dir && !externalFile.isFile())
            {
                wrongType = true;
            }
        }
        
        // Report errors
        if(wrongType)
        {
        	if ( !mayContainVariableOrCommand( cmd ) ) {
	            createError(cmd, path + " is a " + 
	                        (dir ? "file" : "directory") + ". Expected a " +
	                        (dir ? "directory" : "file"));
        	}
        }
        else if(!eclipseExists && !externalExists)
        {
        	if ( !mayContainVariableOrCommand( cmd ) ) {
        		createError(cmd, path + " does not exist");
        	}
        }
    }
    
    private void createError(ReferenceCommand ref, String message) throws SoarModelException
    {
    	//int lineNumber = SoarModelTools.getLineNumber((IFile)this.soarFile.getFile(), ref.node.getStart());
        reporter.report(SoarProblem.createError(message, "Line 1", ref.node.getStart(), ref.node.getLength(),"test",new HashMap<String, Comparable<?>>()));
    	reporter.report(SoarProblem.createError(message, ref.node.getStart(), ref.node.getLength()));
    }
    
    private void createWarning(ReferenceCommand ref, String message) throws SoarModelException
    {
        reporter.report(SoarProblem.createWarning(message, ref.node.getStart(), ref.node.getLength()));
    }
    
    private static class ReferenceCommand
    {
        public TclAstNode node;
        public List<TclAstNode> words;
        public String command;
    }
}
