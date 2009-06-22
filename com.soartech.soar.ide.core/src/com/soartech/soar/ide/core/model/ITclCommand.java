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

import com.soartech.soar.ide.core.tcl.TclAstNode;


/**
 * Represents a single tcl command in a Soar file. 
 * 
 * <p>A tcl command consist of a command name (possibly a variable) followed 
 * by zero or more arguments. The source range of a command starts at the 
 * beginning of its associated comment and extends to the end of its last 
 * argument.
 * 
 * @author ray
 */
public interface ITclCommand extends ISoarElement, ISoarSourceReference
{
    /**
     * @return The name of the command
     */
    String getCommandName();
    
    /**
     * @return The source range of the name of the command
     */
    ISoarSourceRange getCommandNameRange();
    
    /**
     * @return The comment associated with this command, typically the
     *  one just preceding it in the file, <code>null</code> if there is none.
     */
    ITclComment getAssociatedComment();
    
    /**
     * Returns the syntax tree for this command.  Check {@link TclAstNode#getError()}
     * before trusting the nodes. The returned node will always be of type
     * {@link TclAstNode#COMMAND} and will never be null.
     * 
     * @return The syntax tree for this command. This will not return null.
     */
    TclAstNode getTclSyntaxTree();
}
