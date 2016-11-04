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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.SoarUiTools;

/**
 * @author ray
 */
class SoarAgentFileSelector implements ITreeViewerListener, ICheckStateListener, ISelectionChangedListener, IDoubleClickListener
{
    private SoarAgentEditor editor;
    private final ITreeContentProvider treeContentProvider;
    private final IStructuredContentProvider listContentProvider;
    private final ILabelProvider treeLabelProvider = WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider();
    private final ILabelProvider listLabelProvider = WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider();
    private CheckboxTreeViewer treeViewer;
    private CheckboxTableViewer listViewer;

    public SoarAgentFileSelector(SoarAgentEditor editor, Composite parent)
    {
        this.editor = editor;
        this.treeContentProvider = new SoarAgentContentProvider(editor.getAgent());
        this.listContentProvider = new SoarAgentContentProvider(editor.getAgent());
        
        Composite composite = createComposite(parent);
        
        IProject project = getProject();
        createTreeViewer(composite, -1, 150, project);
        createListViewer(composite, -1, 150, project);
        
        treeViewer.setInput(project.getWorkspace());
        treeViewer.expandToLevel(2);
        updateTreeElementCheckState(project, false);
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
        listViewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.widthHint = width;
        data.heightHint = height;
        listViewer.getTable().setLayoutData(data);
        listViewer.getTable().setFont(parent.getFont());
        listViewer.setContentProvider(listContentProvider);
        listViewer.setLabelProvider(listLabelProvider);
        listViewer.addCheckStateListener(this);
        listViewer.addDoubleClickListener(this);
        
        // Only show Soar files in the tree
        listViewer.addFilter(getViewerFilter(IResource.FILE, project)); 
    }

    private void createTreeViewer(Composite parent, int width, int height,
                                    IProject project) {
        Tree tree = new Tree(parent, SWT.CHECK | SWT.BORDER);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.widthHint = width;
        data.heightHint = height;
        tree.setLayoutData(data);
        tree.setFont(parent.getFont());

        treeViewer = new CheckboxTreeViewer(tree);
        treeViewer.setContentProvider(treeContentProvider);
        treeViewer.setLabelProvider(treeLabelProvider);
        treeViewer.addTreeListener(this);
        treeViewer.addCheckStateListener(this);
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
             ((memberType != IResource.FILE && ext == null) || "soar".equals(ext) || "tcl".equals(ext) || "dm".equals(ext)));
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
        IContainer container = (IContainer) event.getElement();
        // getExpandedState(container) isn't updated until after this event is
        // fired (stupid), so we have to force an update of visible children.
        updateTreeElementCheckState(container, true);
    }
    
    /**
     * Update the check state for the given container and any visible
     * children.
     * 
     * @param container The container
     * @param force If true, forces update of the containers children
     */
    private void updateTreeElementCheckState(IContainer container, boolean force)
    {
        //None
        if(!getAgent().containsAnyChildrenOf(container))
        {
            // If there are any children of the container in the agent, then mark
            // the container as grey. 
            final boolean containsAnyChildrenOf = getAgent().containsAnyChildrenOf(container);
            treeViewer.setGrayChecked(container, containsAnyChildrenOf);
        }
        //Some
        else if(!getAgent().containsAllChildrenOf(container))
        {
            // If the container is contained, but all children are not contained
            // then mark the container as grey.
            treeViewer.setGrayChecked(container, true);
        }
        //All
        else
        {
            // All children are contained. Check the container.
        	//If you select a parent folder that was unchecked before
            treeViewer.setChecked(container, true);
            treeViewer.setGrayed(container, false);
        }
        
        // Update check state of children as necessary
        if(force || treeViewer.getExpandedState(container))
        {
            for(Object o : treeContentProvider.getChildren(container))
            {
                if(o instanceof IContainer)
                {
                    updateTreeElementCheckState((IContainer) o, false);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ICheckStateListener#checkStateChanged(org.eclipse.jface.viewers.CheckStateChangedEvent)
     */
    public void checkStateChanged(CheckStateChangedEvent event)
    {
        ISoarAgent agent = getAgent();
        
        boolean add = event.getChecked();
        IResource resource = (IResource) event.getElement();
        
        try
        {
            if(resource instanceof IFile)
            {
                IFile file = (IFile) resource;
                if(add)
                {
                    agent.addFile(file);
                }
                else
                {
                    agent.removeFile(file);
                }
            }
            else if(resource instanceof IContainer)
            {
                IContainer container = (IContainer) resource;
                if(add)
                {
                    addFolder(container);
                }
                else
                {
                    agent.removeFolder(container);
                }
                updateListViewerChecks();
            }
            updateTreeElementCheckState(getProject(), false);
            editor.setDirty(true);
        }
        catch(SoarModelException e)
        {
            SoarEditorUIPlugin.log(e);
        }
    }
    
    private void addFolder(IContainer folder)
    {
        final ISoarAgent agent = editor.getAgent();
        
        synchronized(agent.getLock())
        {
            try
            {
                folder.accept(new IResourceProxyVisitor() {

                    public boolean visit(IResourceProxy proxy) throws CoreException
                    {
                        int type = proxy.getType();
                        if((type != IResource.PROJECT && type != IResource.FOLDER) ||
                           proxy.isDerived())
                        {
                            return false;
                        }
                        
                        // Filter out .settings
                        IPath path = proxy.requestFullPath();
                        if(path.isEmpty() || path.segment(path.segmentCount() - 1).startsWith("."))
                        {
                            return false;
                        }
                        agent.addFolder((IContainer) proxy.requestResource());
                        
                        return true;
                    }}, IResource.NONE);
            }
            catch (CoreException e)
            {
                SoarEditorUIPlugin.log(e);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
     */
    public void selectionChanged(SelectionChangedEvent event)
    {
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        Object selectedElement = selection.getFirstElement();
        listViewer.setInput(selectedElement);
        
        if(selectedElement == null)
        {
            return;
        }
        
        updateListViewerChecks();
    }
    
    /**
     * Update the check boxes for files currently displayed in list viewer 
     */
    private void updateListViewerChecks()
    {
        final Object input = listViewer.getInput();
        if(input == null)
        {
            return;
        }
        
        for(Object o : listContentProvider.getElements(input))
        {
            if(o instanceof IFile) // not filtered until it hits the viewer
            {
                IFile file = (IFile) o;
                boolean contained = getAgent().contains(file);
                listViewer.setChecked(file, contained);
            }
        }
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
    
}
