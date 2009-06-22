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
package com.soartech.soar.ide.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import com.soartech.soar.ide.core.builder.SoarBuilder;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.core.model.impl.SoarModel;

/**
 * Implements a Soar project nature which can be added to Eclipse
 * projects. When the nature is added, it is responsible for hooking
 * the project into the Soar model.
 * 
 * @author ray
 */
public class SoarProjectNature implements IProjectNature
{
    public static final String NATURE_ID = SoarCorePlugin.PLUGIN_ID + ".nature";
    
    private IProject project;

    /**
     * Add this nature to the given project
     * 
     * @param project The project to add the nature to
     * @throws CoreException 
     */
    public static void addToProject(IProject project) throws CoreException
    {
        IProjectDescription description = project.getDescription();
        String[] natures = description.getNatureIds();
        String[] newNatures = new String[natures.length + 1];
        System.arraycopy(natures, 0, newNatures, 0, natures.length);
        newNatures[natures.length] = NATURE_ID;
        IStatus status = project.getWorkspace().validateNatureSet(natures);

        // check the status and decide what to do
        if (status.getCode() == IStatus.OK) 
        {
            description.setNatureIds(newNatures);
            project.setDescription(description, null);
        } 
        else 
        {
            throw new CoreException(status);
        }
    }
    
    /**
     * Remove this nature from a project.
     * 
     * @param project The project to remove the nature from
     * @throws CoreException
     */
    public static void removeFromProject(IProject project) throws CoreException
    {
        IProjectDescription description = project.getDescription();
        List<String> natures = new ArrayList<String>(Arrays.asList(description.getNatureIds()));
        
        if(!natures.remove(NATURE_ID))
        {
            return;
        }
        
        String[] newNatures = natures.toArray(new String[natures.size()]);
        
        description.setNatureIds(newNatures);
        project.setDescription(description, null);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.core.resources.IProjectNature#configure()
     */
    public void configure() throws CoreException
    {
        System.err.println("Soar project nature added to project " + project.getName());
        
        SoarCorePlugin.getDefault().getInternalSoarModel().createSoarProject(project);
        SoarBuilder.addToProject(project);
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.resources.IProjectNature#deconfigure()
     */
    public void deconfigure() throws CoreException
    {
        System.err.println("Soar project nature removed from project " + project.getName());
        
        removeProjectFromModel();
        SoarBuilder.removeFromProject(project);
    }

    /**
     * @throws SoarModelException
     */
    private void removeProjectFromModel() throws SoarModelException
    {
        SoarModel model = SoarCorePlugin.getDefault().getInternalSoarModel();
        ISoarProject soarProject = model.getProject(project.getName());
        model.removeSoarProject(soarProject);
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.resources.IProjectNature#getProject()
     */
    public IProject getProject()
    {
        return project;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
     */
    public void setProject(IProject project)
    {
        this.project = project;
    }


}
