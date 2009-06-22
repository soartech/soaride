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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.model.ISoarModelListener;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.SoarModelEvent;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.SoarUiTools;
import com.soartech.soar.ide.ui.views.ElementDoubleClickListener;
import com.soartech.soar.ide.ui.views.SoarLabelProvider;

/**
 * @author ray
 */
public class SoarAgentPriorityEditor extends TrayDialog
{
    private Map<ISoarProject, List<ISoarAgent>> agentsByProject = new LinkedHashMap<ISoarProject, List<ISoarAgent>>();
    
    private TreeViewer tree;
    private Button up;
    private Button down;
    
    private ModelListener modelListener = new ModelListener();
    
    private boolean disposed = false;
    
    public SoarAgentPriorityEditor(Shell parentShell)
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
        shell.setText("Edit agent priorities");
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
    
    private Label createMessageArea(Composite composite) 
    {
        Label label = new Label(composite, SWT.NONE);
        label.setText("Choose the priority of agents for shared files. When a file is edited,\n" +
                      "it will be parsed in the context of the highest priority agent to which\nit belongs.");
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
        tree.addDoubleClickListener(new ElementDoubleClickListener());
        tree.setContentProvider(new ContentProvider());
        tree.setLabelProvider(SoarLabelProvider.createFullLabelProvider(null));
        tree.setInput(SoarCorePlugin.getDefault().getSoarModel());
        tree.getControl().setLayoutData(gd);
        tree.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event)
            {
                update();
            }});
        
        tree.expandAll();
        
        createButtons(composite);
        
        update();
        
        return composite;
    }

    
    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed()
    {
        for(Map.Entry<ISoarProject, List<ISoarAgent>> e : agentsByProject.entrySet())
        {
            ISoarProject project = e.getKey();
            
            try
            {
                project.setAgentPriorities(e.getValue());
            }
            catch (SoarModelException e1)
            {
                SoarEditorUIPlugin.log(e1);
            }
        }
        
        super.okPressed();
    }

    /**
     * @param composite
     */
    private void createButtons(Composite parent)
    {
        Composite panel = new Composite(parent, SWT.NONE);
        panel.setLayout(new GridLayout());
        
        GridData gd = new GridData();
        gd.horizontalAlignment = GridData.FILL;
        gd.verticalAlignment = SWT.BOTTOM;
        gd.grabExcessHorizontalSpace = true;  
        
        up = new Button(panel, SWT.NONE);
        up.setText("Move up");
        up.setLayoutData(gd);
        up.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e)
            {
                upClicked();
            }});
        
        gd = new GridData();
        gd.horizontalAlignment = GridData.FILL;
        gd.verticalAlignment = SWT.TOP;
        gd.grabExcessHorizontalSpace = true;  
        
        down = new Button(panel, SWT.NONE);
        down.setText("Move down");
        down.setLayoutData(gd);
        down.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e)
            {
                downClicked();
            }});
    }
    
    private void update()
    {
        ISoarAgent agent = SoarUiTools.getValueFromSelection(tree.getSelection(), ISoarAgent.class);
        if(agent == null)
        {
            up.setEnabled(false);
            down.setEnabled(false);
        }
        else
        {
            List<ISoarAgent> agents = agentsByProject.get(agent.getSoarProject());
            int index = agents.indexOf(agent);
            up.setEnabled(index != 0);
            down.setEnabled(index != agents.size() - 1);
        }
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
    
    private void upClicked()
    {
        ISoarAgent agent = SoarUiTools.getValueFromSelection(tree.getSelection(), ISoarAgent.class);
        if(agent == null)
        {
            return;
        }
        List<ISoarAgent> agents = agentsByProject.get(agent.getSoarProject());
        int index = agents.indexOf(agent);
        if(index == 0)
        {
            return;
        }
        
        agents.set(index, agents.get(index - 1));
        agents.set(index - 1, agent);
        
        tree.refresh(agent.getSoarProject());
        update();
    }
    
    private void downClicked()
    {
        ISoarAgent agent = SoarUiTools.getValueFromSelection(tree.getSelection(), ISoarAgent.class);
        if(agent == null)
        {
            return;
        }
        
        List<ISoarAgent> agents = agentsByProject.get(agent.getSoarProject());
        int index = agents.indexOf(agent);
        if(index == agents.size() - 1)
        {
            return;
        }
        
        agents.set(index, agents.get(index + 1));
        agents.set(index + 1, agent);
        
        tree.refresh(agent.getSoarProject());
        update();
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
