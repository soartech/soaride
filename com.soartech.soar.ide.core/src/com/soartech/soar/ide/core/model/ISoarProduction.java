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

import com.soartech.soar.ide.core.model.ast.SoarProductionAst;
import com.soartech.soar.ide.core.model.impl.SoarFileAgentProxy;

/**
 * Represents a Soar command. Extends {@link ITclCommand} because sp is
 * typically implemented as a Tcl command.
 * 
 * @author ray
 */
public interface ISoarProduction extends ITclCommand
{
    
    /**
     * @return The name of the production
     */
    String getProductionName();
    
    /**
     * @return The range or the name of the production
     */
    ISoarSourceRange getProductionNameRange();
    
    /**
     * Parse the content of this production and return the syntax tree.
     * 
     * @return The syntax tree or null if failed to parse
     */
    SoarProductionAst getSyntaxTree();
    
    /**
     * @return true if the body of the production is enclosed in braces, i.e.
     *      it is safe to interpret as pure soar code.
     */
    boolean isBodyInBraces();
    
    /**
     * @return The soar file
     */
    SoarFileAgentProxy getSoarFileProxy();
}
