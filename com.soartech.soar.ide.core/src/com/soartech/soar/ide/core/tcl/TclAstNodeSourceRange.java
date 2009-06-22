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
package com.soartech.soar.ide.core.tcl;

import com.soartech.soar.ide.core.model.ISoarSourceRange;

/**
 * @author ray
 */
public class TclAstNodeSourceRange implements ISoarSourceRange
{
    private TclAstNode node;
    
    /**
     * @param node
     * @param buffer
     */
    public TclAstNodeSourceRange(TclAstNode node)
    {
        super();
        this.node = node;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarSourceRange#getLength()
     */
    public int getLength()
    {
        return node.getLength();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarSourceRange#getOffset()
     */
    public int getOffset()
    {
        return node.getStart();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarSourceRange#getEnd()
     */
    public int getEnd()
    {
        return getOffset() + getLength();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarSourceRange#contains(int)
     */
    public boolean contains(int offset)
    {
        return offset >= getOffset() && offset < getEnd();
    }

}
