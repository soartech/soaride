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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;

import com.soartech.soar.ide.ui.refactoring.CreateSoarFolderRefactoring;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
/**
 * A wizard to create a new .soar folder.
 */
public class NewSoarFolderWizard extends Wizard implements INewWizard
{
    private IWorkbench workbench;

    private IStructuredSelection selection;

    private NewSoarFolderWizardPage mainPage;
    private boolean finished;

    public void init(IWorkbench workbench, IStructuredSelection selection)
    {
        this.workbench = workbench;
        this.selection = selection;
        setWindowTitle("Create New Soar Folder");
        finished = false;
    }

    public void addPages()
    {
        mainPage = new NewSoarFolderWizardPage(workbench, selection);
        addPage(mainPage);
    }

    public boolean performFinish()
    {
    	finished=true;
        boolean good = mainPage.finish();
        if(good && addToLoadFile())
        {
			//create refactoring to handle wizard and file editing
			CreateSoarFolderRefactoring refactoring = new CreateSoarFolderRefactoring("Source New Soar Folder",getFolder()); 
	    	//  Refactor for rename
	    	PerformRefactoringOperation refOperation = new PerformRefactoringOperation(refactoring,CheckConditionsOperation.ALL_CONDITIONS);
	    	try
	    	{
	    		ResourcesPlugin.getWorkspace().run(refOperation, null);
	    	}
	    	catch (OperationCanceledException oce)
	    	{
	    		throw new OperationCanceledException();
	    	}
	    	catch (CoreException ce)
	    	{
	    		SoarEditorUIPlugin.log(ce);
	    		good=false;
	    	}
        }
    	return good;
    }
    public boolean wasFinished()
    {
    	return finished;
    }
    public boolean addToLoadFile()
    {
    	return mainPage.AddToLoadFile();
    }
    public IFolder getFolder(){ return mainPage.getFolder(); }
    public IFile getFile(){ return mainPage.getFile(); }
}
