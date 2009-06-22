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
package com.soartech.soar.ide.ui.wizards;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.SoarProjectNature;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;

public class NewSoarAgentWizardPage extends WizardNewFileCreationPage
{
    private IWorkbench workbench;
    private Button emptyAgentCheckBox;

    public NewSoarAgentWizardPage(IWorkbench workbench,
            IStructuredSelection selection)
    {
        super("newSoarAgentWizardPage", selection);
        setTitle("New Soar Agent");
        setDescription("Create a new Soar Agent");
        this.workbench = workbench;
    }

    public void createControl(Composite parent)
    {
        super.createControl(parent);
        setPageComplete(true);
    }

    
    @Override
    protected void createAdvancedControls(Composite parent)
    {
        // Override so that "linked" resources aren't allowed
        
        emptyAgentCheckBox = new Button(parent, SWT.CHECK);
        emptyAgentCheckBox.setText("Create empty agent");
        emptyAgentCheckBox.setSelection(false);
    }

    @Override
    protected IStatus validateLinkedResource()
    {
        // Override so that "linked" resources aren't allowed
        return new Status(IStatus.OK, 
                          SoarEditorUIPlugin.getDefault().getBundle().getSymbolicName(),
                          IStatus.OK, "", null);
    }

    @Override
    protected void createLinkTarget()
    {
        // Override so that "linked" resources aren't allowed
    }

    @Override
    protected String getNewFileLabel()
    {
        return "Agent na&me";
    }

    public boolean finish()
    {
        automaticallyAddSoarSupport();
        
        String fileName = getFileName();
        if (!fileName.endsWith(".soaragent"))
        {
            setFileName(fileName + ".soaragent");
        }
        org.eclipse.core.resources.IFile newFile = createNewFile();
        if (newFile == null)
            return false;
        try
        {
            IWorkbenchWindow dwindow = workbench.getActiveWorkbenchWindow();
            org.eclipse.ui.IWorkbenchPage page = dwindow.getActivePage();
            if (page != null)
                IDE.openEditor(page, newFile, true);
        }
        catch (PartInitException e)
        {
            SoarEditorUIPlugin.log(e);
            return false;
        }
        return true;
    }

    /**
     * Automatically adds Soar support to the project the agent is being created
     * in if it's not already there.
     */
    private void automaticallyAddSoarSupport()
    {
        IPath containerPath = getContainerFullPath();
        IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(containerPath);
        if(container != null)
        {
            try
            {
                IProject project = container.getProject();
                if(!project.hasNature(SoarProjectNature.NATURE_ID))
                {
                    System.out.println("Automatically adding Soar support to project " + project);
                    SoarProjectNature.addToProject(project);
                }
            }
            catch(CoreException e)
            {
                SoarEditorUIPlugin.log(e);
            }
        }
    }

    protected InputStream getInitialContents()
    {
        IPath path = getContainerFullPath().append(getFileName());
        IFile file = createFileHandle(path);
        
        String contents = SoarCorePlugin.getDefault().getSoarModel().getInitialAgentFileContents(file, emptyAgentCheckBox.getSelection());

        return new ByteArrayInputStream(contents.getBytes());
    }
}