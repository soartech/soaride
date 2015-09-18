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
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.ContainerGenerator;
import org.eclipse.ui.dialogs.WizardNewFolderMainPage;

// these are for Europa version. :-D
/*
import org.eclipse.ui.ide.undo.CreateFileOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
*/
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;

public class NewSoarFolderWizardPage extends WizardNewFolderMainPage
{
//    private IWorkbench workbench;
    private Button addToLoadCheckBox;
    private IFolder newFolder;
    private IFile newFile;
    private boolean sourceFolder;

    
    public NewSoarFolderWizardPage(IWorkbench workbench,
            IStructuredSelection selection)
    {
        super("newSoarDirectoryWizardPage", selection);
        setTitle("New Soar Directory");
        setDescription("Create a new Soar Directory");
//        this.workbench = workbench;
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
    	sourceFolder = addToLoadCheckBox.getSelection();
    	newFolder = this.createNewFolder();
    	if(newFolder==null)return false;
    	return addLoadFile();
    }
    private boolean addLoadFile()
    {
    	final IPath containerPath = newFolder.getFullPath();
    	IPath newFilePath = containerPath.append("load.soar");
    	final IFile newFileHandle = createFileHandle(newFilePath);
    	final InputStream initialContents = getInitialContents();
    	createLinkTarget();
        WorkspaceModifyOperation op = new WorkspaceModifyOperation(createRule(newFileHandle)) {
            protected void execute(IProgressMonitor monitor)
                    throws CoreException {
                try {
                    monitor.beginTask("New Soar Folder Progress", 2000);
                    ContainerGenerator generator = new ContainerGenerator(
                            containerPath);
                    generator.generateContainer(new SubProgressMonitor(monitor,
                            1000));
                    createFile(newFileHandle, initialContents,
                            new SubProgressMonitor(monitor, 1000));
                } finally {
                    monitor.done();
                }
            }
        };
        /**
         * Code for 3.3
         **/
        /*
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				CreateFileOperation op = new CreateFileOperation(newFileHandle,
						null, initialContents,"New File Creation");
				try {
					PlatformUI.getWorkbench().getOperationSupport()
							.getOperationHistory().execute(
									op,
									monitor,
									WorkspaceUndoUtil
											.getUIInfoAdapter(getShell()));
				} catch (final ExecutionException e) {
					getContainer().getShell().getDisplay().syncExec(
							new Runnable() {
								public void run() {
									if (e.getCause() instanceof CoreException) {
										ErrorDialog
												.openError(
														getContainer()
																.getShell(), // Was
														// Utilities.getFocusShell()
														"New Soar File Creation Error",
														null, // no special
														// message
														((CoreException) e
																.getCause())
																.getStatus());
									} else {
										SoarEditorUIPlugin.log(e);
										MessageDialog.openError(getContainer().getShell(),
												"Internal Error", NLS.bind("Internal Error Mesage",	e.getCause().getMessage()));
									}
								}
							});
				}
			}
		};
        */
		try {
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			// Execution Exceptions are handled above but we may still get
			// unexpected runtime errors.
			SoarEditorUIPlugin.log(e);
			return false;
		}
		newFile = newFileHandle;
		return true;
    }
    
    // Adapted from WizardNewFileCreationPage
    /**
     * Returns the scheduling rule to use when creating the resource at
     * the given container path. The rule should be the creation rule for
     * the top-most non-existing parent.
     * @param resource The resource being created
     * @return The scheduling rule for creating the given resource
     * @since 3.1
     */
    protected ISchedulingRule createRule(IResource resource) {
        IResource parent = resource.getParent();
        while (parent != null) {
            if (parent.exists()) {
                return resource.getWorkspace().getRuleFactory().createRule(resource);
            }
            resource = parent;
            parent = parent.getParent();
        }
        return resource.getWorkspace().getRoot();
    }
    // Adapted from WizardNewFileCreationPage
    /**
     * Creates a file resource given the file handle and contents.
     *
     * @param fileHandle the file handle to create a file resource with
     * @param contents the initial contents of the new file resource, or
     *   <code>null</code> if none (equivalent to an empty stream)
     * @param monitor the progress monitor to show visual progress with
     * @exception CoreException if the operation fails
     * @exception OperationCanceledException if the operation is canceled
     */
    protected void createFile(IFile fileHandle, InputStream contents,
            IProgressMonitor monitor) throws CoreException {
        if (contents == null) {
            contents = new ByteArrayInputStream(new byte[0]);
        }

        try {
            // Create a new file resource in the workspace
            IPath path = fileHandle.getFullPath();
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            int numSegments= path.segmentCount();
            if (numSegments > 2 && !root.getFolder(path.removeLastSegments(1)).exists()) {
                // If the direct parent of the path doesn't exist, try to create the
                // necessary directories.
                for (int i= numSegments - 2; i > 0; i--) {
                    IFolder folder = root.getFolder(path.removeLastSegments(i));
                    if (!folder.exists()) {
                        folder.create(false, true, monitor);
                    }
                }
            }
            fileHandle.create(contents, false, monitor);
        } catch (CoreException e) {
            // If the file already existed locally, just refresh to get contents
            if (e.getStatus().getCode() == IResourceStatus.PATH_OCCUPIED) {
                fileHandle.refreshLocal(IResource.DEPTH_ZERO, null);
            } else {
                throw e;
            }
        }

        if (monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }
    
    
	protected IFile createFileHandle(IPath filePath) {
		return SoarEditorUIPlugin.getWorkspace().getRoot().getFile(
				filePath);
	}
    protected InputStream getInitialContents()
    {
        String contents = 
        "##!\n" +
        "# @file: \n" +
        "#\n" +
        "# @created " + System.getProperty("user.name") + " " +
            new SimpleDateFormat("yyyyMMdd").format(new Date()) + "\n";

        return new ByteArrayInputStream(contents.getBytes());
    }
    public boolean AddToLoadFile()
    {
    	return sourceFolder;
    }
    public IFolder getFolder(){ return newFolder; }
    public IFile getFile(){ return newFile; }
}