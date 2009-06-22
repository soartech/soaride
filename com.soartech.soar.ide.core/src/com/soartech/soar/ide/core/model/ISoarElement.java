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

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

/**
 * Base interface implemented by all elements in the model. Elements form a
 * tree, with each element having zero or more children depending on the
 * type of element.
 * 
 * @author ray
 */
public interface ISoarElement extends IAdaptable
{
    /**
     * @return The lock for this element
     */
    Object getLock();
    
    /**
     * Returns the Soar model this element belongs to.
     * 
     * @return The Soar model this element belongs to.
     */
    ISoarModel getSoarModel();
    
    /**
     * Returns the Soar project this element belongs to.
     * 
     * @return The soar project.
     */
    ISoarProject getSoarProject();
    
    /**
     * Returns true if this element has been detached from the model. This 
     * should be checked before doing anything interesting with the element.
     * 
     * @return True if this element has been detached from the model.
     */
    boolean isDetached();
        
    /**
     * Returns the underlying resource for this Soar element. For example,
     * a Soar project, or file.
     * 
     * @return The underlying resource, or <code>null</code> if there is
     *      none.
     */
    IResource getCorrespondingResource();
    
    /**
     * Returns the smallest underlying resource that contains this element.
     * 
     * <p>For example, the containing resource for an {@link ISoarProduction}
     * would be the resource of its parent {@link ISoarFile}
     * 
     * @return Underlying resource, or <code>null</code> if there is none.
     */
    IResource getContainingResource();
    
    /**
     * @return The parent element of this element or <code>null</code>
     *      if it has none
     */
    ISoarElement getParent();
    
    /**
     * @return The first openable parent.
     */
    ISoarOpenable getOpenable();
    
    /**
     * @return <code>true</code> if this element has any children
     */
    boolean hasChildren();
    
    /**
     * @return Unmodifiable list of children in this element
     * @throws SoarModelException 
     */
    List<ISoarElement> getChildren() throws SoarModelException;
    
    /**
     * Returns the path to the innermost resource containing this element.
     * 
     * @return Path to the innermost resource containing this element.
     */
    IPath getPath();
    
    /**
     * @return True if any error problems have been found in this element
     */
    boolean hasErrors();
    
    /**
     * @return True if any warning problems have been found in this element
     */
    boolean hasWarnings();
}
