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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for the datamap
 *
 * @author ray
 */
public class SoarDatamapTools
{
    /**
     * Test whether all attributes in a collection are from the same datamap.
     *
     * @param attrs The attributes
     * @return true if all attributes are from the same datamap or if the
     *      collection is empty
     */
    public static boolean allFromSameDatamap(Collection<ISoarDatamapAttribute> attrs)
    {
        ISoarDatamap d = null;
        for(ISoarDatamapAttribute a : attrs)
        {
            if(d == null)
            {
                d = a.getDatamap();
            }
            else if(d != a.getDatamap())
            {
                return false;
            }
        }
        return true;
    }
    
    public static Map<String, ISoarDatamapNode> getAllElementNodes(ISoarDatamap datamap, boolean includeVariablized)
    {
//        List<ISoarDatamapNode> nodes = new ArrayList<ISoarDatamapNode>();
        
        Map<String, ISoarDatamapNode> nodes = new HashMap<String, ISoarDatamapNode>();
        
        ISoarDatamapNode state = datamap.getState();
        
        Set<ISoarDatamapAttribute> lvl0Attrs = state.getAttributes();
        
        for(ISoarDatamapAttribute attr : lvl0Attrs)
        {
            System.out.println("Level 0 attr: " + attr.getName());
            
            recurseElements(attr, attr.getName(), includeVariablized, 0, nodes);
        }
        
        return nodes;
    }
    
    private static void recurseElements(ISoarDatamapAttribute inputAttr, String path, boolean includeVariablized, int index, Map<String, ISoarDatamapNode> nodes)
    {
        index++;
        
        Set<ISoarDatamapAttribute> attrs = inputAttr.getTarget().getAttributes();
        
        for(ISoarDatamapAttribute attr : attrs)
        {
            System.out.println("Level " + index + " attr: " + attr.getName());
            
            recurseElements(attr, path + "." + attr.getName(), includeVariablized, index, nodes);
        }
        
        if(attrs.isEmpty())
        {
            System.out.println("Found full path to attr: " + path);
            
            //check this result
            nodes.put(path, inputAttr.getTarget());
        }
        
    }

    public static Set<ISoarDatamapAttribute> getElements(ISoarDatamap datamap,
                String path, boolean includeVariablized)
    {
        String[] pathParts = path.split("\\.");
        for(int i = 0; i < pathParts.length; ++i)
        {
            String part = pathParts[i];
            if(part.length() == 0 || (part.startsWith("<") && part.endsWith(">")))
            {
                pathParts[i] = null;
            }
        }
        return datamap.getElements(pathParts, includeVariablized);
    }

    public static List<ISoarDatamapAttribute> getPathToNode(ISoarDatamapNode target)
    {
        List<ISoarDatamapAttribute> path = new ArrayList<ISoarDatamapAttribute>();
        Set<ISoarDatamapNode> visited = new HashSet<ISoarDatamapNode>();

        ISoarDatamap datamap = target.getDatamap();

        getPathToNodeHelper(datamap.getState(), target, path, visited);

        Collections.reverse(path);

        return path;
    }

    private static boolean getPathToNodeHelper(ISoarDatamapNode current,
                                            ISoarDatamapNode target,
                                            List<ISoarDatamapAttribute> path,
                                            Set<ISoarDatamapNode> visited)
    {
        if(target == current)
        {
            return true;
        }
        if(visited.contains(current))
        {
            return false;
        }
        visited.add(current);

        for(ISoarDatamapAttribute a : current.getAttributes())
        {
            if(getPathToNodeHelper(a.getTarget(), target, path, visited))
            {
                path.add(a);
                return true;
            }
        }

        return false;
    }


    public static Set<ISoarDatamapAttribute> findAttributesByName(ISoarDatamap datamap, String attributeName)
    {
        HashSet<ISoarDatamapAttribute> attributes = new HashSet<ISoarDatamapAttribute>();
        HashSet<ISoarDatamapAttribute> visited = new HashSet<ISoarDatamapAttribute>();

        for (ISoarDatamapAttribute attribute : datamap.getState().getAttributes())
        {
            findAttributesByNameHelper(attribute, attributeName, attributes, visited);
        }

        return attributes;
    }

    private static void findAttributesByNameHelper(ISoarDatamapAttribute attribute, String name, Set<ISoarDatamapAttribute> results, Set<ISoarDatamapAttribute> visited)
    {
        if (visited.contains(attribute) || attribute.getName() == null)
        {
            return;
        }
        visited.add(attribute);

        if (attribute.getName().equals(name)) 
        {
            results.add(attribute);
        }
        
        for (ISoarDatamapAttribute child : attribute.getTarget().getAttributes())
        {
            findAttributesByNameHelper(child, name, results, visited);
        }
    }

}
