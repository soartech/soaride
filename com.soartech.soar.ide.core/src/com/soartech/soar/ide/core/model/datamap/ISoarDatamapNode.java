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

import java.util.Set;

import com.soartech.soar.ide.core.model.ISoarProduction;

/**
 * Represents a node in the datamap graph. Each node corresponds to a variable 
 * in a Soar production.
 * 
 * @author ray
 */
public interface ISoarDatamapNode
{
    /**
     * @return The datamap this node is in
     */
    ISoarDatamap getDatamap();
    
    /**
     * @return true if this is the root state node.
     */
    boolean isState();
    
    /**
     * @return true if this node has outgoing attributes
     */
    boolean hasAttributes();
    
    Set<ISoarDatamapAttribute> getAttributes();
    
    /**
     * Retrieve a child by attribute name. If create is true and the child
     * does not exist, a new child is created and returned with the given
     * name
     * 
     * @param name The name of the attribute, null for the variablized attribute.
     * @return The attribute, or null if not found.
     */
    ISoarDatamapAttribute getAttribute(String name);
    
    /**
     * Find the element(s) at the given attribute path starting at this node.
     * 
     * @param path The path components. A null entry is treated as a wild card
     *  and may result in multiple matches.
     * @param offset Start index in path array
     * @param includeVariablized If true, when variablized elements are
     *  encountered, they will be followed even if the current path component
     *  is not a wildcard.
     * @return List of matching attributes
     */
    Set<ISoarDatamapAttribute> getAttributes(String[] path, int offset,
                                         boolean includeVariablized);
 
    /**
     * @return All values associated with this node
     */
    Set<ISoarDatamapValue> getValues();
    
    /**
     * Removes all values.
     */
    void clearValues();
    
    /**
     * 
     * @param production
     * @param value
     */
    ISoarDatamapValue addValue(ISoarProduction production, String value);
}
