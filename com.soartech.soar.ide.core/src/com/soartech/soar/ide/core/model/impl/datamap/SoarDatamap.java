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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamap;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapListener;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapNode;
import com.soartech.soar.ide.core.model.datamap.SoarDatamapAdditionResult;
import com.soartech.soar.ide.core.model.datamap.SoarDatamapEvent;
import com.soartech.soar.ide.core.model.impl.SoarProject;
import com.soartech.soar.ide.core.model.impl.serialization.DatamapAttributeMemento;
import com.soartech.soar.ide.core.model.impl.serialization.DatamapMemento;
import com.soartech.soar.ide.core.model.impl.serialization.DatamapNodeMemento;
import com.soartech.soar.ide.core.model.impl.serialization.SoarDeserializationException;

/**
 * Implementation of {@link ISoarDatamap} interface
 * 
 * @author ray
 */
public class SoarDatamap implements ISoarDatamap
{
    private List<ISoarDatamapListener> listeners = new ArrayList<ISoarDatamapListener>();
    private SoarDatamapNode state;
    private int modificationLevel = 0;
    private SoarDatamapEvent modificationEvent;
    private Set<SoarDatamapAttribute> attributes = new HashSet<SoarDatamapAttribute>();
    private Object lock = new Object();
    
    private ISoarAgent agent = null;
    
    /**
     * Construct a new, empty datamap 
     */
    public SoarDatamap()
    {
        this.state = new SoarDatamapNode(this, 'S');
    }
    
    /**
     * Construct a datamap from the given memento.
     * 
     * @param project The owning project
     * @param memento The memento to read from
     * @return true on success, false if there was a failure indicating that a
     *      full rebuild of the project is necessary
     */
    public boolean deserialize(SoarProject project, DatamapMemento memento)
    {
        synchronized(getLock())
        {
            clear();
        
            beginModification();
            try
            {
                // Build up a map from node mementos to actual nodes
                Map<DatamapNodeMemento, ISoarDatamapNode> nodeMap = 
                    new HashMap<DatamapNodeMemento, ISoarDatamapNode>();
                
                try
                {
                    for(DatamapNodeMemento mementoNode : memento.getNodes())
                    {
                        SoarDatamapNode.createFromMemento(project, this, mementoNode, nodeMap);
                    }
                    
                    this.state = (SoarDatamapNode)SoarDatamapNode.createFromMemento(project, this, memento.getState(), nodeMap);
                    
                    if(state == null)
                    {
                        return false;
                    }
                    
                    // Deserialize all the attributes.
                    for(DatamapAttributeMemento attrMemento : memento.getAttributes())
                    {
                        // The attribute will add itself through onAttributeAdded()
                        new SoarDatamapAttribute(project, this, attrMemento, nodeMap);
                    }
                }                
                catch (SoarDeserializationException e)
                {
                    System.out.println("Failed to deserialize datamap: " + e);
                    return false;
                }

            }
            finally
            {
                endModification();
            }
        }
        return true;
    }
        
    /**
     * @return A new serializable memento representing this datamap
     */
    public DatamapMemento createMemento()
    {
        DatamapMemento memento = new DatamapMemento();
        synchronized(getLock())
        {
            Map<ISoarDatamapNode, DatamapNodeMemento> nodeMap = new HashMap<ISoarDatamapNode, DatamapNodeMemento>();
            
            memento.setState(state.createMemento(nodeMap));
            
            DatamapAttributeMemento[] attrs = new DatamapAttributeMemento[attributes.size()];
            
            int i = 0;
            for(ISoarDatamapAttribute a : attributes)
            {
               attrs[i++] = ((SoarDatamapAttribute) a).createMemento(nodeMap);
            }
            
            memento.setAttributes(attrs);
            memento.setNodes(nodeMap.values().toArray(DatamapNodeMemento.EMPTY_NODE_ARRAY));
        }
        return memento;
    }
    
    /**
     * Clear method used to completely clear out this datamap for reuse. 
     */
    public void clear()
    {
        synchronized(getLock())
        {
            assert modificationLevel == 0;
            
            SoarDatamapEvent e = new SoarDatamapEvent(this);
            e.removed.addAll(attributes);
            
            state = new SoarDatamapNode(this, 'S');
            attributes.clear();
            
            fireEvent(e);
        }
    }
    
    private void fireEvent(SoarDatamapEvent e)
    {
        // Don't bother firing an event if nothing happened.
        if(e.added.isEmpty() && e.removed.isEmpty() && e.modified.isEmpty())
        {
            return;
        }
        
        for(ISoarDatamapListener listener : new ArrayList<ISoarDatamapListener>(listeners))
        {
            listener.onDatamapChanged(e);
        }
    }
    
