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
package com.soartech.soar.ide.ui.perspectives;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.soartech.soar.ide.ui.views.explorer.SoarExplorerView;
import com.soartech.soar.ide.ui.views.itemdetail.SoarDatabaseItemView;
import com.soartech.soar.ide.ui.views.search.SoarDatabaseSearchResultsView;

/**
 * <code>SoarPerspective</code> create layout for Soar editing.
 *
 * @author annmarie.steichmann@soartech.com
 * @version $Revision: 578 $ $Date: 2009-06-22 13:05:30 -0400 (Mon, 22 Jun 2009) $
 */
public class SoarPerspective implements IPerspectiveFactory {

    /**
     * Constructor for a <code>SoarPerspective</code> object.
     */
    public SoarPerspective() {
        super();
    	System.out.println("Constructing Soar Perspective");
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IPerspectiveFactory#createInitialLayout(org.eclipse.ui.IPageLayout)
     */
    public void createInitialLayout( IPageLayout layout ) {
        
        String editorArea = layout.getEditorArea();
        
        layout.addActionSet("com.soartech.soar.ide.ui.soarActionSet");
        
        IFolderLayout folder = layout.createFolder("left", IPageLayout.LEFT, 0.25f, editorArea); //$NON-NLS-1$
        folder.addView(SoarExplorerView.ID);

        IFolderLayout itemFolder = layout.createFolder("item", IPageLayout.BOTTOM, 0.75f, editorArea);
        itemFolder.addView(SoarDatabaseItemView.ID);
        itemFolder.addView(SoarDatabaseSearchResultsView.ID);

        //IFolderLayout outputfolder= layout.createFolder("bottom", IPageLayout.BOTTOM, (float)0.75, editorArea); //$NON-NLS-1$
        //outputfolder.addView(IPageLayout.ID_PROBLEM_VIEW);
        //outputfolder.addView(SoarElementSourceViewer.ID);
        //outputfolder.addView("com.soartech.soar.ide.debug.ui.commandline");
        //outputfolder.addPlaceholder(NewSearchUI.SEARCH_VIEW_ID);
        //outputfolder.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);
        //outputfolder.addPlaceholder(IPageLayout.ID_BOOKMARKS);
        //outputfolder.addPlaceholder(IProgressConstants.PROGRESS_VIEW_ID);
        
        //layout.addView(IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, (float)0.75, editorArea);
        
        layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
        layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);

        // views - Soar
        layout.addShowViewShortcut(SoarExplorerView.ID);
        layout.addShowViewShortcut(SoarDatabaseItemView.ID);
        layout.addShowViewShortcut(SoarDatabaseSearchResultsView.ID);
        //layout.addShowViewShortcut(SoarDatamapView.ID);
        //layout.addShowViewShortcut(SoarElementSourceViewer.ID);
        //layout.addShowViewShortcut("com.soartech.soar.ide.debug.ui.commandline");
        
        // views - search
        //layout.addShowViewShortcut(NewSearchUI.SEARCH_VIEW_ID);
        
        // views - debugging
        //layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);

        // views - standard workbench
        //layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
        //layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
        //layout.addShowViewShortcut(IPageLayout.ID_RES_NAV);
        //layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
        
        // wizard actions in the "new" menu.
        //layout.addNewWizardShortcut("com.soartech.soar.ide.ui.NewSoarAgentWizard");
        //layout.addNewWizardShortcut("com.soartech.soar.ide.ui.NewSoarFolderWizard");//$NON-NLS-1$
        //layout.addNewWizardShortcut("com.soartech.soar.ide.ui.NewSoarFileWizard");//$NON-NLS-1$
    }
}
