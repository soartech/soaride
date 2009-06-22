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
package com.soartech.soar.ide.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * @author ray
 */
public class SoarUiTools
{
    /**
     * Retrieve the first element from a structured selection
     * 
     * @param selection The selection
     * @return The first element or null if the selection is empty
     */
    public static Object getValueFromSelection(ISelection selection)
    {
        if(!selection.isEmpty() && selection instanceof IStructuredSelection)
        {
            IStructuredSelection ss = (IStructuredSelection) selection;
            return ss.getFirstElement();
        }
        return null;
    }
    
    /**
     * Retrieve a value from the given selection of the given type.
     * 
     * @param <T> The type object object to find
     * @param selection The selection object
     * @param klass The klass of object to find
     * @return Object or null if selection is empty or object not found
     */
    @SuppressWarnings("unchecked")
    public static <T> T getValueFromSelection(ISelection selection, Class<T> klass)
    {
        if(!selection.isEmpty() && selection instanceof IStructuredSelection)
        {
            IStructuredSelection ss = (IStructuredSelection) selection;
            for(Object o : ss.toArray())
            {
                if(klass.isInstance(o))
                {
                    return (T) o;
                }
            }
        }
        return null;
    }
    
    /**
     * Retrieve a list of values from the given selection of the given type.
     * 
     * @param <T> The type object object to find
     * @param selection The selection object
     * @param klass The klass of object to find
     * @return List of objects
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> getValuesFromSelection(ISelection selection, Class<T> klass)
    {
        List<T> r = new ArrayList<T>();
        if(!selection.isEmpty() && selection instanceof IStructuredSelection)
        {
            IStructuredSelection ss = (IStructuredSelection) selection;
            for(Object o : ss.toArray())
            {
                if(klass.isInstance(o))
                {
                    r.add((T) o);
                }
            }
        }
        return r;
    }

}
