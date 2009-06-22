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

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author ray
 */
public interface ISoarOpenable
{
    /**
     * Open this openable, typically by constructing its children. If the
     * object is already open this has no effect. Typically, this is called
     * automatically when it is required by a method so it shouldn't need
     * to be called manually.
     * 
     * @param monitor Progress monitor
     * @throws SoarModelException
     */
    void open(IProgressMonitor monitor) throws SoarModelException;
    
    /**
     * @return true if this object is open
     */
    boolean isOpen();
    
    /**
     * Close this object, freeing resource in use.
     * 
     * @throws SoarModelException
     */
    void close() throws SoarModelException;
    
    /**
     * Returns the buffer associated with this openable object.
     * 
     * @return The buffer
     * @throws SoarModelException
     */
    ISoarBuffer getBuffer() throws SoarModelException;
    
    void makeConsistent(IProgressMonitor monitor) throws SoarModelException;
}
