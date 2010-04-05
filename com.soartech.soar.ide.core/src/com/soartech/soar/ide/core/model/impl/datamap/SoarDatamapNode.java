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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamap;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapNode;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapValue;
import com.soartech.soar.ide.core.model.impl.SoarProject;
import com.soartech.soar.ide.core.model.impl.serialization.DatamapNodeMemento;
import com.soartech.soar.ide.core.model.impl.serialization.DatamapProductionReference;
import com.soartech.soar.ide.core.model.impl.serialization.DatamapValueMemento;
import com.soartech.soar.ide.core.model.impl.serialization.SoarDeserializationException;

/**
 * @author ray
 */
public class SoarDatamapNode implements ISoarDatamapNode
{
    private SoarDatamap datamap;
    private char idChar;
    private Map<String, ISoarDatamapAttribute> attributes;
    private Map<String, ISoarDatamapValue> values;
    
    /**
     * @param datamap
     */
    public SoarDatamapNode(SoarDatamap datamap, char idChar)
    {
        this.datamap = datamap;
        this.idChar = idChar;
    }
    
    public static ISoarDatamapNode createFromMemento(SoarProject project, SoarDatamap datamap,
            DatamapNodeMemento memento,
            Map<DatamapNodeMemento, ISoarDatamapNode> nodeMap) throws SoarDeserializationException
    {
        ISoarDatamapNode node = nodeMap.get(memento);
        if(node == null)
        {
            node = new SoarDatamapNode(project, datamap, memento, nodeMap);
        }
        return node;
    }
    
    SoarDatamapNode(SoarProject project, SoarDatamap datamap,
            DatamapNodeMemento memento,
            Map<DatamapNodeMemento, ISoarDatamapNode> nodeMap) throws SoarDeserializationException
    {
        this.datamap = datamap;
        
        if (memento == null)
        {
            throw new SoarDeserializationException("Invalid memento");
        }

        values = new HashMap<String, ISoarDatamapValue>();
        for (DatamapValueMemento value : memento.getValues())
        {
            values.put(value.getName(), new SoarDatamapValue(project, value));
        }
        
        nodeMap.put(memento, this);
    }
    
    DatamapNodeMemento createMemento(Map<ISoarDatamapNode, DatamapNodeMemento> nodeMap)
    {
        DatamapNodeMemento memento = nodeMap.get(this);
        
        if (memento != null) { return memento; }
        memento = new DatamapNodeMemento();

        if (values != null)
        {
            DatamapValueMemento valueMementos[] = new DatamapValueMemento[values.size()];

            int i = 0;
            for (Map.Entry<String, ISoarDatamapValue> e : values.entrySet())
            {
                Set<ISoarProduction> supporting = e.getValue().getSupportingProductions();
                DatamapProductionReference[] prodRefs = new DatamapProductionReference[supporting.size()];
                
                int j = 0;
                for (ISoarProduction production : supporting)
                {
                    // TODO: pass in the real usage
                    prodRefs[j++] = new DatamapProductionReference(production, 0);
                }
                
                DatamapValueMemento valueMemento = new DatamapValueMemento();
                valueMemento.setName(e.getKey());
                valueMemento.setProductions(prodRefs);
                valueMementos[i++] = valueMemento;
            }
            
            memento.setValues(valueMementos);
        }
        nodeMap.put(this, memento);
        
        return memento;
    }
    
    void addAttribute(ISoarDatamapAttribute attr)
    {
        if(attributes == null)
        {
            attributes = new HashMap<String, ISoarDatamapAttribute>();
        }
        attributes.put(attr.getName(), attr);
    }
    
    void removeAttribute(ISoarDatamapAttribute attr)
    {
        if(attributes == null)
        {
            return;
        }
        ISoarDatamapAttribute removed = attributes.remove(attr.getName());
        assert removed == attr;
    }
    
    @Override
    public ISoarDatamapValue addValue(ISoarProduction production, String value)
    {
        if(values == null)
        {
            values = new HashMap<String, ISoarDatamapValue>();
        }
        SoarDatamapValue v = (SoarDatamapValue) values.get(value);
        if(v == null)
        {
            v = new SoarDatamapValue(value);
            values.put(value, v);
        }
        if (production != null)
        {
        	v.addProduction(production);
        }
        return v; 
    }
    
    void removeValue(ISoarProduction production)
    {
        if(values == null)
        {
            return;
        }
        Iterator<ISoarDatamapValue> it = values.values().iterator();
        while(it.hasNext())
        {
            SoarDatamapValue v = (SoarDatamapValue) it.next();
            if(!v.removeProduction(production))
            {
                it.remove();
            }
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapNode#getDatamap()
     */
    public ISoarDatamap getDatamap()
    {
        return datamap;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapNode#getAttribute(java.lang.String)
     */
    public ISoarDatamapAttribute getAttribute(String name)
    {
        synchronized(datamap.getLock())
        {
            return attributes != null ? attributes.get(name) : null;
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapNode#getAttributes()
     */
    public Set<ISoarDatamapAttribute> getAttributes()
    {
        synchronized(datamap.getLock())
        {
            if(attributes == null)
            {
                return Collections.emptySet();
            }
            return new HashSet<ISoarDatamapAttribute>(attributes.values());
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapNode#getElements(java.lang.String[], int, boolean)
     */
    public Set<ISoarDatamapAttribute> getAttributes(String[] path, int offset, boolean includeVariablized)
    {
        Set<ISoarDatamapAttribute> set = new HashSet<ISoarDatamapAttribute>();
        synchronized(datamap.getLock())
        {
            getAttributesHelper(path, offset, includeVariablized, set);
        }
        return set;
    }
    
    private void getAttributesHelper(String[] path, int offset,
            boolean includeVariablized, Set<ISoarDatamapAttribute> set)
    {
        boolean atEnd = offset + 1 >= path.length;

        String part = path[offset];
        if (part != null)
        {
            SoarDatamapAttribute child = (SoarDatamapAttribute) getAttribute(part);
            if (child != null)
            {
                if(atEnd)
                {
                    set.add(child);
                }
                else
                {
                    child.getInternalTarget().getAttributesHelper(path, offset + 1, includeVariablized, set);
                }
            }
            if (includeVariablized)
            {
                // Look for the variablized child (name = null)
                child = (SoarDatamapAttribute) getAttribute(null);
                if (child != null)
                {
                    if(atEnd)
                    {
                        set.add(child);
                    }
                    else
                    {
                        child.getInternalTarget().getAttributesHelper(path, offset + 1, includeVariablized, set);
                    }
                }
            }
        }
        else if(atEnd)
        {
            set.addAll(getAttributes());
        }
        else
        {
            for (ISoarDatamapAttribute ichild : getAttributes())
            {
                SoarDatamapAttribute child = (SoarDatamapAttribute) ichild;
                child.getInternalTarget().getAttributesHelper(path, offset + 1, includeVariablized, set);
            }
        }
    }
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapNode#hasAttributes()
     */
    public boolean hasAttributes()
    {
        synchronized(datamap.getLock())
        {
            return attributes != null && !attributes.isEmpty();
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapNode#isState()
     */
    public boolean isState()
    {
        return this == datamap.getState();
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapNode#getValues()
     */
    public Set<ISoarDatamapValue> getValues()
    {
        synchronized(datamap.getLock())
        {
            if(values == null)
            {
                return Collections.emptySet();
            }
            return new HashSet<ISoarDatamapValue>(values.values());
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return Character.toString(idChar);
    }
    
    @Override
    public void clearValues()
    {
    	if (values != null)
    	{
    		values.clear();
    	}
    }

}
