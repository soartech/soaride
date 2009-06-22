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

import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarProduction;

/**
 * Interface for a Soar data map. 
 * 
 * <p>A data map is a basic representation of the working memory tested, 
 * created and removed by one or more Soar productions. The purpose of 
 * the data map is to provide a tools for querying information about the
 * structure of working memory used by a Soar system. It can answer
 * questions like:
 * 
 * <ul>
 *  <li>Is production X o-supported or i-supported?
 *  <li>Is a given attribute o-supported or i-supported?
 *  <li>What productions create a given attribute in working memory?
 *  <li>What productions test a given attribute in working memory?
 *  <li>What are the possible values of a given attribute?
 *  <li>What are the operators in the system? What are their parameters
 *      and what structures do they create?
 *  <li>Filter productions by operator
 * </ul>
 * 
 * @author ray
 */
public interface ISoarDatamap
{
    /**
     * @return The lock object for this datamap.
     */
    Object getLock();
    
    /**
     * Returns the root state node of this datamap.  
     * 
     * @return The root state node of this datamap
     */
    ISoarDatamapNode getState();
    
    /**
     * Find the element(s) at the given attribute path starting at the state.
     * 
     * @param path The path components. A null entry is treated as a wild card
     *  and may result in multiple matches.
     * @param includeVariablized If true, when variablized elements are
     *  encountered, they will be followed even if the current path component
     *  is not a wildcard.
     * @return List of matching elements
     */
    Set<ISoarDatamapAttribute> getElements(String[] path, 
                                   boolean includeVariablized);
    
    /**
     * Add a listener to this datamap
     * 
     * @param listener The listener to add
     */
    void addListener(ISoarDatamapListener listener);
    
    /**
     * Remove a listener from this datamap
     * 
     * @param listener The listener to remove
     */
    void removeListener(ISoarDatamapListener listener);
    
    /**
     * Tell the datamap that a series of modifications is about to be
     * performed. No events will be fired until the modifications 
     * are completed. When {@link #endModification()} is called, a datamap
     * event will be fired containing all changes to the datamap during
     * the modification process. Multiple calls to this method must be
     * matched by the same number of calls to {@link #endModification()}.
     */
    void beginModification();
    
    /**
     * @return True if a modification is currently active
     */
    boolean isInModification();
    
    /**
     * End an extended modification.
     */
    void endModification();
    
    /**
     * Adds the given production to the datamap, constructing elements based 
     * upon the productions syntax tree.
     * 
     * @param p The production to add
     * @return A structure with results of the addition
     */
    SoarDatamapAdditionResult addProduction(ISoarProduction p);
    
    /**
     * Removes the given production from all elements it supports. Any 
     * non-persistent elements that become unsupported will be removed
     * from the datamap.
     * 
     * @param p The production to remove
     */
    void removeProduction(ISoarProduction p);
    
    /**
     * Convenience function to associate an Agent with a datamap.
     */
    void setAgent(ISoarAgent agent);
    
    /**
     * Convenience function to retrieve the Agent associated with this
     * datamap.  Will return null until setAgent is called.
     */
    ISoarAgent getAgent();
    
}