    /**
     * Called when the given element is added to the datamap so that events can
     * be fired correctly.
     * 
     * @param e The added element
     */
    void onAttributeAdded(SoarDatamapAttribute e)
    {
        attributes.add(e);
        if(isInModification())
        {
            modificationEvent.added.add(e);
        }
        else
        {
            SoarDatamapEvent event = new SoarDatamapEvent(this);
            event.added.add(e);
            fireEvent(event);
        }
    }
    
    /**
     * Called when the given element is removed from the datamap so that events can
     * be fired correctly.
     * 
     * @param e The added element
     */
    void onAttributeRemoved(SoarDatamapAttribute e)
    {
        attributes.remove(e);
        if(isInModification())
        {
            modificationEvent.removed.add(e);
        }
        else
        {
            SoarDatamapEvent event = new SoarDatamapEvent(this);
            event.removed.add(e);
            fireEvent(event);
        }
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamap#getLock()
     */
    public Object getLock()
    {
        return lock;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamap#getState()
     */
    public ISoarDatamapNode getState()
    {
        return state;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamap#addListener(com.soartech.soar.ide.core.model.datamap.ISoarDatamapListener)
     */
    public void addListener(ISoarDatamapListener listener)
    {
        synchronized(getLock())
        {
            if(!listeners.contains(listener))
            {
                listeners.add(listener);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamap#removeListener(com.soartech.soar.ide.core.model.datamap.ISoarDatamapListener)
     */
    public void removeListener(ISoarDatamapListener listener)
    {
        synchronized(getLock())
        {
            listeners.remove(listener);
        }
    }


    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamap#addProduction(com.soartech.soar.ide.core.model.ISoarProduction)
     */
    public SoarDatamapAdditionResult addProduction(ISoarProduction p)
    {
        synchronized(getLock())
        {
            DatamapBuilder builder = new DatamapBuilder();
            return builder.addProduction(this, p);
        }
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamap#removeProduction(com.soartech.soar.ide.core.model.ISoarProduction)
     */
    public void removeProduction(ISoarProduction p)
    {
        synchronized(getLock())
        {
            beginModification();
            try
            {
                Iterator<SoarDatamapAttribute> it = attributes.iterator();
                while(it.hasNext())
                {
                    SoarDatamapAttribute a = it.next();
                    // Remove the production from the attribute, disabling notification
                    // to avoid concurrent modification of the set we're iterating
                    // over.
                    if(a.removeSupportingProduction(p, false))
                    {
                        modificationEvent.removed.add(a);
                        it.remove(); 
                    }
                }
            }
            finally
            {
                endModification();
            }
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamap#beginModification()
     */
    public void beginModification()
    {
        ++modificationLevel;
        if(modificationEvent == null)
        {
            assert modificationLevel == 1;
            modificationEvent = new SoarDatamapEvent(this);
        }

    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamap#isInModification()
     */
    public boolean isInModification()
    {
        synchronized(getLock())
        {
            return modificationLevel > 0;
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamap#endModification()
     */
    public void endModification()
    {
        --modificationLevel;
        if(modificationLevel < 0)
        {
            throw new IllegalStateException("Too many calls to endModification()");
        }
        
        if(modificationLevel == 0)
        {
            assert modificationEvent != null;
            fireEvent(modificationEvent);
            modificationEvent = null;
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamap#getElements(java.lang.String[], boolean)
     */
    public Set<ISoarDatamapAttribute> getElements(String[] path,
            boolean includeVariablized)
    {
        return state.getAttributes(path, 0, includeVariablized);
    }

    private static PrintWriter tabs(PrintWriter out, int depth)
    {
        for(int i = 0; i < depth; ++i)
        {
            out.print("   ");
        }
        return out;
    }
    
    private static void writeNode(ISoarDatamapNode node, PrintWriter out, int depth, Set<ISoarDatamapNode> visited)
    {
        if(visited.contains(node))
        {
            return;
        }
        visited.add(node);
        
        for(ISoarDatamapAttribute a : node.getAttributes())
        {
            tabs(out, depth).println(a);
            if(a.getTarget().hasAttributes())
            {
                writeNode(a.getTarget(), out, depth + 1, visited);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        
        writeNode(state, out, 0, new HashSet<ISoarDatamapNode>());
        
        return writer.toString();
    }

    public ISoarAgent getAgent()
    {
        return agent;
    }

    public void setAgent(ISoarAgent agent)
    {
        this.agent = agent;
    }
}
