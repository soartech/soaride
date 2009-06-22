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

import java.util.List;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.BasicSoarSourceRange;
import com.soartech.soar.ide.core.model.ISoarProblemReporter;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.ITclComment;
import com.soartech.soar.ide.core.model.ITclProcedure;
import com.soartech.soar.ide.core.model.ITclProcedureHelp;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.impl.serialization.ElementMemento;
import com.soartech.soar.ide.core.model.impl.serialization.SourceRangeMemento;
import com.soartech.soar.ide.core.model.impl.serialization.TclProcedureMemento;
import com.soartech.soar.ide.core.tcl.TclAstNode;
import com.soartech.soar.ide.core.tcl.TclAstNodeSourceRange;

/**
 * Implementation of {@link ITclProcedure} interface
 * 
 * @author ray
 */
public class TclProcedure extends TclCommand implements ITclProcedure
{
    private String name = "";
    private ISoarSourceRange nameRange;
    private String arguments = "";
    
    /**
     * @param parent
     * @param astNode
     * @throws SoarModelException
     */
    public TclProcedure(SoarFileAgentProxy parent, ISoarProblemReporter reporter, TclAstNode astNode)
            throws SoarModelException
    {
        super(parent, astNode);
        
        nameRange = new BasicSoarSourceRange(astNode.getStart(), 0);
        
        List<TclAstNode> words = getTclSyntaxTree().getWordChildren();
        // proc name args
        if(words.size() >= 2) // get proc name
        {
            TclAstNode nameNode = words.get(1);
            nameRange = new TclAstNodeSourceRange(nameNode);
            name = getSource(nameRange);
        }
        if(words.size() >= 3) // get args
        {
            TclAstNode argsNode = words.get(2);
            parseArguments(argsNode);
        }
    }
    
    TclProcedure(SoarFileAgentProxy parent, TclProcedureMemento memento) throws SoarModelException
    {
        super(parent, memento);
        
        this.name = memento.getProcedureName();
        this.nameRange = new BasicSoarSourceRange(memento.getProcedureNameRange());
        this.arguments = memento.getArguments();
    }
    
    private void parseArguments(TclAstNode argsNode) throws SoarModelException
    {
        int type = argsNode.getType();
        
        // First strip off braces, quotes, etc.
        int start = argsNode.getStart();
        int length = argsNode.getLength();
        if(type != TclAstNode.NORMAL_WORD)
        {
            ++start;
            length = Math.max(length - 2, 0);
        }
        
        arguments = getSource(new BasicSoarSourceRange(start, length));
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.TclCommand#createMemento()
     */
    @Override
    public ElementMemento createMemento()
    {
        return saveState(new TclProcedureMemento());
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.TclCommand#saveState(com.soartech.soar.ide.core.model.impl.serialization.Mementos.Element)
     */
    @Override
    protected ElementMemento saveState(ElementMemento memento)
    {
        super.saveState(memento);
        
        TclProcedureMemento procMemento = (TclProcedureMemento) memento;
        
        procMemento.setArguments(arguments);
        procMemento.setProcedureName(name);
        procMemento.setProcedureNameRange(new SourceRangeMemento(nameRange));
        
        return memento;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ITclProcedure#getProcedureName()
     */
    public String getProcedureName()
    {
        return name;
    }


    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ITclProcedure#getProcedureNameRange()
     */
    public ISoarSourceRange getProcedureNameRange()
    {
        return nameRange;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ITclProcedure#getArgumentList()
     */
    public String getArgumentList()
    {
        return arguments;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ITclProcedure#getHelp()
     */
    public ITclProcedureHelp getHelp()
    {
    	ITclComment comment = getAssociatedComment();
    	String commentText = ""; 
		
    	if(comment != null)
    	{
        	try {
				commentText = comment.getCommentText();
			} catch (SoarModelException e) {
                SoarCorePlugin.log(e);
			}
    	}
        
        if(commentText.length() > 0)
        {
            commentText += "\n";
        }
        commentText += "Arguments:\n   ";
        commentText += getArgumentList().trim();
    	
    	return new TclProcedureHelp(commentText, getProcedureName(), false);
    }
}
