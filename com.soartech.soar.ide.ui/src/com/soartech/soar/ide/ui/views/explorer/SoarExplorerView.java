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

import java.awt.event.KeyEvent;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.model.ISoarModelListener;
import com.soartech.soar.ide.core.model.SoarModelEvent;
import com.soartech.soar.ide.core.sql.ISoarDatabaseTreeItem;
import com.soartech.soar.ide.core.sql.SoarDatabaseConnection;
import com.soartech.soar.ide.core.sql.ISoarDatabaseEventListener;
import com.soartech.soar.ide.core.sql.SoarDatabaseEvent;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow;
import com.soartech.soar.ide.core.sql.SoarDatabaseRowFolder;
import com.soartech.soar.ide.core.sql.SoarDatabaseRow.Table;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.SoarUiModelTools;
import com.soartech.soar.ide.ui.SoarUiTools;
import com.soartech.soar.ide.ui.actions.explorer.AddChildRowAction;
import com.soartech.soar.ide.ui.actions.explorer.AddOperatorTemplateChildrenAction;
import com.soartech.soar.ide.ui.actions.explorer.AddRuleTemplateChildrenAction;
import com.soartech.soar.ide.ui.actions.explorer.AddSubstateAction;
import com.soartech.soar.ide.ui.actions.explorer.ExportSoarDatabaseRowAction;
import com.soartech.soar.ide.ui.actions.explorer.GenerateDatamapAction;
import com.soartech.soar.ide.ui.actions.explorer.RemoveJoinFromParentAction;
import com.soartech.soar.ide.ui.views.SoarLabelProvider;
import com.soartech.soar.ide.ui.views.explorer.DragAndDrop.SoarDatabaseExplorerDragAdapter;
import com.soartech.soar.ide.ui.views.explorer.DragAndDrop.SoarDatabaseExplorerDropAdapter;

