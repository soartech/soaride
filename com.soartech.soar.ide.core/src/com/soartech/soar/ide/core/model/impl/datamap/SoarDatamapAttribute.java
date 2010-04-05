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
import java.util.Map;
import java.util.Set;

import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamap;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapNode;
import com.soartech.soar.ide.core.model.impl.SoarProject;
import com.soartech.soar.ide.core.model.impl.serialization.DatamapAttributeMemento;
import com.soartech.soar.ide.core.model.impl.serialization.DatamapNodeMemento;
import com.soartech.soar.ide.core.model.impl.serialization.DatamapProductionReference;
import com.soartech.soar.ide.core.model.impl.serialization.SoarDeserializationException;

/**
 * Implementation of the {@link ISoarDatamapAttribute} interface.
 * 
 * @author ray
 */
public class SoarDatamapAttribute implements ISoarDatamapAttribute
{
    private SoarDatamap datamap;
    private String name;
    private SoarDatamapNode source;
    private SoarDatamapNode target;
    private boolean persistent;
    private Map<ISoarProduction, ProductionInfo> productions;
    
    /**
     * Construct a new attribute. Used only by the datamap.
     * 
     * @param datamap Owning datamap
     * @param name The name of the attribute
     * @param source The source node, never null
     * @param target The target node, never null
     */
    SoarDatamapAttribute(SoarDatamap datamap, String name, 
                                SoarDatamapNode source, 
                                SoarDatamapNode target)
    {
        assert datamap != null;
        assert source != null;
        assert target != null;
        
        this.datamap = datamap;
        this.name = name;
        this.source = source;
		this.target = target;

		source.addAttribute(this);
		datamap.onAttributeAdded(this);
	}
    
    SoarDatamapAttribute(SoarProject project, 
                         SoarDatamap datamap, DatamapAttributeMemento memento,
                         Map<DatamapNodeMemento, ISoarDatamapNode> nodeMap) throws SoarDeserializationException
    {
        this.datamap = datamap;
        this.name = memento.getName(); // may be null for variablized attributes
        this.source = (SoarDatamapNode) nodeMap.get(memento.getSource());
        this.target = (SoarDatamapNode) nodeMap.get(memento.getTarget());
        
        if(source == null || target == null)
        {
            throw new SoarDeserializationException("Invalid source, or target");
        }
        
        this.persistent = memento.isPersistent();
        
        if(memento.getProductions().length > 0)
        {
            productions = new HashMap<ISoarProduction, ProductionInfo>();
            for(DatamapProductionReference pr : memento.getProductions())
            {
                ISoarProduction p = pr.lookupProduction(project);
                if(p != null)
                {
                    ProductionInfo info = new ProductionInfo();
                    info.usage = pr.getUsage();
                    productions.put(p, info);
                }
                else
                {
                    throw new SoarDeserializationException("Couldn't find production " + pr);
                }
            }
        }

		source.addAttribute(this);
		datamap.onAttributeAdded(this);
	}
    
    DatamapAttributeMemento createMemento(Map<ISoarDatamapNode, DatamapNodeMemento> nodeMap)
    {
        DatamapAttributeMemento memento = new DatamapAttributeMemento();
        
        memento.setName(name);
        memento.setPersistent(persistent);
        memento.setSource(source.createMemento(nodeMap));
        memento.setTarget(target.createMemento(nodeMap));
        
        DatamapProductionReference prods[] = new DatamapProductionReference[productions.size()];
        
        if(productions != null)
        {
            int i = 0;
            for(Map.Entry<ISoarProduction, ProductionInfo> e : productions.entrySet())
            {
                prods[i++] = new DatamapProductionReference(e.getKey(), e.getValue().usage);
            }
            
            memento.setProductions(prods);
        }
        
        return memento;
    }
    
    SoarDatamapNode getInternalTarget()
    {
        return target;
    }
    
    private void remove(boolean notify)
    {
        source.removeAttribute(this);
        if(notify)
        {
            datamap.onAttributeRemoved(this);
        }
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute#getDatamap()
     */
    public ISoarDatamap getDatamap()
    {
        return datamap;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute#getName()
     */
    public String getName()
    {
        return name;
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute#getSource()
     */
    public ISoarDatamapNode getSource()
    {
        return source;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute#getTarget()
     */
    public ISoarDatamapNode getTarget()
    {
        return target;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute#isVariablized()
     */
    public boolean isVariablized()
    {
        return name == null;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute#getOverallUsage()
     */
    public int getOverallUsage()
    {
        synchronized(datamap.getLock())
        {
            int usage = 0;
            if(productions == null)
            {
                return usage;
            }
            
            for(ProductionInfo info : productions.values())
            {
                usage |= info.usage;
            }
            return usage;
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute#getSupportingProductionUsage(com.soartech.soar.ide.core.model.ISoarProduction)
     */
    public int getSupportingProductionUsage(ISoarProduction p)
    {
        synchronized(datamap.getLock())
        {
            if(productions == null)
            {
                return 0;
            }
            
            ProductionInfo info = productions.get(p);
            
            return info != null ? info.usage : 0;
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute#getSupportingProductions()
     */
    public Set<ISoarProduction> getSupportingProductions()
    {
        synchronized(datamap.getLock())
        {
            if(productions == null)
            {
                return Collections.emptySet();
            }
            return new HashSet<ISoarProduction>(productions.keySet());
        }
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute#addSupportingProduction(com.soartech.soar.ide.core.model.ISoarProduction, int)
     */
    public void addSupportingProduction(ISoarProduction p, int usage)
    {
        synchronized(datamap.getLock())
        {
            if(productions == null)
            {
                productions = new HashMap<ISoarProduction, ProductionInfo>();
            }
            ProductionInfo info = productions.get(p);
            if(info != null)
            {
                info.usage |= usage;
            }
            else
            {
                info = new ProductionInfo();
                info.usage = usage;
                productions.put(p, info);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute#removeSupportingProduction(com.soartech.soar.ide.core.model.ISoarProduction)
     */
    public void removeSupportingProduction(ISoarProduction p)
    {
        removeSupportingProduction(p, true);
    }
    
    /**
     * Internal version of removeSupportingProduction. This should only be 
     * called by removeSupportingProduction and SoarDatamap.removeProduction.
     * 
     * @param p The production to remove
     * @param notify If true, the datamap is notified of the attribute's removal
     * @return True if the attribute was removed because of the production's 
     *      removal.
     */
    boolean removeSupportingProduction(ISoarProduction p, boolean notify)
    {
        synchronized(datamap.getLock())
        {
            // Remove any values in the target node contributed by this production
            target.removeValue(p);
            
            if(productions == null)
            {
                return false;
            }
            
            // Look up abd remove production info
            ProductionInfo info = productions.remove(p);
            if(info == null)
            {
                return false;
            }
            
            if(!isPersistent() && productions.isEmpty())
            {
                remove(notify);
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute#isPersistent()
     */
    public boolean isPersistent()
    {
        return persistent;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute#setPersistent(boolean)
     */
    public void setPersistent(boolean persistent)
    {
        synchronized(datamap.getLock())
        {
            if(this.persistent == persistent)
            {
                return;
            }
            
            // If it's becoming non-persistent and there are no supporting 
            // productions, remove it from the data map
            if(!persistent && getSupportingProductions().isEmpty())
            {
                remove(true);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "(" + source + " - " + name + " ->" + target + ")";
    }

    private static class ProductionInfo
    {
        int usage;
    }

}
