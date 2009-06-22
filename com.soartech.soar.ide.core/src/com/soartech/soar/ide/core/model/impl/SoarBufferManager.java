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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import com.soartech.soar.ide.core.model.ISoarBuffer;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarOpenable;

/**
 * Manages all open buffers for non-working copy Soar model elements. 
 * Implements a most-recently-used (MRU) policy where only a fixed number of 
 * buffers are in memory at any given time. Older buffers are closed. They 
 * will be lazily opened again when then are needed.
 * 
 * @author ray
 */
public class SoarBufferManager
{
    private static boolean DEBUG = false;
    
    /**
     * The maximum number of buffers open at any time.
     */
    private static final int MAX_BUFFERS = 20;
    
    private List<ISoarBuffer> buffers = Collections.synchronizedList(new LinkedList<ISoarBuffer>());
    
    /**
     * Create a new default buffer for the given owner. The buffer is not added
     * to the buffer manager.
     * 
     * @param owner The owner of the new buffer
     * @return The new buffer, never <code>null</code>
     */
    public ISoarBuffer createBuffer(ISoarOpenable owner)
    {
        synchronized(buffers)
        {
            if(DEBUG) { System.out.println("Creating default buffer for element " + owner); }
            ISoarElement element = (ISoarElement) owner;
            IResource resource = element.getCorrespondingResource();
            
            return new SoarBuffer(resource instanceof IFile ? (IFile) resource : null, 
                                  owner);
        }
    }
    
    /**
     * Add the given buffer to the manager. If the buffer is already registered
     * this is a noop.
     * 
     * @param buffer The buffer to add
     */
    public void addBuffer(ISoarBuffer buffer)
    {
        // TODO: If the buffer is not already in the manager and there are more
        // than N buffers in the manager, take the oldest buffer and remove
        // it by calling ISoarBuffer.close()
        
        synchronized(buffers)
        {
            if(!buffers.contains(buffer))
            {
                buffers.add(0, buffer);
                if(DEBUG) { System.out.println("Added buffer for element " + buffer.getOwner() + " " + getUsage()); }
            }
            if(buffers.size() > MAX_BUFFERS)
            {
                ISoarBuffer oldBuffer = buffers.remove(buffers.size() - 1);
                if(DEBUG) { System.out.println("Removing old buffer for element " + oldBuffer.getOwner() + " " + getUsage()); }
                
                oldBuffer.close();
            }
        }
    }
    
    /**
     * Remove the given buffer from the manager
     * 
     * @param buffer The buffer to remove
     */
    void removeBuffer(ISoarBuffer buffer)
    {
        if(buffers.remove(buffer))
        {
            if(DEBUG) { System.out.println("Removed buffer for element " + buffer.getOwner() + " " + getUsage()); }
        }
    }
    
    private String getUsage()
    {
        return "(" + buffers.size() + "/" + MAX_BUFFERS + ")";
    }
}
