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
package com.soartech.soar.ide.ui.views.datamap;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

import com.soartech.soar.ide.core.SoarCorePlugin;
import com.soartech.soar.ide.core.model.ISoarAgent;
import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarModel;
import com.soartech.soar.ide.core.model.ISoarModelListener;
import com.soartech.soar.ide.core.model.ISoarProduction;
import com.soartech.soar.ide.core.model.ISoarProject;
import com.soartech.soar.ide.core.model.SoarModelEvent;
import com.soartech.soar.ide.core.model.datamap.CompositeSoarDatamapListener;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapAttribute;
import com.soartech.soar.ide.core.model.datamap.ISoarDatamapListener;
import com.soartech.soar.ide.core.model.datamap.SoarDatamapEvent;
import com.soartech.soar.ide.ui.SoarEditorPluginImages;
import com.soartech.soar.ide.ui.SoarUiTools;
import com.soartech.soar.ide.ui.views.ElementDoubleClickListener;
import com.soartech.soar.ide.ui.views.SoarLabelProvider;

/**
 * @author ray
 */
public class SoarDatamapView extends ViewPart
{
    public static final String ID = "com.soartech.soar.ide.ui.views.datamap.SoarDatamapView";
    
    private TreeViewer treeViewer;
    private TableViewer listViewer; // ListViewer does not support icons
    private ModelListener modelListener = new ModelListener();
    private CompositeSoarDatamapListener datamapListener;
    private DrillDownAdapter drilldown;
    
    private DatamapFilterActionProvider filterProvider;
    private DatamapRemoveFilterAction removeFilterAction;

    //view orientations for the datamap's sash form
	static final int VIEW_ORIENTATION_VERTICAL= 0;
	static final int VIEW_ORIENTATION_HORIZONTAL= 1;
	static final int VIEW_ORIENTATION_AUTOMATIC = 2;
	
	static final String STORE_ORIENTATION = "sashform.orientation";
	
	/**
	 * The current orientation; either <code>VIEW_ORIENTATION_HORIZONTAL</code>
	 * <code>VIEW_ORIENTATION_VERTICAL</code>, or <code>VIEW_ORIENTATION_AUTOMATIC</code>.
	 */
	private int fOrientation = VIEW_ORIENTATION_AUTOMATIC;
	/**
	 * The current orientation; either <code>VIEW_ORIENTATION_HORIZONTAL</code>
	 * <code>VIEW_ORIENTATION_VERTICAL</code>.
	 */
	private int fCurrentOrientation;
	
	private ToggleOrientationAction[] fToggleOrientationActions;
	
	/**
	 * The parent composite of this view part.
	 */
	private Composite parent;

	/**
	 * The sash form widget which holds the datamap widgets.
	 */
	private SashForm sashForm;
	
	private IMemento memento;
	
    private boolean isDisposed()
    {
        return treeViewer == null ||
               treeViewer.getControl() == null ||
               treeViewer.getControl().isDisposed();
    }
    
