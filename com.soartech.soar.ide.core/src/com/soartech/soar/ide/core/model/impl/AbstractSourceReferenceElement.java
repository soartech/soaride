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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import com.soartech.soar.ide.core.model.BasicSoarSourceRange;
import com.soartech.soar.ide.core.model.ISoarBuffer;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarOpenable;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.ISoarSourceReference;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarProblem;
import com.soartech.soar.ide.core.model.impl.serialization.ElementMemento;
import com.soartech.soar.ide.core.model.impl.serialization.SourceRangeMemento;
import com.soartech.soar.ide.core.model.impl.serialization.SourceReferenceMemento;

/**
 * @author ray
 */
public abstract class AbstractSourceReferenceElement extends AbstractSoarElement 
    implements ISoarSourceReference
{
    BasicSoarSourceRange range;
    boolean errors;
    boolean warnings;
    String expandedSource = null;
    
    /**
     * @param parent
     */
    public AbstractSourceReferenceElement(AbstractSoarElement parent)
    {
        super(parent);
    }
    
    /**
     * @param parent
     */
    public AbstractSourceReferenceElement(AbstractSoarElement parent, String expandedSource)
    {
        super(parent);
        this.expandedSource = expandedSource;
    }
    
    public AbstractSourceReferenceElement(AbstractSoarElement parent, SourceReferenceMemento memento) throws SoarModelException
    {
        super(parent, memento);
        
        this.range = new BasicSoarSourceRange(memento.getRange());
    }
    
    protected void setSourceRange(BasicSoarSourceRange range)
    {
        this.range = range;
    }
    
    public ISoarOpenable getOpenableParent()
    {
        ISoarElement current = getParent();
        while(current != null)
        {
            if(current instanceof ISoarOpenable)
            {
                return (ISoarOpenable) current;
            }
            current = current.getParent();
        }
        return null;
    }

    public String getSource(ISoarSourceRange range) throws SoarModelException
    {
        if(expandedSource != null)
        {
            int start = range.getOffset();
            int end = range.getOffset() + range.getLength();
            return expandedSource.substring(start, end);
        }
        
        ISoarOpenable openable = getOpenableParent();
        ISoarBuffer buffer = openable.getBuffer();
        if(buffer == null)
        {
            return null;
        }
        
        return buffer.getText(range.getOffset(), range.getLength());
    }
    
    /**
     * Reset the current cached problem states for this object
     */
    public void resetProblems()
    {
        errors = false;
        warnings = false;
    }
    
    /**
     * Update the cached problem states for this object based on the marker 
     * passed.
     * 
     * @param marker The problem marker
     * @param start The CHAR_START attribute of the marker
     * @param severity The SEVERITY attribute of the marker
     * @throws SoarModelException
     */
    public void updateProblems(SoarProblem problem) throws SoarModelException
    {
        if(range.contains(problem.start))
        {
           if(problem.severity == IMarker.SEVERITY_ERROR)
           {
               errors = true;
           }
           else if(problem.severity == IMarker.SEVERITY_WARNING)
           {
               warnings = true;
           }
        }
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#createMemento()
     */
    @Override
    public ElementMemento createMemento()
    {
        return saveState(new SourceReferenceMemento());
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#saveState(com.soartech.soar.ide.core.model.impl.serialization.Mementos.Element)
     */
    @Override
    protected ElementMemento saveState(ElementMemento memento)
    {
        SourceReferenceMemento refMemento = (SourceReferenceMemento) memento;
        
        super.saveState(memento);
        
        refMemento.setRange(new SourceRangeMemento(range));
        
        return memento;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getContainingResource()
     */
    public IResource getContainingResource()
    {
        return getParent().getContainingResource();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarElement#getPath()
     */
    public IPath getPath()
    {
        return getParent().getPath();
    }

    @Override
    public boolean hasErrors()
    {
        return errors;
    }

    @Override
    public boolean hasWarnings()
    {
        return warnings;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarSourceReference#getSource()
     */
    public String getSource() throws SoarModelException
    {
        if(expandedSource != null)
        {
            return expandedSource;
        }
        
        ISoarOpenable openable = getOpenableParent();
        ISoarBuffer buffer = openable.getBuffer();
        if(buffer == null)
        {
            return null;
        }
        
        return buffer.getText(range.getOffset(), range.getLength());
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarSourceReference#getSourceRange()
     */
    public ISoarSourceRange getSourceRange() throws SoarModelException
    {
        return range;
    }
}