import edu.umich.soar.debugger.jmx.SoarCommandLineMXBean;

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
    
	private TreeViewer tree;
	
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
	
	private ILabelProvider databaseLabelProvider = SoarLabelProvider.createFullLabelProvider(null);
	
	/**
	 * Constructor.
	 */
	public SoarExplorerView() 
	{
		super();
	}

	@Override
	public void createPartControl(Composite parent) 
	{		
		tree = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		
		tree.addDoubleClickListener(this);
        tree.setUseHashlookup(true); // this significantly improves update performance
		
		tree.setContentProvider(new SoarExplorerDatabaseContentProvider());
		tree.setLabelProvider(databaseLabelProvider);
		ISoarModel input = SoarCorePlugin.getDefault().getSoarModel();
        tree.setInput(input);
        getSite().setSelectionProvider(tree);

        createContextMenu();
        makeActions();
        SoarCorePlugin.getDefault().getSoarModel().getDatabase().addListener(this);
        tree.addDragSupport(DND.DROP_MOVE, new Transfer[] {LocalSelectionTransfer.getTransfer()}, new SoarDatabaseExplorerDragAdapter());
        tree.addDropSupport(DND.DROP_MOVE, new Transfer[] {LocalSelectionTransfer.getTransfer()}, new SoarDatabaseExplorerDropAdapter(tree));
   	}
	
	private ArrayList<Action> actionsForRow(SoarDatabaseRow row, TreeSelection selection) {
		ArrayList<Action> ret = new ArrayList<Action>();
		Table table = row.getTable();
		if (table == Table.AGENTS) {
			//ret.add(new GenerateAgentStructureActionDelegate(row));
		}
		if (table == Table.PROBLEM_SPACES) {
			ret.add(new AddChildRowAction(row, Table.OPERATORS, row, tree, false));
			ret.add(new AddOperatorTemplateChildrenAction(row, tree));
			ret.add(new AddChildRowAction(row, Table.RULES, row, tree, false));
			ret.add(new AddRuleTemplateChildrenAction(row, tree));
			ret.add(new AddSubstateAction(row, false, tree));
			ret.add(new GenerateDatamapAction(row));
		}
		if (table == Table.OPERATORS) {
			ret.add(new AddChildRowAction(row, Table.RULES, row, tree, false));
			ret.add(new AddRuleTemplateChildrenAction(row, tree));
		}
		RemoveJoinFromParentAction remove = new RemoveJoinFromParentAction(selection);
		if (remove.isRunnable()) {
			ret.add(remove);
		}
		ret.add(new ExportSoarDatabaseRowAction(row));
		SoarCommandLineMXBean proxy = SoarCorePlugin.getDefault().getSoarModel().getCommandLineProxy(); 
		if (proxy != null) {
			ret.add(new ExportSoarDatabaseRowAction(row, proxy));
		}
		return ret;
	}
	
	private ArrayList<Action> actionsForFolder(SoarDatabaseRowFolder folder) {
		ArrayList<Action> ret = new ArrayList<Action>();
		ret.add(new AddChildRowAction(folder.getRow(), folder.getTable(), folder, tree, false));
		return ret;
	}
	
    private void createContextMenu()
    {
    	MenuManager manager = new MenuManager();
        manager.addMenuListener(new IMenuListener() {

			@Override
			public void menuAboutToShow(IMenuManager manager) {
				manager.removeAll();
				ISelection selection = tree.getSelection();
				if (selection instanceof TreeSelection) {
					TreeSelection ts = (TreeSelection) selection;
					Object obj = ts.getFirstElement();
					if (obj instanceof SoarDatabaseRow) {
						SoarDatabaseRow row = (SoarDatabaseRow) obj;
						ArrayList<Action> actions = actionsForRow(row, ts);
						for (Action action : actions) {
							manager.add(action);
						}
					}
					if (obj instanceof SoarDatabaseRowFolder) {
						SoarDatabaseRowFolder folder = (SoarDatabaseRowFolder) obj;
						ArrayList<Action> actions = actionsForFolder(folder);
						for (Action action : actions) {
							manager.add(action);
						}
					}
				}
			}
        	
        });
        Menu menu = manager.createContextMenu(tree.getControl());
        tree.getControl().setMenu(menu);
        getSite().registerContextMenu(manager, tree);
    }
    
    private void makeActions() {

		tree.getControl().addKeyListener(new org.eclipse.swt.events.KeyListener() {

			@Override
			public void keyReleased(org.eclipse.swt.events.KeyEvent event) {

			}

			@Override
			public void keyPressed(org.eclipse.swt.events.KeyEvent event) {
				ISelection selection = tree.getSelection();
				if (selection instanceof TreeSelection) {
					TreeSelection ts = (TreeSelection) selection;
					if (event.keyCode == KeyEvent.VK_DELETE && event.stateMask == (event.stateMask | SWT.CONTROL)) {
						for (Object element : ts.toArray()) {
							if (element instanceof SoarDatabaseRow) {
								SoarDatabaseRow selectedRow = (SoarDatabaseRow) element;
								Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
								String title = "Delete item?";
								org.eclipse.swt.graphics.Image image = shell.getDisplay().getSystemImage(SWT.ICON_QUESTION);
								String message = "Are you sure you want to delete \"" + selectedRow.getName() + "\"?\nThis action cannot be undone.";
								String[] labels = new String[] { "OK", "Cancel" };
								MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.QUESTION, labels, 0);
								int result = dialog.open();
								if (result == 1) {
									return;
								}
								selectedRow.deleteAllChildren(true);
								tree.refresh();
							}
						}
					}
					
					else if (event.keyCode == KeyEvent.VK_DELETE) {
						for (Object element : ts.toArray()) {
							if (element instanceof SoarDatabaseRow) {
								RemoveJoinFromParentAction action = new RemoveJoinFromParentAction(ts);
								if (action.isRunnable()) {
									Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
									String title = "Remove item?";
									org.eclipse.swt.graphics.Image image = shell.getDisplay().getSystemImage(SWT.ICON_QUESTION);
									String message = "Are you sure you want to remove \"" + action.row.getName() + "\" from \"" + action.parent.getName() + "\"?";
									String[] labels = new String[] { "OK", "Cancel" };
									MessageDialog dialog = new MessageDialog(shell, title, image, message, MessageDialog.QUESTION, labels, 0);
									int result = dialog.open();
									if (result == 1) {
										return;
									}
									action.run();
								}
							}
						}
					}
				}
			}
		});
	}
    
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException 
	{
		super.init(site, memento);
		this.memento = memento;
	}

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
		if(tree != null)
		{
            Control control = tree.getControl();
            if(control != null && !control.isDisposed())
            {
    			//save the state of the expanded tree elements
                Object[] expandedElements = tree.getExpandedElements();
                
                control.setRedraw(false);
    			
                tree.refresh();
    			
    			//re-expand the tree to it's previous state
                tree.setExpandedElements(expandedElements);
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
            tree.setContentProvider(fullViewContentProvider);
            tree.setLabelProvider(fullViewLabelProvider);
		}
		else
		{
			tree.setContentProvider(productionViewContentProvider);
            tree.setLabelProvider(productionViewLabelProvider);
		}

		update();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() 
	{
		if(tree != null)
        {
            tree.getControl().setFocus();
        }
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
		ISoarDatabaseTreeItem item = SoarUiTools.getValueFromSelection(tree.getSelection(), ISoarDatabaseTreeItem.class);
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
			} else if (selectedTable == Table.OPERATORS) {
				try {
					SoarUiModelTools.showOperatorInEditor(page, selectedRow);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} else if (selectedTable == Table.PROBLEM_SPACES) {
				try {
					SoarUiModelTools.showProblemSpaceInEditor(page, selectedRow);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} else if (selectedTable == Table.AGENTS) {
				try {
					SoarUiModelTools.showAgentInEditor(page, selectedRow);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onEvent(SoarDatabaseEvent event, SoarDatabaseConnection db) {
		
		if (event.type == SoarDatabaseEvent.Type.DATABASE_PATH_CHANGED) {
			ISoarModel input = SoarCorePlugin.getDefault().getSoarModel();
	        tree.setInput(input);
		}
		
		Object[] elements = tree.getExpandedElements();
		TreePath[] treePaths = tree.getExpandedTreePaths();
        tree.refresh();
		tree.setExpandedElements(elements);
		tree.setExpandedTreePaths(treePaths);
	}
	
	public TreeViewer getTreeViewer() {
		return tree;
	}
}
