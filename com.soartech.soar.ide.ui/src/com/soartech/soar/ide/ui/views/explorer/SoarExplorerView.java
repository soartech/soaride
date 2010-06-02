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
package com.soartech.soar.ide.ui.views.explorer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.model.ISoarModelListener;
import com.soartech.soar.ide.core.model.SoarModelEvent;
import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseConnection;
import com.soartech.soar.ide.core.sql.ISoarDatabaseEventListener;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.SoarUiModelTools;
import com.soartech.soar.ide.ui.SoarUiTools;
import com.soartech.soar.ide.ui.views.SoarLabelProvider;

/**
 * Implementation of a ViewPart representing the Package Explorer for 
 * the Soar IDE.
 * 
 * @author aron
 */
public class SoarExplorerView extends ViewPart 
							  implements ISoarModelListener,
								 		 IDoubleClickListener,
								 		 ISoarDatabaseEventListener
{
    public static final String ID = "com.soartech.soar.ide.ui.views.SoarExplorerView";
    
	private TreeViewer viewer;
	
	/**
	 * A copy of the memento for the soar explorer. The memento persists
	 * the state of the view's ui elements.
	 */
	private IMemento memento;
	
	/**
	 * The content provider for the 'productions view' structure.
	 */
	private SoarExplorerProductionViewContentProvider productionViewContentProvider =
		new SoarExplorerProductionViewContentProvider();
    private ILabelProvider productionViewLabelProvider = SoarLabelProvider.createFullLabelProvider(null);
	
    /**
	 * The content provider for the 'full view' structure.
	 */
	private SoarExplorerFullViewContentProvider fullViewContentProvider =
		new SoarExplorerFullViewContentProvider();
	private ILabelProvider fullViewLabelProvider = SoarLabelProvider.createFullLabelProvider(null);
	
    /**
	 * The content provider for the database view.
	 */
	private SoarExplorerDatabaseContentProvider databaseContentProvider =
		new SoarExplorerDatabaseContentProvider(true, false, true, true, true, false);
	private ILabelProvider databaseLabelProvider = SoarLabelProvider.createFullLabelProvider(null);
	
	/**
	 * The viewer filter for the soar explorer.
	 */
	private SoarExplorerFilter viewerFilter = new SoarExplorerFilter();
	
	/**
	 * The sorter for the soar explorer.
	 */
	private SoarExplorerSorter sorter = new SoarExplorerSorter();
	
    
	/**
	 * Constructor.
	 */
	public SoarExplorerView() 
	{
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) 
	{
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		
		viewer.addDoubleClickListener(this);
        viewer.setUseHashlookup(true); // this significantly improves update performance
		
		viewer.setContentProvider(databaseContentProvider);
		viewer.setLabelProvider(databaseLabelProvider);
		viewer.addFilter(viewerFilter);
		ISoarModel input = SoarCorePlugin.getDefault().getSoarModel();
        viewer.setInput(input);
        getSite().setSelectionProvider(viewer);

        createContextMenu();
        makeActions();
        
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
		
		FilterContributionItem filterContribution = new FilterContributionItem("text_filter", this);
		toolbarManager.add(filterContribution);
		
        SoarCorePlugin.getDefault().getSoarModel().getDatabase().addListener(this);
	}
    
    private void createContextMenu()
    {
//        newWizardMenu = new NewWizardMenu(getSite().getWorkbenchWindow());
        
        MenuManager mgr = new MenuManager();
        mgr.setRemoveAllWhenShown(true);
        mgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager mgr) {
                fillContextMenu(mgr);
            }
        });
        Menu menu = mgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(mgr, viewer);
        
    }
    
    private void makeActions() {
    	//doubleClickAction = null;
    }
    
    /* (non-Javadoc)
	 * @see org.eclipse.ui.part.ViewPart#init(org.eclipse.ui.IViewSite, org.eclipse.ui.IMemento)
	 */
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException 
	{
		super.init(site, memento);
		this.memento = memento;
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
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose() 
	{
		super.dispose();
	}

	/**
     * Notifies this listener that the menu is about to be shown by
     * the given menu manager.
     *
     * @param manager the menu manager
     */
    void fillContextMenu(IMenuManager manager) {
//        MenuManager newMenu = new MenuManager("Ne&w");
//        manager.add(newMenu);
//        newMenu.add(newWizardMenu);
        
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS + "-end"));
        manager.add(new Separator());
    }
	
	/**
	 * Update the package explorer according to the new model.
	 */
	public void update()
	{
		if(viewer != null)
		{
            Control control = viewer.getControl();
            if(control != null && !control.isDisposed())
            {
    			//save the state of the expanded tree elements
                Object[] expandedElements = viewer.getExpandedElements();
                
                control.setRedraw(false);
    			
                viewer.refresh();
    			
    			//re-expand the tree to it's previous state
                viewer.setExpandedElements(expandedElements);
                control.setRedraw(true);
            }
		}
	}
	
	/**
	 * Switch the structure of the package explorer.
	 *
	 */
	public void switchViewStructure(boolean showFullView)
	{
		if(showFullView)
		{
            viewer.setContentProvider(fullViewContentProvider);
            viewer.setLabelProvider(fullViewLabelProvider);
		}
		else
		{
			viewer.setContentProvider(productionViewContentProvider);
            viewer.setLabelProvider(productionViewLabelProvider);
		}

		update();
	}
	
	/**
	 * Switch the viewer sorter to the given sort value.
	 *
	 * @param checked Sort if true, unsort if false.
	 */
	public void switchViewerSorter(boolean sort)
	{
		if(sort)
		{
			viewer.setSorter(sorter);
		}
		else
		{
			viewer.setSorter(null);
		}
	}
	
	/**
	 * Set the filter to show/hide the procedures. Update the viewer.
	 * 
	 */
	public void showProcedures(boolean show)
	{
		viewerFilter.showProcedures(show);
		
		update();
	}
	
	/**
	 * Set the filter to show/hide the productions. Update the viewer.
	 * 
	 */
	public void showProductions(boolean show)
	{
		viewerFilter.showProductions(show);
		
		update();
	}
	
    /**
     * Set the filter string.
     * 
     * @param text The new filter string
     */
    void setFilterString(String text)
    {
        viewerFilter.setFilterString(text);
        update();
    }
    
	/**
	 * @return the memento
	 */
	public IMemento getMemento() 
	{
		return memento;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() 
	{
		if(viewer != null)
        {
            viewer.getControl().setFocus();
        }
	}
	
	/**
	 * Return the correctly selected SoarElement.
	 * 
	 * @return The SoarElement
	 */
	private ISoarElement getSelectedSoarElement()
    {
		if(viewer == null)
		{
			return null;
		}
        
        return SoarUiTools.getValueFromSelection(viewer.getSelection(), ISoarElement.class);
    }

	/* (non-Javadoc)
	 * @see com.soartech.soar.ide.core.model.ISoarModelListener#onEvent(com.soartech.soar.ide.core.model.SoarModelEvent)
	 */
	public void onEvent(SoarModelEvent event) 
	{
		Display.getDefault().asyncExec(new Runnable(){

			public void run() {
				update();
			}
		}
		);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IDoubleClickListener#doubleClick(org.eclipse.jface.viewers.DoubleClickEvent)
	 */
	public void doubleClick(DoubleClickEvent event) 
	{
		ISoarDatabaseTreeItem item = SoarUiTools.getValueFromSelection(viewer.getSelection(), ISoarDatabaseTreeItem.class);
		if (item == null) {
			return;
		}
		
		if (item instanceof SoarDatabaseRow) {
	        IWorkbench workbench = SoarEditorUIPlugin.getDefault().getWorkbench();
	        IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
			SoarDatabaseRow selectedRow = (SoarDatabaseRow) item;
			Table selectedTable = selectedRow.getTable();
			if (selectedTable == Table.RULES) {
				try {
					SoarUiModelTools.showRuleInEditor(page, selectedRow);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} else if (selectedTable == Table.PROBLEM_SPACES) {
				try {
					SoarUiModelTools.showProblemSpaceInEditor(page, selectedRow);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onEvent(SoarDatabaseEvent event, SoarDatabaseConnection db) {
		
		//Control control = viewer.getControl();
		//control.setRedraw(false);
		Object[] elements = viewer.getExpandedElements();
		TreePath[] treePaths = viewer.getExpandedTreePaths();
        viewer.refresh();
		viewer.setExpandedElements(elements);
		viewer.setExpandedTreePaths(treePaths);
		//control.setRedraw(true);
		
		// hack
		// Freezes when tree is of infinite depth.
		// viewer.expandAll();
	}
	
	public TreeViewer getTreeViewer() {
		return viewer;
	}

}
