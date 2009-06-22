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
package com.soartech.soar.ide.core.model.datamap;

import java.util.ArrayList;
import java.util.List;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.model.ISoarModelListener;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.SoarModelEvent;
import com.soartech.soar.ide.core.model.SoarModelException;

/**
 * Handles the details of listening for datamap events from all datamaps
 * in the soar model
 * 
 * @author ray
 */
public class CompositeSoarDatamapListener implements ISoarDatamapListener, ISoarModelListener
{
    private ISoarModel model;
    private List<ISoarDatamapListener> listeners = new ArrayList<ISoarDatamapListener>();
    
    public CompositeSoarDatamapListener(ISoarModel model)
    {
        this.model = model;
        model.addListener(this);
        try
        {
            for(ISoarProject project : model.getProjects())
            {
                for(ISoarAgent agent : project.getAgents())
                {
                    agent.getDatamap().addListener(this);
                }
            }
        }
        catch(SoarModelException e)
        {
            SoarCorePlugin.log(e);
        }
    }
    
    public synchronized void dispose()
    {
        this.model.removeListener(this);
        listeners.clear();
        
        try
        {
            for(ISoarProject project : model.getProjects())
            {
                for(ISoarAgent agent : project.getAgents())
                {
                    agent.getDatamap().addListener(this);
                }
            }
        } 
        catch(SoarModelException e)
        {
            SoarCorePlugin.log(e);
        }
    }
    
    public synchronized void addListener(ISoarDatamapListener listener)
    {
        listeners.add(listener);
    }
    
    public synchronized void removeListener(ISoarDatamapListener listener)
    {
        listeners.remove(listener);
    }
    
    private synchronized void fireEvent(SoarDatamapEvent event)
    {
        for(ISoarDatamapListener listener : new ArrayList<ISoarDatamapListener>(listeners))
        {
            listener.onDatamapChanged(event);
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.datamap.ISoarDatamapListener#onDatamapChanged(com.soartech.soar.ide.core.model.datamap.SoarDatamapEvent)
     */
    public void onDatamapChanged(SoarDatamapEvent event)
    {
        fireEvent(event);
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarModelListener#onEvent(com.soartech.soar.ide.core.model.SoarModelEvent)
     */
    public void onEvent(SoarModelEvent event)
    {
        if(event.getType() == SoarModelEvent.ELEMENTS_ADDED)
        {
            for(ISoarElement e : event.getElements())
            {
                if(e instanceof ISoarAgent)
                {
                    ((ISoarAgent) e).getDatamap().addListener(this);
                }
            }
        }
        else if(event.getType() == SoarModelEvent.ELEMENTS_REMOVED)
        {
            for(ISoarElement e : event.getElements())
            {
                if(e instanceof ISoarAgent)
                {
                    ((ISoarAgent) e).getDatamap().removeListener(this);
                }
            }
        }
    }
    
}
