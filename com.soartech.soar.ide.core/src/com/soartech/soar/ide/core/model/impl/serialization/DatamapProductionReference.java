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
package com.soartech.soar.ide.core.model.impl.serialization;

import java.io.Serializable;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarFileAgentProxy;
import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.SoarModelTools;
import com.soartech.soar.ide.core.model.impl.SoarProject;

/**
 * Serializable object representing a reference to a production
 * 
 * @author ray
 */
@SuppressWarnings("serial")
public class DatamapProductionReference implements Serializable
{
    public static final DatamapProductionReference[] EMPTY_PRODUCTION_ARRAY = {};
    
    private String file;
    private String agent;
    private String name;
    private int offset;
    private int usage;
    
    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }
    public String getAgent() { return agent; }
    public void setAgent(String agent) { this.agent = agent; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }
    public int getUsage() { return usage; }
    public void setUsage(int usage) { this.usage = usage; }

    
    public DatamapProductionReference(ISoarProduction production, int usage)
    {
        setName(production.getProductionName());
        setFile(production.getPath().toPortableString());
        
        final ISoarFileAgentProxy agentProxy = (ISoarFileAgentProxy) production.getParent();
        if(agentProxy != null)
        {
            ISoarAgent agent = agentProxy.getAgent();
            if(agent != null)
            {
                setAgent(agent.getPath().toPortableString());
            }
        }
        try
        {
            setOffset(production.getSourceRange().getOffset());
        }
        catch (SoarModelException e1)
        {
            SoarCorePlugin.log(e1);
        }
        setUsage(usage);
    }
    
    /**
     * Look up the production referenced by this object in the given project.
     * 
     * @param project The project
     * @return The production, or null if not found
     * @throws SoarModelException
     */
    public ISoarProduction lookupProduction(SoarProject project)
    {
        if(file == null || agent == null || name == null)
        {
            return null;
        }
        
        IResource resource = SoarModelTools.getEclipseResource(new Path(file));
        if(resource == null || !(resource instanceof IFile))
        {
            return null;
        }
        
        try
        {
            ISoarFileAgentProxy proxy = getAgentProxy(project, (IFile) resource);
            if(proxy == null)
            {
                return null;
            }
            
            for(ISoarProduction p : proxy.getProductions())
            {
                if(name.equals(p.getProductionName()) &&
                   p.getSourceRange().getOffset() == offset)
                {
                    return p;
                }
            }
        }
        catch(SoarModelException e)
        {
            return null;
        }
        
        return null;
    }
    
    ISoarFileAgentProxy getAgentProxy(SoarProject project, IFile file) throws SoarModelException
    {
        ISoarFile soarFile = project.getSoarFile(file);
        if(soarFile == null)
        {
            return null;
        }
        
        for(ISoarFileAgentProxy proxy : soarFile.getAgentProxies())
        {
            if(agent.equals(proxy.getAgent().getPath().toPortableString()))
            {
                return proxy;
            }
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return file + ":" + agent + ":" + name + ":" + offset;
    }
    
    
}
