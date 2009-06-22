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

import com.soartech.soar.ide.core.model.impl.serialization.SourceRangeMemento;
import com.soartech.soar.ide.core.tcl.TclAstNode;

/**
 * Basic implementation of {@link ISoarSourceRange} interface
 * 
 * @author ray
 */
public class BasicSoarSourceRange implements ISoarSourceRange
{
    private int offset;
    private int length;
    
    /**
     * @param offset
     * @param length
     */
    public BasicSoarSourceRange(int offset, int length)
    {
        this.offset = offset;
        this.length = length;
    }
    
    public BasicSoarSourceRange(TclAstNode tclAstNode)
    {
        this.offset = tclAstNode.getStart();
        this.length = tclAstNode.getLength();
    }
    
    public BasicSoarSourceRange(SourceRangeMemento memento)
    {
        this.offset = memento.getOffset();
        this.length = memento.getLength();
    }

    /**
     * Update method in support of {@link ISoarSourceReference#updateRanges(int, int)}
     * 
     * @param endOfAffectedArea
     * @param positionDelta
     */
    void update(int endOfAffectedArea, int positionDelta)
    {
        if(offset >= endOfAffectedArea)
        {
            offset += positionDelta;
        }
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarSourceRange#getLength()
     */
    public int getLength()
    {
        return length;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarSourceRange#getOffset()
     */
    public int getOffset()
    {
        return offset;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarSourceRange#getEnd()
     */
    public int getEnd()
    {
        return offset + length;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarSourceRange#contains(int)
     */
    public boolean contains(int offset)
    {
        return offset >= this.offset && offset < getEnd();
    }

}
