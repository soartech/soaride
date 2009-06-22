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


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.model.ISoarModelListener;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.SoarModelEvent;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.views.SoarLabelProvider;

/**
 * @author ray
 */
public class DeleteSoarAgentDialog extends TrayDialog
{
    private Map<ISoarProject, List<ISoarAgent>> agentsByProject = new LinkedHashMap<ISoarProject, List<ISoarAgent>>();
    
    private TreeViewer tree;
    private ModelListener modelListener = new ModelListener();
    private boolean disposed = false;
    
    public DeleteSoarAgentDialog(Shell parentShell)
    {
        super(parentShell);
        
        setHelpAvailable(false);

        try
        {
            for(ISoarProject project : SoarCorePlugin.getDefault().getSoarModel().getProjects())
            {
                List<ISoarAgent> agents = new ArrayList<ISoarAgent>(project.getAgents());
                agentsByProject.put(project, agents);
            }
        }
        catch (SoarModelException e)
        {
            SoarEditorUIPlugin.log(e);
            return;
        }
        
        SoarCorePlugin.getDefault().getSoarModel().addListener(modelListener);
    }
    
    /*
     * (non-Javadoc) Method declared in Window.
     */
    protected void configureShell(Shell shell) 
    {
        super.configureShell(shell);
        shell.setText("Delete Soar Agent");
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.TrayDialog#close()
     */
    @Override
    public boolean close()
    {
        disposed = true;
        SoarCorePlugin.getDefault().getSoarModel().removeListener(modelListener);
        return super.close();
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed()
    {
        IStructuredSelection s = (IStructuredSelection) tree.getSelection();
        final List<ISoarAgent> agents = new ArrayList<ISoarAgent>();
        for(Object o : s.toArray())
        {
            if(o instanceof ISoarAgent)
            {
                agents.add((ISoarAgent) o);
            }
        }
        
        WorkspaceModifyOperation operation = new WorkspaceModifyOperation()
        {
            @Override
            protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException
            {
                monitor.beginTask("Deleting Soar agents", agents.size());
                for(ISoarAgent agent : agents)
                {
                    if(monitor.isCanceled())
                    {
                        break;
                    }
                    IFile file = agent.getFile();
                    if(file != null && file.exists())
                    {
                        file.delete(false, monitor);
                    }
                    monitor.worked(1);
                }
            }
        };
        
        try
        {
            new ProgressMonitorDialog(getShell()).run(true, true, operation);
        }
        catch (InvocationTargetException e)
        {
            SoarEditorUIPlugin.log(e);
        }
        catch (InterruptedException e)
        {
        }
        
        super.okPressed();
    }

    private Label createMessageArea(Composite composite) 
    {
        Label label = new Label(composite, SWT.NONE);
        label.setText("Choose agent(s) to delete");
        label.setFont(composite.getFont());
        return label;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createDialogArea(Composite container)
    {
        Composite parent = (Composite) super.createDialogArea(container);
        createMessageArea(parent);
        
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.verticalSpacing = 10;

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(layout);
        
        GridData gd = new GridData();
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;  
        
        composite.setLayoutData(gd);
        
        gd = new GridData();
        gd.heightHint = 120;
        gd.widthHint = 200;
        
        tree = new TreeViewer(composite);
        tree.setContentProvider(new ContentProvider());
        tree.setLabelProvider(SoarLabelProvider.createFullLabelProvider(null));
        tree.setInput(SoarCorePlugin.getDefault().getSoarModel());
        tree.getControl().setLayoutData(gd);
        
        tree.expandAll();
        
        return composite;
    }
    
    private void refreshTree()
    {
        if(disposed)
        {
            return;
        }
        try
        {
            for(ISoarProject project : SoarCorePlugin.getDefault().getSoarModel().getProjects())
            {
                List<ISoarAgent> agents = new ArrayList<ISoarAgent>(project.getAgents());
                agentsByProject.put(project, agents);
            }
        }
        catch (SoarModelException e)
        {
            SoarEditorUIPlugin.log(e);
            agentsByProject.clear();
        }
        tree.refresh();
    }
    
    
    private class ContentProvider implements ITreeContentProvider
    {
        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
         */
        public Object[] getChildren(Object parentElement)
        {
            if(parentElement instanceof ISoarModel)
            {
                try
                {
                    return ((ISoarModel) parentElement).getProjects().toArray();
                }
                catch (SoarModelException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            else if(parentElement instanceof ISoarProject)
            {
                return agentsByProject.get(parentElement).toArray();
            }
            return new Object[] {};
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
         */
        public Object getParent(Object element)
        {
            return null;
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
         */
        public boolean hasChildren(Object element)
        {
            return getChildren(element).length > 0;
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
         */
        public Object[] getElements(Object inputElement)
        {
            return getChildren(inputElement);
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.IContentProvider#dispose()
         */
        public void dispose()
        {
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
         */
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
        }
    }
    
    private class ModelListener implements ISoarModelListener
    {
        /* (non-Javadoc)
         * @see com.soartech.soar.ide.core.model.ISoarModelListener#onEvent(com.soartech.soar.ide.core.model.SoarModelEvent)
         */
        public void onEvent(SoarModelEvent event)
        {
            boolean refresh = false;
            for(ISoarElement e : event.getElements())
            {
                if(e instanceof ISoarAgent || e instanceof ISoarProject)
                {
                    refresh = true;
                }
            }
            
            if(refresh)
            {
                Display.getDefault().asyncExec(new Runnable() {

                    public void run()
                    {
                        refreshTree();
                    }});
            }
        }
        
    }
}
