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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;

import com.soartech.soar.ide.core.SoarCorePlugin;

/**
 * Implements an adapter factory for adapting existing Eclipse types like
 * IProject and IFile to their corresponding Soar model types.
 * 
 * @author ray
 */
public class SoarModelAdapterFactory implements IAdapterFactory
{
    private static Class<?>[] SUPPORTED_TYPES = 
        new Class<?>[] { ISoarProject.class, ISoarFile.class, ISoarSourceReference.class };

    /**
     * Register this adapter factory with the adapter manager
     */
    public void register()
    {
        IAdapterManager mgr = Platform.getAdapterManager();
        mgr.registerAdapters(this, IProject.class);
        mgr.registerAdapters(this, IFile.class);
    }
    
    /**
     * Unregister this adapter factory from the adapter manager 
     */
    public void unregister()
    {
        Platform.getAdapterManager().unregisterAdapters(this);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object getAdapter(Object adaptableObject, Class adapterType)
    {
    	ISoarModel model = SoarCorePlugin.getDefault().getSoarModel();
        if(ISoarProject.class.equals(adapterType))
        {
            if(adaptableObject instanceof IProject)
            {
                try
                {
                    return model.getProject(((IProject) adaptableObject).getName());
                }
                catch (SoarModelException e)
                {
                    return null;
                }
            }
        }
        else if(ISoarFile.class.equals(adapterType) ||
                ISoarSourceReference.class.equals(adapterType))
        {
            if(adaptableObject instanceof IFile)
            {
                try
                {
                    return model.getFile((IFile) adaptableObject);
                }
                catch (SoarModelException e)
                {
                    return null;
                }
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
     */
    public Class<?>[] getAdapterList()
    {
        return SUPPORTED_TYPES;
    }

}
