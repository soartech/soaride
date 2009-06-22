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

import org.eclipse.core.runtime.IProgressMonitor;

import com.soartech.soar.ide.core.model.ISoarBuffer;
import com.soartech.soar.ide.core.model.ISoarBufferChangedListener;
import com.soartech.soar.ide.core.model.ISoarOpenable;
import com.soartech.soar.ide.core.model.SoarBufferChangedEvent;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.impl.serialization.ElementMemento;

/**
 * Base implementation of the ISoarOpenable interface
 * 
 * @author ray
 */
public abstract class AbstractSoarOpenable extends AbstractSoarElement implements
        ISoarOpenable
{
    private boolean open = false;
    private ISoarBuffer buffer = null;
    private BufferListener bufferListener = new BufferListener();
    
    /**
     * 
     */
    public AbstractSoarOpenable(AbstractSoarElement parent)
    {
        super(parent);
    }
    
    public AbstractSoarOpenable(AbstractSoarElement parent, ElementMemento memento) throws SoarModelException
    {
        super(parent, memento);
    }
    
    /**
     * @return true if this element may have an associated source buffer.
     *      Sub-classes must override as required.
     */
    protected boolean hasBuffer()
    {
        return false;
    }
    
    /**
     * If a subclass overrides {@link #hasBuffer()} to return true, it must
     * implement this method to open the buffer and initialize its contents
     * appropriately.
     *  
     * @param monitor Progress monitor for opening the buffer
     * @return The buffer.
     * @throws SoarModelException If opening the buffer fails.
     */
    protected ISoarBuffer openBuffer(IProgressMonitor monitor) throws SoarModelException
    {
        return null;
    }
    
    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarOpenable#close()
     */
    public void close() throws SoarModelException
    {
        open = false;
        if(buffer != null)
        {
            buffer.close();
            buffer = null;
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarOpenable#getBuffer()
     */
    public ISoarBuffer getBuffer() throws SoarModelException
    {
        if(hasBuffer() && buffer == null)
        {
            openWhenClosed(null);
            buffer = openBuffer(null);
            if(buffer != null)
            {
                buffer.addListener(bufferListener);
            }
        }
        return buffer;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarOpenable#isOpen()
     */
    public boolean isOpen()
    {
        return open;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarOpenable#open(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void open(IProgressMonitor monitor) throws SoarModelException
    {
        open = true;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.impl.AbstractSoarElement#detach()
     */
    @Override
    protected void detach()
    {
        // TODO: Call close?  Do this in response to a buffer closed event?
        if(buffer != null)
        {
            buffer.close();
        }
        assert buffer == null; // From close callback
        super.detach();
    }


    /**
     * Listener for buffer closed event. This allows buffers to be closed to
     * preserve system resources.  They will be re-opened lazily when they
     * are requested again.
     * 
     * @author ray
     */
    private class BufferListener implements ISoarBufferChangedListener
    {
        /* (non-Javadoc)
         * @see com.soartech.soar.ide.core.model.ISoarBufferChangedListener#onBufferChanged(com.soartech.soar.ide.core.model.SoarBufferChangedEvent)
         */
        public void onBufferChanged(SoarBufferChangedEvent event)
        {
            // Nothing to do here
        }

        /* (non-Javadoc)
         * @see com.soartech.soar.ide.core.model.ISoarBufferChangedListener#onBufferClosed(com.soartech.soar.ide.core.model.ISoarBuffer)
         */
        public void onBufferClosed(ISoarBuffer buffer)
        {
            assert buffer == AbstractSoarOpenable.this.buffer;
            getInternalSoarModel().getBufferManager().removeBuffer(buffer);
            AbstractSoarOpenable.this.buffer = null;
        }
        
    }
}
