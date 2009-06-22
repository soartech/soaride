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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Represents a Soar source code buffer.  This object backs Soar files both
 * in the workspace generally and while being edited by and editor. All 
 * operations on the buffer are synchronized, i.e. thread-safe.
 * 
 * @author ray
 */
public interface ISoarBuffer
{
    /**
     * Add a listener to this buffer
     * 
     * @param listener The listener
     */
    void addListener(ISoarBufferChangedListener listener);
    
    /**
     * Remove a listener from this buffer
     * 
     * @param listener The listener
     */
    void removeListener(ISoarBufferChangedListener listener);
    
    /**
     * @return The openable object that owns this buffer
     */
    ISoarOpenable getOwner();
    
    /**
     * Save this buffer
     * 
     * @param monitor Progress monitor
     * @throws SoarModelException
     */
    void save(IProgressMonitor monitor) throws SoarModelException;
    
    
    /**
     * Close this buffer 
     */
    void close();
    
    /**
     * @return True if the buffer is currently closed
     */
    boolean isClosed();
    
    /**
     * @return The Eclipse resource that backs this buffer
     */
    IResource getUnderlyingResource();
    
    /**
     * @return All the characters in this buffer
     */
    char[] getCharacters();
    
    /**
     * @return The contents of this buffer as a string
     */
    String getContents();
    
    /**
     * Retrieves a subsection of this buffer as a string.
     * 
     * @param offset The starting offset of the string
     * @param length The length
     * @return The text
     */
    String getText(int offset, int length);
    
    /**
     * @return The length of the buffer.
     */
    int getLength();
    
    /**
     * Replace the contents of the buffer with the given characters
     * 
     * @param contents New contents
     */
    void setContents(char[] contents);
    
    /**
     * Replace a range of the buffer with new text
     * 
     * @param position The start of the range to replace
     * @param length The length of the range to replace
     * @param text The new text, possibly empty
     */
    void replace(int position, int length, char[] text);
    
    /**
     * Replace a range of the buffer with new text
     * 
     * @param position The start of the range to replace
     * @param length The length of the range to replace
     * @param text The new text, possibly empty
     */
    void replace(int position, int length, String text);

}
