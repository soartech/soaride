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

import org.eclipse.core.resources.IResource;

import com.soartech.soar.ide.core.model.BasicSoarSourceRange;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarModelConstants;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.ITclCommand;
import com.soartech.soar.ide.core.model.ITclComment;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.impl.serialization.ElementMemento;
import com.soartech.soar.ide.core.model.impl.serialization.SourceRangeMemento;
import com.soartech.soar.ide.core.model.impl.serialization.TclCommandMemento;
import com.soartech.soar.ide.core.tcl.TclAstNode;
import com.soartech.soar.ide.core.tcl.TclAstNodeSourceRange;

/**
 * Base implementation of {@link ITclCommand} interface.
 *  
 * @author ray
 */
public class TclCommand extends AbstractSourceReferenceElement implements ITclCommand
{
    private SoarFileAgentProxy soarFile;
    private TclAstNode astNode;
    private String commandName;
    private ISoarSourceRange commandNameRange;
    private TclComment comment;
    
    /**
     * @param parent
     * @throws SoarModelException 
     */
    public TclCommand(SoarFileAgentProxy parent, TclAstNode astNode, String expandedSource) throws SoarModelException
    {
        super(parent, expandedSource);
        this.soarFile = parent;
        this.astNode = astNode;
        
        int startOffset = astNode.getStart();
        
        // If the preceding node is a comment, use it.
        TclAstNode commentNode = astNode.getPrevious();
        if(commentNode != null && commentNode.getType() == TclAstNode.COMMENT)
        {
            String text = commentNode.getInternalText(parent.getBuffer().getCharacters());
            if(!text.startsWith(ISoarModelConstants.REGION_START) &&
               !text.startsWith(ISoarModelConstants.REGION_END) &&
               !text.contains(ISoarModelConstants.SOARDOC_FILE))
            {
                // Include comment in this object's total source range
                startOffset = commentNode.getStart();
                comment = new TclComment(this, commentNode);
                addChild(comment);
            }
        }
        
        int length = (astNode.getStart() + astNode.getLength()) - startOffset;
        setSourceRange(new BasicSoarSourceRange(startOffset, length));
        
        List<TclAstNode> words = astNode.getWordChildren();
        if(!words.isEmpty())
        {
            commandNameRange = new TclAstNodeSourceRange(words.get(0));
            commandName = getSource(commandNameRange);
        }
    }
    
    public TclCommand(SoarFileAgentProxy parent, TclCommandMemento memento) throws SoarModelException
    {
        super(parent, memento);
        this.soarFile = parent;
        this.astNode = null;
        this.commandName = memento.getCommandName();
        this.commandNameRange = new BasicSoarSourceRange(memento.getCommandNameRange());
        
        for(ISoarElement kid : getChildren())
        {
            if(kid instanceof TclComment)
            {
                this.comment = (TclComment) kid;
            }
        }
    }
    
    protected SoarFileAgentProxy getSoarFile()
    {
        return soarFile;
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSourceReferenceElement#createMemento()
     */
    @Override
    public ElementMemento createMemento()
    {
        return saveState(new TclCommandMemento());
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSourceReferenceElement#saveState(com.soartech.soar.ide.core.model.impl.serialization.Mementos.Element)
     */
    @Override
    protected ElementMemento saveState(ElementMemento memento)
    {
        super.saveState(memento);
        
        TclCommandMemento cmdMemento = (TclCommandMemento) memento;
        
        cmdMemento.setCommandName(commandName);
        cmdMemento.setCommandNameRange(new SourceRangeMemento(commandNameRange));

        // Comment is saved automatically in Element.children
        
        return memento;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ITclCommand#getAssociatedComment()
     */
    public ITclComment getAssociatedComment()
    {
        return comment;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ITclCommand#getName()
     */
    public ISoarSourceRange getCommandNameRange()
    {
        return commandNameRange;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ITclCommand#getTclSyntaxTree()
     */
    public TclAstNode getTclSyntaxTree()
    {
        return astNode;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getCorrespondingResource()
     */
    public IResource getCorrespondingResource()
    {
        // No resource for an individual command
        return null;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarSourceRange#getLength()
     */
    public int getLength()
    {
        return astNode.getLength();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarSourceRange#getOffset()
     */
    public int getOffset()
    {
        return astNode.getStart();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ITclCommand#getCommandName()
     */
    public String getCommandName()
    {
        return commandName;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#shouldFireEvents()
     */
    @Override
    protected boolean shouldFireEvents()
    {
        // Only fire events if our owning soarFile fires events
        return soarFile != null && soarFile.shouldFireEvents();
    }
    
    
}
