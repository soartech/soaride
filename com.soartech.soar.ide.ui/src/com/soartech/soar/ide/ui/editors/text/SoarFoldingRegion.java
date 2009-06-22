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
package com.soartech.soar.ide.ui.editors.text;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.soartech.soar.ide.core.model.BasicSoarSourceRange;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.ISoarSourceReference;
import com.soartech.soar.ide.core.model.SoarModelException;

/**
 * @author ray
 */
public class SoarFoldingRegion implements ISoarSourceReference
{
    private IDocument document;
    private String name;
    private ISoarSourceRange range;
    
    public SoarFoldingRegion(IDocument document, String name, int offset, int length)
    {
        this.document = document;
        this.name = name.trim();
        this.range = new BasicSoarSourceRange(offset, length);
    }
    
    /**
     * @return The name of the region
     */
    public String getName()
    {
        return name;
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarSourceReference#getSource()
     */
    public String getSource() throws SoarModelException
    {
        try
        {
            return document.get(range.getOffset(), range.getLength());
        }
        catch (BadLocationException e)
        {
            return "";
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarSourceReference#getSourceRange()
     */
    public ISoarSourceRange getSourceRange() throws SoarModelException
    {
        return range;
    }

}
