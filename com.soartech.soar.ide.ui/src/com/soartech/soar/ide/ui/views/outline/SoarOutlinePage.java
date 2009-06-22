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
package com.soartech.soar.ide.ui.views.outline;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import com.soartech.soar.ide.core.model.ISoarElement;
import com.soartech.soar.ide.core.model.ISoarSourceRange;
import com.soartech.soar.ide.core.model.ISoarSourceReference;
import com.soartech.soar.ide.core.model.SoarModelException;
import com.soartech.soar.ide.ui.SoarEditorUIPlugin;
import com.soartech.soar.ide.ui.SoarUiTools;
import com.soartech.soar.ide.ui.actions.OutlineSortingAction;
import com.soartech.soar.ide.ui.editors.text.SoarEditor;
import com.soartech.soar.ide.ui.views.SoarLabelProvider;

/**
 * <code>SoarOutlinePage</code> creates an outline page for the
 * <code>SoarEditor</code>
 *
 * @author annmarie.steichmann@soartech.com
 * @version $Revision: 578 $ $Date: 2009-06-22 13:05:30 -0400 (Mon, 22 Jun 2009) $
 */
public class SoarOutlinePage extends ContentOutlinePage
{
    private SoarEditor editor = null;
    private TreeViewer viewer = null;
    private MenuManager menuMgr = null;
    private boolean settingEditorPosition = false;
        
    /**
     * Sorter for this view.
     */
    public static final SoarOutlineSorter SORTER = new SoarOutlineSorter();
    
    /**
     * Constructor for a <code>SoarOutlinePage</code> object.
     * 
     * @param editor The associated <code>SoarEditor</code>
     */
    public SoarOutlinePage( SoarEditor editor ) 
    {
        this.editor = editor;
    }
    
    /**
     * Called when the working copy has been reconciled and the outline view 
     * should be updated 
     */
    public void workingCopyReconciled()
    {
        if(viewer != null && !viewer.getControl().isDisposed())
        {
            viewer.refresh();
        }
    }
    
    private ISoarSourceReference getSelectedElement()
    {
        return SoarUiTools.getValueFromSelection(getSelection(), ISoarSourceReference.class);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.views.contentoutline.ContentOutlinePage#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl( Composite parent ) {
        
        super.createControl( parent );
        
        viewer = getTreeViewer();
        viewer.setContentProvider( new SoarOutlineContentProvider(editor.getFoldingSupport()) );
        viewer.setLabelProvider( SoarLabelProvider.createFullLabelProvider(null) );
        viewer.addSelectionChangedListener( this );
        viewer.setInput( editor.getSoarFileWorkingCopy() );
        
        createContextMenu();
        
        IToolBarManager toolbarManager = getSite().getActionBars().getToolBarManager();
        toolbarManager.add(new OutlineSortingAction(viewer));
    }
    
    private void createContextMenu()
    {
        // Configure the context menu.
        menuMgr = new MenuManager(); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        
        menuMgr.addMenuListener( new IMenuListener() {

            public void menuAboutToShow(IMenuManager manager) {
                
                populateMenu();
            }
        } );
        
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getTree().setMenu(menu);
        getSite().registerContextMenu(SoarEditorUIPlugin.PLUGIN_ID + ".outline", 
                                      menuMgr, viewer);
    }
    
    private void populateMenu() {
        // Add the standard additions point so that other plugins can add
        // to this menu.
        menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS + "-end"));
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.views.contentoutline.ContentOutlinePage#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
     */
    @Override
    public void selectionChanged( SelectionChangedEvent event ) 
    {
        super.selectionChanged( event );
        
        ISoarSourceReference ref = getSelectedElement();
        if(ref == null)
        {
            return;
        }
        
        ISoarSourceRange range;
        try
        {
            range = ref.getSourceRange();
            settingEditorPosition = true;
            editor.selectAndReveal(range.getOffset(), 0 /* just highlight line, not all text */);
            settingEditorPosition = false;
        }
        catch (SoarModelException e)
        {
            SoarEditorUIPlugin.log(e.getStatus());
        }
    }

    /**
     * Set the selection of the outline to the given element
     * 
     * @param e The element to select
     */
    public void setElementSelection(ISoarElement e)
    {
        if(viewer == null || settingEditorPosition)
        {
            return;
        }
        viewer.removeSelectionChangedListener(this);
        viewer.setSelection(new StructuredSelection(e));
        viewer.addSelectionChangedListener(this);
    }
}