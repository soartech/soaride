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

import org.eclipse.core.resources.IResource;

import com.soartech.soar.ide.core.model.BasicSoarSourceRange;
import com.soartech.soar.ide.core.model.ITclComment;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.impl.serialization.ElementMemento;
import com.soartech.soar.ide.core.model.impl.serialization.TclCommentMemento;
import com.soartech.soar.ide.core.tcl.TclAstNode;

/**
 * @author ray
 */
public class TclComment extends AbstractSourceReferenceElement implements
        ITclComment
{
    /**
     * @param parent
     */
    public TclComment(TclCommand parent, TclAstNode commentNode)
    {
        super(parent);
        setSourceRange(new BasicSoarSourceRange(commentNode));
    }

    public TclComment(TclCommand parent, TclCommentMemento memento) throws SoarModelException
    {
        super(parent, memento);
        
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSourceReferenceElement#createMemento()
     */
    @Override
    public ElementMemento createMemento()
    {
        return saveState(new TclCommentMemento());
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSourceReferenceElement#saveState(com.soartech.soar.ide.core.model.impl.serialization.Mementos.Element)
     */
    @Override
    protected ElementMemento saveState(ElementMemento memento)
    {
        return super.saveState(memento);
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ITclComment#getCommentText()
     */
    public String getCommentText() throws SoarModelException
    {
        // TODO: Remove # characters?
        return getSource();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getCorrespondingResource()
     */
    public IResource getCorrespondingResource()
    {
        // No resource for an individual comment
        return null;
    }

}
