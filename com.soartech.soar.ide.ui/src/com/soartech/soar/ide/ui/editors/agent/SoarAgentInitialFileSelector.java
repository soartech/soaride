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

package com.soartech.soar.ide.ui.editors.agent;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.SoarUiTools;
/**
 * A modification of the standard file selection dialog which 
 * solicits a list of files from the user, allowing only soar files in the workspace.
 */
public class SoarAgentInitialFileSelector extends SelectionDialog implements ITreeViewerListener, ISelectionChangedListener, IDoubleClickListener
{
    private SoarAgentEditor editor;
    private final ITreeContentProvider treeContentProvider = new WorkbenchContentProvider();
    private final IStructuredContentProvider listContentProvider = new WorkbenchContentProvider();
    private final ILabelProvider treeLabelProvider = WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider();
    private final ILabelProvider listLabelProvider = WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider();
    private TreeViewer treeViewer;
    private TableViewer listViewer;
    // expand all items in the tree view on dialog open
    private boolean expandAllOnOpen = true;

    
    @Override
	public boolean isHelpAvailable() {
		return false;
	}
	/**
     * Creates a file selection dialog rooted at the given file system element.
     *
     * @param parentShell the parent shell
     * @param editor the Soar Agent Editor
     */
    public SoarAgentInitialFileSelector(Shell parentShell, SoarAgentEditor editor) {
        super(parentShell);
        setTitle("Select Initial Soar File");
		setMessage("Select Initial Soar File");
        this.editor = editor;
    }
    private IProject getProject()
    {
        return getAgent().getSoarProject().getProject();
    }
    
