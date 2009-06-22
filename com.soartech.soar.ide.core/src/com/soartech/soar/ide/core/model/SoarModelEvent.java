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

import java.util.List;

/**
 * @author ray
 */
public class SoarModelEvent
{
    public static final int ELEMENTS_ADDED = 0;
    public static final int ELEMENTS_REMOVED = 1;
    public static final int ELEMENTS_CHANGED = 2;
    public static final int MAX_ELEMENT_TYPE = 3;
    
    private ISoarElement[] elements;
    private int type;

    public static SoarModelEvent createAdded(ISoarElement... elements)
    {
        return new SoarModelEvent(elements, ELEMENTS_ADDED);
    }
    
    public static SoarModelEvent createAdded(List<ISoarElement> elements)
    {
        return new SoarModelEvent(toArray(elements), ELEMENTS_ADDED);
    }
    
    public static SoarModelEvent createRemoved(ISoarElement... element)
    {
        return new SoarModelEvent(element, ELEMENTS_REMOVED);
    }
    
    public static SoarModelEvent createRemoved(List<ISoarElement> elements)
    {
        return new SoarModelEvent(toArray(elements), ELEMENTS_REMOVED);
    }
    
    public static SoarModelEvent createChanged(ISoarElement... element)
    {
        return new SoarModelEvent(element, ELEMENTS_CHANGED);
    }

    private static ISoarElement[] toArray(List<ISoarElement> elements)
    {
        return elements.toArray(new ISoarElement[elements.size()]);
    }
    /**
     * @return The affected elements
     */
    public ISoarElement[] getElements()
    {
        return elements;
    }

    /**
     * @return The type of event
     */
    public int getType()
    {
        return type;
    }

    public SoarModelEvent(ISoarElement[] element, int type)
    {
        this.elements = element;
        this.type = type;
    }
}
