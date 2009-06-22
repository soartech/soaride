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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.soartech.soar.ide.core.model.ISoarBuffer;
import com.soartech.soar.ide.core.model.ISoarBufferChangedListener;
import com.soartech.soar.ide.core.model.ISoarOpenable;
import com.soartech.soar.ide.core.model.SoarBufferChangedEvent;
import com.soartech.soar.ide.core.model.SoarModelException;

/**
 * @author ray
 */
public class SoarBuffer implements ISoarBuffer
{
    private IFile file;
    private ISoarOpenable owner;
    private char[] contents;
    private List<ISoarBufferChangedListener> listeners = new ArrayList<ISoarBufferChangedListener>();
    private boolean closed = false;
    private Object lock = new Object();
   
    /**
     * @param file
     * @param contents
     * @param owner
     */
    public SoarBuffer(IFile file, ISoarOpenable owner)
    {
        this.file = file;
        this.contents = new char[0];
        this.owner = owner;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#addListener(com.soartech.soar.ide.core.model.ISoarBufferChangedListener)
     */
    public synchronized void addListener(ISoarBufferChangedListener listener)
    {
        listeners.add(listener);
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#removeListener(com.soartech.soar.ide.core.model.ISoarBufferChangedListener)
     */
    public synchronized void removeListener(ISoarBufferChangedListener listener)
    {
        listeners.remove(listener);
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#close()
     */
    public void close()
    {
        synchronized(lock)
        {
            if(isClosed())
            {
                return;
            }
            this.contents = null;
            this.closed = true;
        }
        for(ISoarBufferChangedListener listener : getListeners())
        {
            listener.onBufferClosed(this);
        }
        synchronized(this)
        {
            listeners.clear();
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#getCharacters()
     */
    public char[] getCharacters()
    {
        synchronized(lock)
        {
            char[] newContents = new char[contents.length];
            System.arraycopy(contents, 0, newContents, 0, contents.length);
            return newContents;
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#getContents()
     */
    public String getContents()
    {
        return new String(getCharacters());
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#getLength()
     */
    public int getLength()
    {
        synchronized (lock)
        {
            return contents.length;
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#getOwner()
     */
    public ISoarOpenable getOwner()
    {
        return owner;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#getText(int, int)
     */
    public String getText(int offset, int length)
    {
        synchronized (lock)
        {
            StringBuilder buffer = new StringBuilder();
            buffer.append(contents, offset, length);
            return buffer.toString();
        }
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#getUnderlyingResource()
     */
    public IResource getUnderlyingResource()
    {
        return file;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#isClosed()
     */
    public boolean isClosed()
    {
        return closed;
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#replace(int, int, char[])
     */
    public void replace(int position, int length, char[] text)
    {
        synchronized(lock)
        {
            int delta = text.length - (length - position + 1);
            int newLength = contents.length + delta;
            
            char[] newContents = new char[newLength];
            // Copy old contents up to insertion point
            System.arraycopy(contents, 0, newContents, 0, position);
            
            // Copy new text
            System.arraycopy(text, 0, newContents, position, text.length);
            
            // Copy remaining old contents to end of buffer
            System.arraycopy(contents, position + length, 
                             newContents, position + text.length, 
                             newLength - (position + text.length + 1));
            
            contents = newContents;
        }
        fireChangedEvent(position, length, new String(text));
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#replace(int, int, java.lang.String)
     */
    public void replace(int position, int length, String text)
    {
        replace(position, length, text.toCharArray());
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#save(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void save(IProgressMonitor monitor) throws SoarModelException
    {
        // TODO: What exactly should this do?
    }

    /* (non-Javadoc)
     * @see com.soartech.soar.ide.core.model.ISoarBuffer#setContents(char[])
     */
    public void setContents(char[] contents)
    {
        synchronized(lock)
        {
            this.contents = contents;
        }
        fireChangedEvent(0, getLength(), new String(contents));
    }

    private List<ISoarBufferChangedListener> getListeners()
    {
        return new ArrayList<ISoarBufferChangedListener>(listeners);
    }
    
    private void fireChangedEvent(int offset, int length, String text)
    {
        SoarBufferChangedEvent e = new SoarBufferChangedEvent(this, offset, length, text);
        
        for(ISoarBufferChangedListener listener : getListeners())
        {
            listener.onBufferChanged(e);
        }
    }
}
