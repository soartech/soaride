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
package com.soartech.soar.ide.core.model.impl.datamap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.ast.Pair;
import com.soartech.soar.ide.core.model.ast.SoarProductionAst;
import com.soartech.soar.ide.core.model.ast.Triple;
import com.soartech.soar.ide.core.model.ast.TripleUtils;
import com.soartech.soar.ide.core.model.ast.TriplesExtractor;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapNode;
import com.soartech.soar.ide.core.model.datamap.SoarDatamapAdditionResult;

/**
 * Internal helper class for inserting a production into a datamap
 * 
 * @author ray
 */
class DatamapBuilder
{
    private SoarDatamap datamap;
    private ISoarProduction production;
    private TriplesExtractor extractor;
    private Map<String, List<Triple>> triples = new HashMap<String, List<Triple>>();
    private Map<String, ISoarDatamapNode> nodes = new HashMap<String, ISoarDatamapNode>();
    private SoarDatamapAdditionResult result;
    
    public SoarDatamapAdditionResult addProduction(SoarDatamap datamap, ISoarProduction p)
    {
        this.datamap = datamap;
        this.production = p;
        this.result = new SoarDatamapAdditionResult();
        
        SoarProductionAst ast = production.getSyntaxTree();
        if(ast == null)
        {
            return result;
        }
        
        // Extract triples from the syntax tree
        extractor = new TriplesExtractor(ast);
        
        // Create a map of triples index by their identifier variable
        triples.clear();
        for(Pair v : extractor.variables())
        {
            triples.put(v.getString(), new ArrayList<Triple>());
        }
        
        for(Triple triple : extractor.triples())
        {
            List<Triple> varTriples = triples.get(triple.getVariable().getString());
            assert varTriples != null;
            varTriples.add(triple);
        }
        
        // Starting at the production's state variables fill in the datamap
        try
        {
            datamap.beginModification();
            nodes.clear();
            for(Pair stateVarPair : extractor.stateVariables())
            {
                String stateVar = stateVarPair.getString();
                visitVariable(stateVar, null);
            }
        }
        finally
        {
            datamap.endModification();
        }
        
        // Any variable not in nodes.keySet() is unreachable from the state.
        // Report these. We ignore variablized attributes since they're
        // almost never connected to the state.
        Set<String> attrVars = new HashSet<String>();
        for(Triple triple : extractor.triples())
        {
            String idVar = triple.getVariable().getString();
            String attrVar = triple.getAttribute().getString();

            // Unreachable variable that is not an attribute variable...
            if(isVariable(idVar) && 
               !nodes.keySet().contains(idVar) &&
               !attrVars.contains(idVar))
            {
                result.disconnectedVariables.add(idVar);
            }
            
            // Remember attribute variables so we can filter them out above.
            if(isVariable(attrVar) && !isGeneratedVariable(attrVar))
            {
                attrVars.add(attrVar);
            }
        }
        
        for(Pair v : extractor.getUnboundRhsFunctionVariables())
        {
            result.unboundRhsFunctionVariables.add(v.getString());
        }
        
        return result;
    }
    
    private void visitVariable(String var, ISoarDatamapNode source)
    {
        if(nodes.containsKey(var))
        {
            return;
        }
        if(source == null)
        {
            source = datamap.getState();
        }
        nodes.put(var, source);
        
        List<Triple> varTriples = triples.get(var);
        for(Triple triple : varTriples)
        {
            processTriple(source, triple);
        }
    }

    /**
     * Process a single triple (an it's children, recursively) using the given 
     * node as its identifier node.
     * 
     * @param source The source node
     * @param triple The triple to process
     */
    private void processTriple(ISoarDatamapNode source, Triple triple)
    {
        String name = triple.getAttribute().getString();
        String value = triple.getValue().getString();
        // Variablized attributes use null as their name
        String nameKey = isVariable(name) ? null : name;
        
        // See if this attribute is already present
        ISoarDatamapAttribute attr = source.getAttribute(nameKey);
        if(attr != null)
        {
            // If the attribute is present we just continue following our 
            // current path through the graph 
            if(isVariable(value))
            {
                visitVariable(value, attr.getTarget());
            }
            else
            {
                SoarDatamapNode target = (SoarDatamapNode) attr.getTarget();
                target.addValue(production, value);
            }
        }
        else
        {
            // Find or create a target node for this new attribute.
            SoarDatamapNode target = null;
            if(isVariable(value))
            {
                target = (SoarDatamapNode) nodes.get(value);
                if(target == null)
                {
                    target = new SoarDatamapNode(datamap, nameKey != null ? nameKey.toUpperCase().charAt(0) : '*');
                }
                // Recursively follow the target variable
                visitVariable(value, target);
            }
            else
            {
                target = new SoarDatamapNode(datamap, nameKey != null ? nameKey.toUpperCase().charAt(0) : '*');
                target.addValue(production, value);
            }
            
            assert source != null;
            assert target != null;
            
            
            // Create the new attribute
            attr = new SoarDatamapAttribute(datamap, nameKey, 
                                            (SoarDatamapNode) source,
                                            (SoarDatamapNode) target);
        }
        
        attr.addSupportingProduction(production, getUsageFromTriple(triple));
    }
    
    private static boolean isVariable(String v)
    {
        return TripleUtils.isVariable(v);
    }
    
    private static boolean isGeneratedVariable(String v)
    {
        return v.charAt(1) == ' ';
    }
    
    private static int getUsageFromTriple(Triple t)
    {
        int usage = 0;
        if(t.isCondition())
        {
            usage |= ISoarDatamapAttribute.USAGE_TEST;
        }
        else if(t.isAction())
        {
            usage |= ISoarDatamapAttribute.USAGE_ADD;
        }
        return usage;
    }
}
