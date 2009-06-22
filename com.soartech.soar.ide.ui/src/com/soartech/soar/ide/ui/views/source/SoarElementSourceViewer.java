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
package com.soartech.soar.ide.ui.views.source;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarFile;
import com.soartech.soar.ide.core.model.ISoarFileAgentProxy;
import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.ISoarSourceReference;
import com.soartech.soar.ide.core.model.ITclCommand;
import com.soartech.soar.ide.core.model.ITclProcedure;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.ui.SoarEditorPluginImages;
import com.soartech.soar.ide.ui.editors.text.SoarEditor;
import com.soartech.soar.ide.ui.editors.text.SoarSourceViewerConfiguration;

/**
 * @author ray
 */
public class SoarElementSourceViewer extends ViewPart
{	
    public static final String ID = "com.soartech.soar.ide.ui.views.SoarElementSourceViewer";
    
    private SoarSourceViewerConfiguration viewerConfig = new SoarSourceViewerConfiguration();
    private SourceViewer viewer;
    private ISoarSourceReference selectedSourceRef;
    private ISelectionListener selectionListener = new ISelectionListener()
    {
        public void selectionChanged(IWorkbenchPart part, ISelection selection)
        {
            SoarElementSourceViewer.this.selectionChanged(part, selection);
        }
    };
    private ExpandTclToggleAction expandTcl = new ExpandTclToggleAction();
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent)
    {
        viewer = new SourceViewer(parent, null, SWT.V_SCROLL | SWT.H_SCROLL);
        viewer.configure(viewerConfig);
        viewer.setDocument(new Document());
        viewer.getTextWidget().setEditable(false);
        viewer.getTextWidget().setFont(JFaceResources.getTextFont());
        viewer.getTextWidget().setBackground(new Color(Display.getCurrent(), 0xff, 0xff, 0xe1));
        getSite().getPage().addPostSelectionListener(selectionListener);
        
        createActions();
    }

    /* (non-Javadoc)
	 * @see org.eclipse.ui.part.ViewPart#init(org.eclipse.ui.IViewSite, org.eclipse.ui.IMemento)
	 */
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException 
	{
		super.init(site, memento);
        
        if(memento == null)
        {
            expandTcl.setChecked(false);
            return;
        }
		
		String checked = memento.getString(ExpandTclToggleAction.ID);
        
        if(checked == null)
        {
        	return;
        }
        
        if(checked.equals("true"))
        {
        	expandTcl.setChecked(true);
        }
        else
        {
        	expandTcl.setChecked(false);
        }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.ViewPart#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento memento) 
	{
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
		IContributionItem[] items = toolbarManager.getItems();
		
		for(IContributionItem item:items)
		{
			//save the state of all the action items in the toolbar
			if(item instanceof ActionContributionItem)
			{
				ActionContributionItem actionItem = (ActionContributionItem) item;
				
				String id = actionItem.getId();
				IAction action = actionItem.getAction();
				
				boolean checked = action.isChecked();
				
				if(checked)
				{
					memento.putString(id, "true");
				}
				else
				{
					memento.putString(id, "false");
				}
			}
		}
		
		super.saveState(memento);
	}

	/* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus()
    {
        viewer.getTextWidget().setFocus();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#dispose()
     */
    @Override
    public void dispose()
    {
        getSite().getPage().removePostSelectionListener(selectionListener);
        super.dispose();
    }

    private void createActions()
    {
        IActionBars actionBars = getViewSite().getActionBars();
        
        IMenuManager menuManager = actionBars.getMenuManager();
        menuManager.add(expandTcl);
        
        IToolBarManager toolbarManager = actionBars.getToolBarManager();
        toolbarManager.add(expandTcl);
    }
    
    private void selectionChanged(IWorkbenchPart part, ISelection selection)
    {
        Object o = null;
        if(selection instanceof IStructuredSelection)
        {
            IStructuredSelection ss = (IStructuredSelection) selection;
            o = !selection.isEmpty() ? ss.getFirstElement() : null;
        }
        else if(part instanceof SoarEditor)
        {
            o = ((SoarEditor) part).getElementAtCaretOffset();
        }
        
        if(o == null)
        {
            viewer.getDocument().set("No selection.");
            setContentDescription("");
            return;
        }
        
        if(!(o instanceof IAdaptable))
        {
            viewer.getDocument().set("No source code to display.");
            setContentDescription("");
            return;
        }
        
        selectedSourceRef = (ISoarSourceReference) ((IAdaptable) o).getAdapter(ISoarSourceReference.class);
        
        update();
    }
    
    private void update()
    {
        if(selectedSourceRef == null)
        {
            viewer.getDocument().set("No source code to display.");
            setContentDescription("");
            return;
        }
        
        // TODO: This is a temporary hack to avoid a synchronization problem :(
        if(selectedSourceRef instanceof ISoarElement)
        {
            if(((ISoarElement) selectedSourceRef).getParent() == null)
            {
                System.out.println("##### Soar element has no parent! " + selectedSourceRef);
                return;
            }
        }
        
        try
        {
            String source = "";
            if(selectedSourceRef instanceof ISoarProduction)
            {
                ISoarProduction p = (ISoarProduction) selectedSourceRef;
                if(expandTcl.isChecked())
                {
                    source = p.getExpandedSource();
                }
                if(source == null || source.length() == 0)
                {
                    source = selectedSourceRef.getSource();
                }
            }
            else
            {
                source = selectedSourceRef.getSource();
            }
            viewer.getDocument().set(source);
        }
        catch (SoarModelException e1)
        {
            viewer.getDocument().set("Error: " + e1);
        }
        
        String desc = "";
        if(selectedSourceRef instanceof ISoarFile)
        {
            ISoarFile file = (ISoarFile) selectedSourceRef;
            desc = file.getPath().toString();
        }
        else if(selectedSourceRef instanceof ISoarProduction)
        {
            ISoarProduction p = (ISoarProduction) selectedSourceRef;
            ISoarFileAgentProxy file = (ISoarFileAgentProxy) p.getParent();
            desc = (file != null ? file.getPath().toString() : "(detached)") + "  " + p.getProductionName();
        }
        else if(selectedSourceRef instanceof ITclProcedure)
        {
            ITclProcedure p = (ITclProcedure) selectedSourceRef;
            ISoarFileAgentProxy file = (ISoarFileAgentProxy) p.getParent();
            desc = (file != null ? file.getPath().toString() : "(detached)") + "  " + p.getProcedureName();
        }
        else if(selectedSourceRef instanceof ITclCommand)
        {
            ITclCommand c = (ITclCommand) selectedSourceRef;
            ISoarFileAgentProxy file = (ISoarFileAgentProxy) c.getParent();
            desc = (file != null ? file.getPath().toString() : "(detached)") + "  " + c.getCommandName();
        }
        
        setContentDescription(desc);        
    }
    
    /**
     * Action that toggles Tcl expansion on and off.
     */
    private class ExpandTclToggleAction extends Action
    {
    	static final String ID = "com.soartech.soar.ide.ui.views.source.ExpandTclToggleAction";
    	
        public ExpandTclToggleAction()
        {
            super("Expand Tcl macros", Action.AS_CHECK_BOX);
            setToolTipText("Enable/disable expansion of Tcl macros in Soar source code");
            setImageDescriptor(SoarEditorPluginImages.getDescriptor(SoarEditorPluginImages.IMG_EXPAND));
            setId(ID);
        }

        public void run()
        {
            update();
        }
    }
}