    /* (non-Javadoc)
	 * @see org.eclipse.ui.part.ViewPart#init(org.eclipse.ui.IViewSite, org.eclipse.ui.IMemento)
	 */
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		this.memento = memento;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.ViewPart#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		memento.putString( STORE_ORIENTATION, Integer.toString( fOrientation ) );
	}

    private Object getInitialTreeInput()
    {
        return SoarCorePlugin.getDefault().getSoarModel();
    }
    
	/* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent)
    {
    	this.parent = parent;
    	
        ISoarModel soarModel = SoarCorePlugin.getDefault().getSoarModel();
        soarModel.addListener(modelListener);
        datamapListener = new CompositeSoarDatamapListener(soarModel);
        datamapListener.addListener(new ISoarDatamapListener() {

            public void onDatamapChanged(SoarDatamapEvent event)
            {
                safeRefresh(null, event);
            }});
        
        addResizeListener(parent);
        
        createSashForm(parent);
        
        createContextMenu();
        
        configureToolBar();
        
        IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
        
        removeFilterAction = new DatamapRemoveFilterAction(this);
        toolbarManager.add(removeFilterAction);
        
        filterProvider = new DatamapFilterActionProvider(this);
        treeViewer.addSelectionChangedListener(filterProvider);

        drilldown = new DrillDownAdapter(treeViewer);
        drilldown.addNavigationActions(getViewSite().getActionBars().getMenuManager());
        drilldown.addNavigationActions(getViewSite().getActionBars().getToolBarManager());
    }
    
    public void clearFilter()
    {
        for (ViewerFilter filter : treeViewer.getFilters()) { treeViewer.removeFilter(filter); }
        for (ViewerFilter filter : listViewer.getFilters()) { listViewer.removeFilter(filter); }
        
        removeFilterAction.disable();
    }

    public void setFilter(ViewerFilter filter, String filterDescription)
    {
        clearFilter();
        treeViewer.addFilter(filter);
        listViewer.addFilter(filter);
        
        removeFilterAction.enable(filterDescription);
    }
    
    
    /**
     * Create the sash form widget to hold the datamap widgets.
     * 
     * @param parent
     * @return
     */
    private void createSashForm(Composite parent)
    {
    	sashForm = new SashForm(parent, SWT.VERTICAL);
    	
    	createTreeViewer(sashForm);
    	createListViewer(sashForm);
    	
    	sashForm.setWeights(new int[]{50, 50});
        
       	if ( memento == null ) return;
    	String value = memento.getString( STORE_ORIENTATION );
    	if ( value == null ) return;
    	fOrientation = Integer.parseInt( value );
    }
    
    /**
     * Create the tree viewer widget part of the datamap.
     * 
     * @param parent
     * @param soarModel
     */
    private void createTreeViewer(Composite parent)
    {
    	treeViewer = new TreeViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        treeViewer.getTree().setHeaderVisible(true);
        treeViewer.getTree().setLinesVisible(true);
        
        TreeColumn column0 = new TreeColumn(treeViewer.getTree(), SWT.NONE);
        column0.setText("Attribute");
        column0.setWidth(200);
        column0.setResizable(true);
        
        TreeColumn column1 = new TreeColumn(treeViewer.getTree(), SWT.NONE);
        column1.setText("Values");
        column1.setResizable(true);
        column1.setWidth(200);
        
        treeViewer.setContentProvider(new SoarDatamapContentProvider());
        // Can't use the full label provider because it hides the column stuff. 
        treeViewer.setLabelProvider(SoarLabelProvider.createFastLabelProvider(null));
        treeViewer.setInput(getInitialTreeInput());
        treeViewer.setSorter(new ViewerSorter());
        
        treeViewer.addSelectionChangedListener(new ISelectionChangedListener(){

            public void selectionChanged(SelectionChangedEvent event)
            {
                IStructuredSelection ss = (IStructuredSelection) event.getSelection();
                treeSelectionChanged(ss);
            }});
        treeViewer.addDoubleClickListener(new ElementDoubleClickListener());
    }
    
    /**
     * Create the list viewer widget part of the datamap.
     * 
     * @param parent
     */
    private void createListViewer(Composite parent)
    {
    	listViewer = new TableViewer(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        listViewer.setContentProvider(new ProductionListContentProvider());
        listViewer.setLabelProvider(SoarLabelProvider.createFullLabelProvider(null));
        listViewer.addDoubleClickListener(new ElementDoubleClickListener());
        listViewer.setSorter(new ViewerSorter());
        
        getSite().setSelectionProvider(listViewer);
    }
    
    /**
     * Add a resize listener to the sash form. This listener will adjust
     * the orientation of the sash's children as it is resized.
     * 
     * @param parent
     */
    private void addResizeListener(Composite parent)
    {
    	parent.addControlListener(new ControlListener() {
    		
			public void controlMoved(ControlEvent e) { }
			
			public void controlResized(ControlEvent e) {
				computeOrientation();
			}
    	});
    }
    
    /**
     * Compute the orientation of the sash form's children according
     * to its current size.
     */
    private void computeOrientation()
    {
		if (fOrientation != VIEW_ORIENTATION_AUTOMATIC) {
			fCurrentOrientation= fOrientation;
			setOrientation(fCurrentOrientation);
		} else {
			Point size = parent.getSize();
			if (size.x != 0 && size.y != 0) {
				if (size.x > size.y) {
					setOrientation(VIEW_ORIENTATION_HORIZONTAL);
				} else {
					setOrientation(VIEW_ORIENTATION_VERTICAL);
				}
			}
		}
    }
    
    /**
     * Set the orientation of the sash form's children to the given value.
     * 
     * @param orientation
     */
    private void setOrientation(int orientation)
    {
    	if(sashForm == null || sashForm.isDisposed())
    	{
    		return;
    	}
    	
    	if(orientation == VIEW_ORIENTATION_HORIZONTAL)
    	{
    		sashForm.setOrientation(SWT.HORIZONTAL);
    	}
    	else
    	{
    		sashForm.setOrientation(SWT.VERTICAL);
    	}
    }
    
    private void createContextMenu()
    {
        MenuManager mgr = new MenuManager();
        mgr.setRemoveAllWhenShown(true);
        mgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager mgr) {
                fillContextMenu(mgr);
            }
        });
        Menu menu = mgr.createContextMenu(treeViewer.getControl());
        treeViewer.getControl().setMenu(menu);
        getSite().registerContextMenu(mgr, treeViewer);
    }
    
    /**
     * Notifies this listener that the menu is about to be shown by
     * the given menu manager.
     *
     * @param manager the menu manager
     */
    void fillContextMenu(IMenuManager manager)
    {
        drilldown.addNavigationActions(manager);
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        IContributionItem filterMenu = filterProvider.createFilterMenu();
        if (filterMenu != null) manager.add(filterMenu);
        
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS + "-end"));
        manager.add(new Separator());
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus()
    {
        if(!isDisposed())
        {
            treeViewer.getControl().setFocus();
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#dispose()
     */
    @Override
    public void dispose()
    {
        datamapListener.dispose();
        SoarCorePlugin.getDefault().getSoarModel().addListener(modelListener);
        super.dispose();
    }
    
    private void treeSelectionChanged(IStructuredSelection ss)
    {
        // Calculate intersection of supporting productions to display in list.
        List<ISoarDatamapAttribute> attrs = SoarUiTools.getValuesFromSelection(ss, ISoarDatamapAttribute.class);
        Set<ISoarProduction> prods = new HashSet<ISoarProduction>();
        for(ISoarDatamapAttribute a : attrs)
        {
            prods.addAll(a.getSupportingProductions());
        }
        for(ISoarDatamapAttribute a : attrs)
        {
            prods.retainAll(a.getSupportingProductions());
        }
        listViewer.setInput(prods);
    }

    private class ProductionListContentProvider implements IStructuredContentProvider
    {

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
         */
        public Object[] getElements(Object inputElement)
        {
            if(!(inputElement instanceof Set))
            {
                return Collections.EMPTY_LIST.toArray();
            }
            
            return ((Set<?>) inputElement).toArray();
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
    
    /**
     * Determines if the current tree input (root) is has been removed from the 
     * model or the datamap. This is possible if the user has drilled down into
     * the datamap. If it has been removed, we have to reset the tree back to
     * its normal state.
     *  
     * @param sme A model event, possibly null
     * @param sde A datamap event, possible null
     * @return True if the current tree input has been removed
     */
    private boolean treeRequiresReset(final SoarModelEvent sme, final SoarDatamapEvent sde)
    {
        Object input = treeViewer.getInput();
        boolean reset = false;
        if(sme != null && sme.getType() == SoarModelEvent.ELEMENTS_REMOVED)
        {
            for(ISoarElement e : sme.getElements())
            {
                if(e == input)
                {
                    reset = true;
                    break;
                }
            }
        }
        if(!reset && sde != null)
        {
            for(ISoarDatamapAttribute a : sde.removed)
            {
                if(a == input)
                {
                    reset = true;
                    break;
                }
            }
        }
        return reset;
    }
    
    /**
     * Safely (threads) refresh the view in response to a model or datamap event.
     * 
     * @param sme A model event, possibly null
     * @param sde A datamap event, possibly null
     */
    private void safeRefresh(final SoarModelEvent sme, final SoarDatamapEvent sde)
    {
        Display.getDefault().asyncExec(new Runnable(){

            public void run()
            {
                if(!isDisposed())
                {
                    if(!treeRequiresReset(sme, sde))
                    {
                        treeViewer.refresh();
                        // Force production list to repopulate
                        treeViewer.setSelection(treeViewer.getSelection());
                    }
                    else
                    {
                        treeViewer.setInput(getInitialTreeInput());
                        drilldown.reset();
                    }
                }
            }});

    }
    
    private class ModelListener implements ISoarModelListener
    {
        /* (non-Javadoc)
         * @see com.soartech.soar.ide.core.model.ISoarModelListener#onEvent(com.soartech.soar.ide.core.model.SoarModelEvent)
         */
        public void onEvent(SoarModelEvent event)
        {
            for(ISoarElement e : event.getElements())
            {
                if(e instanceof ISoarProject || e instanceof ISoarAgent)
                {
                    safeRefresh(event, null);
                    break;
                }
            }
        }
    }
    
    private void configureToolBar()
    {

        IActionBars actionBars = getViewSite().getActionBars();
        IMenuManager viewMenu = actionBars.getMenuManager();

        fToggleOrientationActions = new ToggleOrientationAction[] {
                new ToggleOrientationAction(this, VIEW_ORIENTATION_VERTICAL),
                new ToggleOrientationAction(this, VIEW_ORIENTATION_HORIZONTAL),
                new ToggleOrientationAction(this, VIEW_ORIENTATION_AUTOMATIC) };

        MenuManager layoutSubMenu = new MenuManager("Layout");
        for (int i = 0; i < fToggleOrientationActions.length; ++i)
        {
            layoutSubMenu.add(fToggleOrientationActions[i]);
        }
        viewMenu.add(layoutSubMenu);

        actionBars.updateActionBars();

        if (fOrientation == VIEW_ORIENTATION_VERTICAL)
        {
            fToggleOrientationActions[0].setChecked(true);
        }
        else if (fOrientation == VIEW_ORIENTATION_HORIZONTAL)
        {
            fToggleOrientationActions[1].setChecked(true);
        }
        else
        {
            fToggleOrientationActions[2].setChecked(true);
        }

    }

	private class ToggleOrientationAction extends Action
    {

        private final int fActionOrientation;

        public ToggleOrientationAction(SoarDatamapView v, int orientation)
        {
            super("", AS_RADIO_BUTTON); //$NON-NLS-1$
            if (orientation == VIEW_ORIENTATION_HORIZONTAL)
            {
                setText("Horizontal View Orientation");
                setImageDescriptor(SoarEditorPluginImages
                        .getDescriptor(SoarEditorPluginImages.IMG_HORIZONTAL_ORIENTATION));
            }
            else if (orientation == VIEW_ORIENTATION_VERTICAL)
            {
                setText("Vertical View Orientation");
                setImageDescriptor(SoarEditorPluginImages
                        .getDescriptor(SoarEditorPluginImages.IMG_VERTICAL_ORIENTATION));
            }
            else if (orientation == VIEW_ORIENTATION_AUTOMATIC)
            {
                setText("Automatic View Orientation");
                setImageDescriptor(SoarEditorPluginImages
                        .getDescriptor(SoarEditorPluginImages.IMG_AUTOMATIC_ORIENTATION));
            }
            fActionOrientation = orientation;
        }

        public int getOrientation()
        {
            return fActionOrientation;
        }

        public void run()
        {
            if (isChecked())
            {
                fOrientation = fActionOrientation;
                computeOrientation();
            }
        }
    }
}