    private ISoarAgent getAgent()
    {
        return editor.getAgent();
    }
    private Composite createComposite(Composite parent)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.makeColumnsEqualWidth = true;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setFont(parent.getFont());
        return composite;
    }
    private void createListViewer(Composite parent, int width, int height,
            IProject project) {
    	listViewer = new TableViewer(parent);//.newList(parent, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = width;
		data.heightHint = height;
		listViewer.getTable().setLayoutData(data);
		listViewer.getTable().setFont(parent.getFont());
		listViewer.setContentProvider(listContentProvider);
		listViewer.setLabelProvider(listLabelProvider);
		//listViewer.addCheckStateListener(this);
		listViewer.addDoubleClickListener(this);
		listViewer.addSelectionChangedListener(this);

		// Only show Soar files in the tree
		listViewer.addFilter(getViewerFilter(IResource.FILE, project)); 
    }

    private void createTreeViewer(Composite parent, int width, int height,
            IProject project) {
		Tree tree = new Tree(parent, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = width;
		data.heightHint = height;
		tree.setLayoutData(data);
		tree.setFont(parent.getFont());
		
		treeViewer = new TreeViewer(tree);
		treeViewer.setContentProvider(treeContentProvider);
		treeViewer.setLabelProvider(treeLabelProvider);
		treeViewer.addTreeListener(this);
		//treeViewer.addCheckStateListener(this);
		treeViewer.addSelectionChangedListener(this);

		// Only show containers in the tree
		treeViewer.addFilter(getViewerFilter(IResource.FOLDER | IResource.PROJECT | IResource.ROOT, project));
    }
    private static boolean isRelevantResource(IResource member, int resourceType)
    {
        int memberType = member.getType();
        String ext = memberType == IResource.FILE ? member.getFileExtension() : null;
        //And the test bits with the resource types to see if they are what we want
        return ((member.getType() & resourceType) > 0 &&
             !member.getName().startsWith(".") &&
             ((memberType != IResource.FILE && ext == null) || "soar".equals(ext)));
    }
    private ViewerFilter getViewerFilter(final int resourceType, final IProject project) {
        
        return new ViewerFilter() {

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                // Filter out other projects
                if(element instanceof IProject && element != project)
                {
                    return false;
                }
                return isRelevantResource((IResource) element, resourceType);
            }};
    }
    
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeViewerListener#treeCollapsed(org.eclipse.jface.viewers.TreeExpansionEvent)
     */
    public void treeCollapsed(TreeExpansionEvent event)
    {
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeViewerListener#treeExpanded(org.eclipse.jface.viewers.TreeExpansionEvent)
     */
    public void treeExpanded(TreeExpansionEvent event)
    {
    }
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
     */
    public void selectionChanged(SelectionChangedEvent event)
    {
    	if(event.getSource()==treeViewer)
    	{
    		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        	Object selectedElement = selection.getFirstElement();
        	listViewer.setInput(selectedElement);
    	}
    	checkOK();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IDoubleClickListener#doubleClick(org.eclipse.jface.viewers.DoubleClickEvent)
     */
    public void doubleClick(DoubleClickEvent event)
    {
        IFile file = SoarUiTools.getValueFromSelection(event.getSelection(), IFile.class);
        if(file == null)
        {
            return;
        }
        try
        {
            IWorkbenchPage page = editor.getSite().getPage();
            if (page != null)
            {
                IDE.openEditor(page, file, true);
            }
        }
        catch (PartInitException e)
        {
            SoarEditorUIPlugin.log(e);
        }
    }
    
    
    /* (non-Javadoc)
     * Method declared in Window.
     */
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
    }

    public void create() {
        super.create();
        initializeDialog();
    }

    /* (non-Javadoc)
     * Method declared on Dialog.
     */
    protected Control createDialogArea(Composite parent) {
        // page group
        Composite composite = (Composite) super.createDialogArea(parent);


        

        createMessageArea(composite);

        Composite composite2 = createComposite(composite);
        
        IProject project = getProject();
        createTreeViewer(composite2, -1, 150, project);
        createListViewer(composite2, -1, 150, project);
        
        treeViewer.setInput(project.getWorkspace());
        //treeViewer.expandToLevel(2);

        return composite;
    }

    /**
     * Returns whether the tree view of the file system element
     * will be fully expanded when the dialog is opened.
     *
     * @return true to expand all on dialog open, false otherwise.
     */
    public boolean getExpandAllOnOpen() {
        return expandAllOnOpen;
    }

    /**
     * Returns a content provider for <code>FileSystemElement</code>s that returns 
     * only files as children.
     */
//    private ITreeContentProvider getFileProvider() {
//        return new WorkbenchContentProvider() {
//            public Object[] getChildren(Object o) {
//                if (o instanceof FileSystemElement) {
//                    return ((FileSystemElement) o).getFiles().getChildren(o);
//                }
//                return new Object[0];
//            }
//        };
//    }

    /**
     * Returns a content provider for <code>FileSystemElement</code>s that returns 
     * only folders as children.
     */
//    private ITreeContentProvider getFolderProvider() {
//        return new WorkbenchContentProvider() {
//            public Object[] getChildren(Object o) {
//                if (o instanceof FileSystemElement) {
//                    return ((FileSystemElement) o).getFolders().getChildren(o);
//                }
//                return new Object[0];
//            }
//        };
//    }

    /**
     * Initializes this dialog's controls.
     */
    private void initializeDialog() {
    	List<?> ar = this.getInitialElementSelections();
    	if(ar.size()==1 && ar.get(0) instanceof IFile)
    	{
    		IFile file = (IFile)ar.get(0);
    		IContainer folder= file.getParent();
    		ArrayList<IResource> l = new ArrayList<IResource>();
    		l.add(folder);
    		StructuredSelection sel = new StructuredSelection(l);
    		treeViewer.setSelection(sel);
    		listViewer.setInput(folder);
    		ArrayList<IResource> l2 = new ArrayList<IResource>();
    		l2.add(file);
    		StructuredSelection sel2 = new StructuredSelection(l2);
    		listViewer.setSelection(sel2);
    	}
    	if(expandAllOnOpen)
    		treeViewer.expandAll();
    	checkOK();
    }
    private void checkOK() {
        // initialize page	
    	IFile file = SoarUiTools.getValueFromSelection(listViewer.getSelection(), IFile.class);

        if (file==null) {
			getOkButton().setEnabled(false);
		} else {
			getOkButton().setEnabled(true);
		}    	
    }

    /**
     * The <code>FileSelectionDialog</code> implementation of this
     * <code>Dialog</code> method builds a list of the selected files for later 
     * retrieval by the client and closes this dialog.
     */
    protected void okPressed() {
    	ISelection selection = listViewer.getSelection();
    	if(selection instanceof IStructuredSelection)
    	{
    		IStructuredSelection ssel = (IStructuredSelection)selection;
    		if(ssel.getFirstElement() instanceof IFile)
    		{
    			IFile file = (IFile)ssel.getFirstElement();
    	    	ArrayList<IFile> list = new ArrayList<IFile>();
    	    	list.add(file);
    	    	setResult(list);
    		}
    	}
        super.okPressed();
    }

    /**
     * Set whether the tree view of the file system element
     * will be fully expanded when the dialog is opened.
     *
     * @param expandAll true to expand all on dialog open, false otherwise.
     */
    public void setExpandAllOnOpen(boolean expandAll) {
        expandAllOnOpen = expandAll;
    }
}