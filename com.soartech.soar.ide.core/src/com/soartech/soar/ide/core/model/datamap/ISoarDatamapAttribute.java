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
 * Represents an element in a data map graph.
 * 
 * <p>Each data map element represents the attribute and value part of a
 * WME. It also contains a list of child elements in the case that the
 * element's value is used as an object (identifier). For example, the 
 * following production:
 * 
 * <pre>
 * sp {test
 *    (state <s> ^io.input-link <il> ^data <data>)
 *    (<il> ^message <m>)
 *    (<m> ^peer tasker ^timestamp { <ts> > <now>})
 *    (<data> ^location <loc> ^timestamp <now>)
 *    (<loc> ^name start ^x <x> ^y <y>)
 * -->
 *    (<s> ^operator <o>)
 *    (<o> ^name move ^location <loc>)
 * }
 * </pre>
 * 
 * <p>would produce the following elements
 * 
 *  <pre>
 *  (<s> (io <io>) (data <data>) (+ operator <o>))
 *  (<io> input-link <il>)
 *  (<il> message <m>)
 *  (<m> (peer tasker) (timestamp <ts> <now>))
 *  (<data> (location <loc>) (timestamp <now>))
 *  (<loc> (name start) (x <x>) (y <y))
 *  (<o> (+ name move) (+ location <loc>))
 *  </pre>
 *  
 *  <p>Although this is a tree representation, it is actually a graph.
 *  
 * @author ray
 */
public interface ISoarDatamapAttribute
{
    public static final int USAGE_TEST = 1;
    public static final int USAGE_ADD = 2;
    public static final int USAGE_REMOVE = 4;
    
    /**
     * @return The data map this element belongs to
     */
    ISoarDatamap getDatamap();
    
    /**
     * @return The source node of this attribute
     */
    ISoarDatamapNode getSource();
    
    /**
     * @return The target node of this attribute
     */
    ISoarDatamapNode getTarget();
    
    /**
     * @return The name of the attribute or <code>null</code> if it is 
     *      variablized.
     */
    String getName();
    
    /**
     * @return True if the attribute of this element is variablized
     */
    boolean isVariablized();
    
    /**
     * Set whether this element is persistent in the data map. A persistent
     * element will not be removed from the datamap even after all of its
     * supporting productions have been removed.
     * 
     * <p>If persistent is <code>false</code> and this element has no
     * supporting productions, it is removed from the data map automatically.
     * 
     * <p>This method has no affect if {@link #isState()} is true.
     * 
     * @param persistent The new persistence value
     */
    void setPersistent(boolean persistent);
    
    /**
     * @return true if this element is persistent or if 
     *      {#link {@link #isState()} is true.
     */
    boolean isPersistent();
    
    /**
     * @return The overall usage of this attribute by combining usage of all
     *      supporting productions.
     */
    int getOverallUsage();
    
    /**
     * @return The set of productions currently supporting this element
     *  in the datamap. Is {@link #isState()} is true, this method will
     *  always retrun true.
     */
    Set<ISoarProduction> getSupportingProductions();
    
    /**
     * Add a production to the list of supporting productions. Adding the
     * same production more than once has no affect.
     * 
     * @param p The supporting production
     * @param usage The usage of the element in the production. If the 
     *  production is already a supporting production, this value is
     *  combined (bitwise or) with the previous usage value.
     */
    void addSupportingProduction(ISoarProduction p, int usage);
    
    /**
     * Remove a supporting production from this element. If this element
     * has not been marked as persistent then it will be automatically
     * removed from the data map.
     * 
     * @param p The supporting production to remove
     */
    void removeSupportingProduction(ISoarProduction p);
    
    /**
     * Returns the usage of this element by the given supporting production.
     * 
     * @param p The production
     * @return bit-wise or of usages.
     */
    int getSupportingProductionUsage(ISoarProduction p);
}
