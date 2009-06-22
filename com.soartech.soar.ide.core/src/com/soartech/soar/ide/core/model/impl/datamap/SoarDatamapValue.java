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
package com.soartech.soar.ide.core.model.impl.datamap;

import java.util.HashSet;
import java.util.Set;

import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapValue;
import com.soartech.soar.ide.core.model.impl.SoarProject;
import com.soartech.soar.ide.core.model.impl.serialization.DatamapProductionReference;
import com.soartech.soar.ide.core.model.impl.serialization.DatamapValueMemento;
import com.soartech.soar.ide.core.model.impl.serialization.SoarDeserializationException;

/**
 * Very basic implementation of ISoarDatamapValue that simply stores a string 
 * and supporting productions
 * 
 * @author ray
 */
class SoarDatamapValue implements ISoarDatamapValue
{
    private Set<ISoarProduction> productions = new HashSet<ISoarProduction>();
    private String value;
    
    /**
     * Construct a value with the given string
     * 
     * @param value The value as a string
     */
    public SoarDatamapValue(String value)
    {
        this.value = value;        
    }
        
    public SoarDatamapValue(SoarProject project, DatamapValueMemento memento) throws SoarDeserializationException
    {
        this.value = memento.getName();
        
        for(DatamapProductionReference pr : memento.getProductions())
        {
            ISoarProduction p = pr.lookupProduction(project);
            if (p != null)
            {
                productions.add(p);
            }
            else
            {
                throw new SoarDeserializationException("Couldn't find production " + pr);
            }
        }
    }
    
    /**
     * Add a supporting production to this value
     * 
     * @param p The production
     */
    public void addProduction(ISoarProduction p)
    {
        productions.add(p);
    }
    
    /**
     * Remove a supporting produciton from this value
     * 
     * @param p The production to remove
     * @return false if no more supporting productions remain
     */
    public boolean removeProduction(ISoarProduction p)
    {
        productions.remove(p);
        return !productions.isEmpty();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return value;
    }

    public Set<ISoarProduction> getSupportingProductions()
    {
        return productions;
    }
}
