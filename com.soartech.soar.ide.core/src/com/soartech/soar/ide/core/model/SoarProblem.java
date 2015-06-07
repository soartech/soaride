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

import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;

import com.soartech.soar.ide.core.SoarCorePlugin;

/**
 * Representation of a Soar problem, independent of the Eclipse marker
 * mechanism.
 * 
 * @author ray
 */
public class SoarProblem
{
    public int severity = IMarker.SEVERITY_ERROR;
    public int start = -1;
    public int length = 0;
    public String location = "Unknown";
    public String message = "";
    public String quickFixID = "";
    @SuppressWarnings("unchecked")
	public HashMap<String, Comparable<?>> map = null;
    
    @SuppressWarnings("unchecked")
    public static final SoarProblem createError(String message, int start, int length)
    {
        return SoarProblem.createError(message, "Unknown", start, length, "", new HashMap<String, Comparable<?>>());
    }
    @SuppressWarnings("unchecked")
    public static final SoarProblem createError(String message, String location, int start, int length, String fixID, HashMap<String, Comparable<?>> map)
    {
        SoarProblem p = new SoarProblem();
        
        p.message = message;
        p.start = start;
        p.length = length;
        p.quickFixID=fixID;
        p.map=map;
        p.location=location;
        p.createMarkerMap();

        return p;
    }
    @SuppressWarnings("unchecked")
    public static final SoarProblem createWarning(String message, int start, int length)
    {
        return SoarProblem.createWarning(message, "Unknown", start, length, "", new HashMap<String, Comparable<?>>());
    }
    @SuppressWarnings("unchecked")
    public static final SoarProblem createWarning(String message, String location, int start, int length, String fixID, HashMap<String, Comparable<?>> map)
    {
        SoarProblem p = new SoarProblem();
        
        p.severity = IMarker.SEVERITY_WARNING;
        p.message = message;
        p.start = start;
        p.length = length;
        p.quickFixID=fixID;
        p.map=map;
        p.location=location;
        p.createMarkerMap();

        return p;
    }
    @SuppressWarnings("unchecked")
    public void createMarkerMap()
    {
    	if(map==null)
    		map = new HashMap<String, Comparable<?>>();
    	map.put("com.soartech.soar.ide.core.quickFixId",quickFixID);
        if(start >= 0)
        {
        	map.put(IMarker.CHAR_START, start);
        	map.put(IMarker.CHAR_END, start + length);
        }
        map.put(IMarker.SEVERITY, severity);
        map.put(IMarker.MESSAGE, message);
        map.put(IMarker.LOCATION, location);       
    }
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + length;
        result = PRIME * result + ((message == null) ? 0 : message.hashCode());
        result = PRIME * result + severity;
        result = PRIME * result + start;
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final SoarProblem other = (SoarProblem) obj;
        if (length != other.length)
            return false;
        if (message == null)
        {
            if (other.message != null)
                return false;
        }
        else if (!message.equals(other.message))
            return false;
        if (severity != other.severity)
            return false;
        if (start != other.start)
            return false;
        return true;
    }
    
    
}
