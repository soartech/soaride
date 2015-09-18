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
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.resources.IFile;
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

import com.soartech.soar.ide.ui.SoarEditorUIPlugin;

public class NewSoarFileWizardPage extends WizardNewFileCreationPage
{
    private IWorkbench workbench;
    private Button addToLoadCheckBox;
    private IFile newFile;
    private boolean sourceFile;
    
    public NewSoarFileWizardPage(IWorkbench workbench,
            IStructuredSelection selection)
    {
        super("newSoarFileWizardPage", selection);
        setTitle("New Soar File");
        setDescription("Create a new Soar File");
        this.workbench = workbench;
        newFile=null;
        sourceFile=false;
    }
    public IFile getFile(){ return newFile; }
    public void createControl(Composite parent)
    {
        super.createControl(parent);
        setPageComplete(true);
    }

    
    @Override
    protected void createAdvancedControls(Composite parent)
    {
        // Override so that "linked" resources aren't allowed
        
        addToLoadCheckBox = new Button(parent, SWT.CHECK);
        addToLoadCheckBox.setText("Add source statement to existing load.soar file");
        addToLoadCheckBox.setSelection(true);
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

    public boolean finish()
    {
        String fileName = getFileName();
        sourceFile = addToLoadCheckBox.getSelection();
        if (!fileName.endsWith(".soar") && !fileName.endsWith(".tcl"))
        {
            setFileName(fileName + ".soar");
        }
        newFile = createSoarFile();
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

    protected InputStream getInitialContents()
    {
        String contents = 
        "##!\n" +
        "# @file\n" +
        "#\n" +
        "# @created " + System.getProperty("user.name") + " " +
            new SimpleDateFormat("yyyyMMdd").format(new Date()) + "\n";

        return new ByteArrayInputStream(contents.getBytes());
    }
    
    private IFile createSoarFile()
    {
        final IFile result = createNewFile();
        if(result == null)
        {
            return null;
        }
        /* Going to move this logic to the refactoring
        WorkspaceModifyOperation operation = new WorkspaceModifyOperation()
        {
            protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException
            {
                addToLoadFile(result, monitor);
            }
        };
        
        try
        {
            getContainer().run(false, true, operation);
        }
        catch (InvocationTargetException e)
        {
            if (e.getTargetException() instanceof CoreException) {
                ErrorDialog
                        .openError(
                                getContainer().getShell(), // Was Utilities.getFocusShell()
                                IDEWorkbenchMessages.WizardNewFileCreationPage_errorTitle,
                                null, // no special message
                                ((CoreException) e.getTargetException())
                                        .getStatus());
            } else {
                // CoreExceptions are handled above, but unexpected runtime exceptions and errors may still occur.
                SoarEditorUIPlugin.log(e.getTargetException()); //$NON-NLS-1$
                MessageDialog
                        .openError(
                                getContainer().getShell(),
                                IDEWorkbenchMessages.WizardNewFileCreationPage_internalErrorTitle, NLS.bind(IDEWorkbenchMessages.WizardNewFileCreationPage_internalErrorMessage, e.getTargetException().getMessage()));
            }
            return result;
        }
        catch (InterruptedException e)
        {
            return result;
        }
        */
        return result;
    }
    public boolean AddToLoadFile()
    {
    	return sourceFile;
    }
//    private void addToLoadFile(IFile file, IProgressMonitor monitor) throws CoreException
//    {
//        if(!addToLoadCheckBox.getSelection())
//        {
//            return;
//        }
//        
//        String name = file.getName();
//        if(name.endsWith(".tcl"))
//        {
//            return;
//        }
//        
//        IContainer parent = file.getParent();
//        IResource resource = parent.findMember("load.soar");
//        if(resource == null || 
//           resource.getType() != IResource.FILE ||
//           resource.equals(file))
//        {
//            return;
//        }
//        
//        IFile loadFile = (IFile) resource;
//        StringBuilder contents = new StringBuilder();
//        contents.append(SoarModelTools.readFileAsCharArray(loadFile));
//        
//        contents.append("\n" + getSourceCommand(contents.toString()) + " \"" + name + "\"\n");
//        InputStream inByteStream = new ByteArrayInputStream(
//                contents.toString().getBytes());
//
//        loadFile.setContents(inByteStream, false, true, monitor);
//    }
    
//    private String getSourceCommand(String contents)
//    {
//        for(String c : ITclFileReferenceConstants.LP_COMMANDS)
//        {
//            if(contents.contains(c))
//            {
//                return ITclFileReferenceConstants.LP_SOURCE_FILE;
//            }
//        }
//        
//        for(String c : ITclFileReferenceConstants.NGS_COMMANDS)
//        {
//            if(contents.contains(c))
//            {
//                return ITclFileReferenceConstants.NGS_SOURCE;
//            }
//        }
//        
//        return ITclFileReferenceConstants.SOURCE;
//    }
}