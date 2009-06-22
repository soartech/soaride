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
package com.soartech.soar.ide.ui.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import com.soartech.soar.ide.core.refactoring.AbstractSoarRefactoring;
import com.soartech.soar.ide.core.refactoring.change.IChangeProcessor;
import com.soartech.soar.ide.core.refactoring.change.SourceSoarChangeProcessor;

/**
 * Refactoring to handle adding a new folder to the load.soar file of its parent folder.
 * 
 * @author sfurtwangler
 *
 */
public class CreateSoarFolderRefactoring extends AbstractSoarRefactoring {

	//private GeneratePropertiesRequestProcessor requestProcessor;
	private IChangeProcessor changeProcessor;
	private IFolder newFolder;
    private IFile loadFile;
    private IFile soarFile;
	public CreateSoarFolderRefactoring(String name, IFolder newFolder) {
		super(name);
			this.newFolder = newFolder;
			this.soarFile = newFolder.getFile("load.soar");
			initWizard(name);
	}

	private void initWizard(String name) {
		changeProcessor=null;
		if(soarFile==null)
		{
			return;
		}
        IContainer parent = newFolder.getParent();
        IResource resource = parent.findMember("load.soar");
        if(resource == null || 
           resource.getType() != IResource.FILE ||
           resource.equals(soarFile))
        {
            return;
        }
        loadFile = (IFile) resource;
        changeProcessor = new SourceSoarChangeProcessor("Sourcing soar file",soarFile,loadFile);
	}                            

	@Override
	protected List<IChangeProcessor> getChangeProcessors() {
		List<IChangeProcessor> processors = new ArrayList<IChangeProcessor>();
		processors.add(changeProcessor);
		return processors;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		return status;
	}

}

