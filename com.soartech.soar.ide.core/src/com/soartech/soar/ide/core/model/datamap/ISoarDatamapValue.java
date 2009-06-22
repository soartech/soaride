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
 * A currently very minimal interface for a value in the datamap. Each datamap
 * node stores a list of constant values it was associated with in Soar code.
 * This interface is pretty meaningless right now because only its string
 * representation can be used. We will fill it out more and refactor once we
 * know the kinds of things we'd like to do with values.
 * 
 * @author ray
 */
public interface ISoarDatamapValue
{
    /**
     * @return The set of productions currently supporting this value.
     */
    Set<ISoarProduction> getSupportingProductions();
    
    /**
     * Add a supporting production to this value
     * 
     * @param p The production
     */
    void addProduction(ISoarProduction p);
    
    /**
     * Remove a supporting production from this value
     * 
     * @param p The production to remove
     * @return false if no more supporting productions remain
     */
    boolean removeProduction(ISoarProduction p);
    
    
    /**
     * @return String representation of this value
     */
    String toString();
}
